#20260706_kpopmodder: Added StarCraft116 worker loop for async command execution + state bookkeeping.
from __future__ import annotations

import queue
import threading
import time
from typing import Any, Dict, Mapping, Optional, Protocol

from core.logger import log_print

from .starcraft116_bridge import StarCraft116Bridge


class StarCraft116WorkerProtocol(Protocol):
    #20260706_kpopmodder: Define minimal worker protocol for queue/loop ownership.
    def start(self) -> bool: ...
    def stop(self) -> bool: ...
    def submit(self, command: Any) -> bool: ...
    def handle_command(self, command: Any) -> Dict[str, Any]: ...
    def get_status(self) -> Dict[str, Any]: ...


class StarCraft116Worker:
    #20260706_kpopmodder: Keep command handling asynchronous while existing game polling remains in plugin.
    def __init__(
        self,
        bridge: StarCraft116Bridge,
        poll_interval_sec: float = 0.2,
        queue_size: int = 64,
    ):
        self.bridge = bridge
        self.poll_interval_sec = max(float(poll_interval_sec), 0.05)
        self._queue: queue.Queue = queue.Queue(maxsize=max(int(queue_size), 1))
        self._stop_event = threading.Event()
        self._thread = None
        self._started = False
        self._start_error: Optional[str] = None
        self._last_bridge_error: str = ""
        self._last_command: Optional[Dict[str, Any]] = None
        self._last_bridge_result: Optional[Dict[str, Any]] = None
        self._last_processed_unix = 0.0
        self._stats = {
            "processed": 0,
            "failed": 0,
            "last_processed_unix": 0.0,
        }

    @property
    def is_running(self) -> bool:
        return self._started

    def start(self) -> bool:
        if self._started and self._thread is not None and self._thread.is_alive():
            return True
        self._started = True
        self._start_error = None
        self._stop_event.clear()
        self._thread = threading.Thread(
            target=self._loop,
            name="StarCraft116Worker",
            daemon=True,
        )
        self._thread.start()
        return True

    def stop(self) -> bool:
        self._started = False
        self._stop_event.set()
        thread = self._thread
        if thread is not None and thread.is_alive():
            try:
                thread.join(timeout=self.poll_interval_sec * 4)
            except KeyboardInterrupt:
                log_print("[StarCraft116Worker] stop interrupted by keyboard")
            except Exception as e:
                log_print(f"[StarCraft116Worker] stop failed: {e}")
        return True

    def submit(self, command: Any) -> bool:
        try:
            self._queue.put(command, timeout=0.1)
            return True
        except queue.Full:
            log_print("[StarCraft116Worker] command queue full")
            return False

    def handle_command(self, command: Any) -> Dict[str, Any]:
        if not self.submit(command):
            return {"ok": False, "error": "queue_full"}

        return {"ok": True}

    def get_status(self) -> Dict[str, Any]:
        return {
            "running": self._started and bool(self._thread and self._thread.is_alive()),
            "queue_size": self._queue.qsize(),
            "queue_max": self._queue.maxsize,
            "stats": dict(self._stats),
            "last_command": self._snapshot_last_command(),
            "last_bridge_result": self._last_bridge_result or {},
            "last_bridge_error": self._last_bridge_error,
            "start_error": self._start_error,
        }

    def _loop(self):
        while not self._stop_event.is_set():
            processed = self._process_queue_once()
            if not processed:
                self._run_tick()
                self._stop_event.wait(self.poll_interval_sec)

    def _process_queue_once(self) -> bool:
        try:
            command = self._queue.get(timeout=0.05)
        except queue.Empty:
            return False

        self._last_bridge_error = ""
        self._last_command = self._snapshot_command(command)
        self._last_bridge_result = None
        try:
            result = self.bridge.send_command(command)
            self._last_bridge_result = result
            if not isinstance(result, dict) or not bool(result.get("ok")):
                self._stats["failed"] += 1
                error = (
                    str(result.get("error", "unknown_error"))
                    if isinstance(result, dict)
                    else "invalid_bridge_result"
                )
                action = result.get("action") if isinstance(result, dict) else ""
                self._last_bridge_error = error
                self._start_error = error
                log_print(
                    "[StarCraft116Worker] bridge command failed: "
                    f"action={action} error={error}"
                )
                return True

            self._stats["processed"] += 1
            self._last_processed_unix = time.time()
            self._stats["last_processed_unix"] = self._last_processed_unix
        except Exception as e:
            self._stats["failed"] += 1
            self._last_bridge_error = str(e)
            self._start_error = str(e)
            log_print(f"[StarCraft116Worker] bridge exception: {e}")
        finally:
            self._queue.task_done()
        return True

    def _snapshot_command(self, command: Any) -> Dict[str, Any]:
        if isinstance(command, Mapping):
            try:
                return dict(command)
            except Exception:
                return {"raw": str(command)}
        if isinstance(command, (str, int, float, bool)) or command is None:
            return {"raw": command}
        return {"raw_type": type(command).__name__, "raw": str(command)}

    def _snapshot_last_command(self) -> Dict[str, Any]:
        last_command = self._last_command
        if last_command is None:
            return {}
        if isinstance(last_command, dict):
            return dict(last_command)
        return {"raw": str(last_command)}

    def _run_tick(self):
        bridge = self.bridge
        if bridge is None:
            return
        plugin = getattr(bridge, "plugin", None)
        if plugin is None or getattr(plugin, "status_event_callback", None) is None:
            return

        # Keep plugin-driven polling as the primary source; only poll via worker
        # when no native watcher thread is running.
        watcher = getattr(plugin, "game_event_thread", None)
        if watcher is not None and watcher.is_alive():
            return
        bridge.poll_once()
