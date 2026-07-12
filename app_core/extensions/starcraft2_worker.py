#20260707_kpopmodder: Added StarCraft2 command worker so UI/LLM commands do not run SC2 loops inline.
from __future__ import annotations

import queue
import threading
import time
from typing import Any, Dict, Mapping, Optional, Protocol

from core.logger import log_print

from .starcraft2_bridge import StarCraft2Bridge


class StarCraft2WorkerProtocol(Protocol):
    #20260707_kpopmodder: Minimal async worker API for StarCraft2 command routing.
    def start(self) -> bool: ...
    def stop(self) -> bool: ...
    def submit(self, command: Any) -> bool: ...
    def handle_command(self, command: Any) -> Dict[str, Any]: ...
    def get_status(self) -> Dict[str, Any]: ...


class StarCraft2Worker:
    #20260707_kpopmodder: Queue mutating commands while allowing direct status snapshots.
    def __init__(
        self,
        bridge: StarCraft2Bridge,
        poll_interval_sec: float = 0.2,
        queue_size: int = 64,
    ):
        self.bridge = bridge
        self.poll_interval_sec = max(float(poll_interval_sec), 0.05)
        self._queue: queue.Queue = queue.Queue(maxsize=max(int(queue_size), 1))
        self._stop_event = threading.Event()
        self._thread = None
        self._started = False
        self._last_command: Optional[Dict[str, Any]] = None
        self._last_bridge_result: Optional[Dict[str, Any]] = None
        self._last_bridge_error = ""
        self._stats = {
            "processed": 0,
            "failed": 0,
            "last_processed_unix": 0.0,
        }

    @property
    def is_running(self) -> bool:
        return self._started and bool(self._thread and self._thread.is_alive())

    def start(self) -> bool:
        if self.is_running:
            return True
        self._started = True
        self._stop_event.clear()
        self._thread = threading.Thread(
            target=self._loop,
            name="StarCraft2Worker",
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
                log_print("[StarCraft2Worker] stop interrupted by keyboard")
            except Exception as e:
                log_print(f"[StarCraft2Worker] stop failed: {e}")
        return True

    def submit(self, command: Any) -> bool:
        try:
            self._queue.put(command, timeout=0.1)
            return True
        except queue.Full:
            log_print("[StarCraft2Worker] command queue full")
            return False

    def handle_command(self, command: Any) -> Dict[str, Any]:
        action = self._command_action(command)
        if action in {"status", "get_status", "set_engine"}:
            return self.bridge.send_command(command)
        if not self.submit(command):
            return {"ok": False, "action": action, "error": "queue_full"}
        return {"ok": True, "action": action, "queued": True}

    def get_status(self) -> Dict[str, Any]:
        return {
            "running": self.is_running,
            "queue_size": self._queue.qsize(),
            "queue_max": self._queue.maxsize,
            "stats": dict(self._stats),
            "last_command": dict(self._last_command or {}),
            "last_bridge_result": dict(self._last_bridge_result or {}),
            "last_bridge_error": self._last_bridge_error,
        }

    def _loop(self):
        while not self._stop_event.is_set():
            processed = self._process_queue_once()
            if not processed:
                self._stop_event.wait(self.poll_interval_sec)

    def _process_queue_once(self) -> bool:
        try:
            command = self._queue.get(timeout=0.05)
        except queue.Empty:
            return False

        self._last_command = self._snapshot_command(command)
        self._last_bridge_result = None
        self._last_bridge_error = ""
        try:
            result = self.bridge.send_command(command)
            self._last_bridge_result = result
            if not isinstance(result, dict) or not bool(result.get("ok")):
                self._stats["failed"] += 1
                self._last_bridge_error = (
                    str(result.get("error", "unknown_error"))
                    if isinstance(result, dict)
                    else "invalid_bridge_result"
                )
                log_print(
                    "[StarCraft2Worker] bridge command failed: "
                    f"action={self._command_action(command)} error={self._last_bridge_error}"
                )
                return True
            self._stats["processed"] += 1
            self._stats["last_processed_unix"] = time.time()
        except Exception as e:
            self._stats["failed"] += 1
            self._last_bridge_error = str(e)
            log_print(f"[StarCraft2Worker] bridge exception: {e}")
        finally:
            self._queue.task_done()
        return True

    def _command_action(self, command: Any) -> str:
        if isinstance(command, Mapping):
            return str(command.get("action") or command.get("type") or "").strip().lower()
        if isinstance(command, str):
            return command.strip().lower()
        return ""

    def _snapshot_command(self, command: Any) -> Dict[str, Any]:
        if isinstance(command, Mapping):
            try:
                return dict(command)
            except Exception:
                return {"raw": str(command)}
        if isinstance(command, (str, int, float, bool)) or command is None:
            return {"raw": command}
        return {"raw_type": type(command).__name__, "raw": str(command)}

