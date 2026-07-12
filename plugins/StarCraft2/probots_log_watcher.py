#20260708_kpopmodder: Added tail-style ProBots log watcher for passive SC2 commentary.
from __future__ import annotations

import os
import threading
import time
from dataclasses import dataclass
from typing import Any, Callable, Dict, Iterable, List, Optional

from core.logger import log_print


LogLineCallback = Callable[[str, str], None]


@dataclass
class WatchedLogState:
    path: str
    offset: int = 0
    exists: bool = False
    last_message: str = ""
    lines_seen: int = 0


class ProBotsLogWatcher:
    """Poll configured log files and emit newly appended lines."""

    def __init__(self, poll_interval_sec: float = 0.5, start_at_end: bool = True):
        self.poll_interval_sec = max(0.1, float(poll_interval_sec))
        self.start_at_end = bool(start_at_end)
        self._states: Dict[str, WatchedLogState] = {}
        self._callback: Optional[LogLineCallback] = None
        self._thread = None
        self._stop_event = threading.Event()
        self._lock = threading.Lock()
        self._status_messages: List[str] = []

    def start(
        self,
        log_paths: Iterable[str],
        callback: LogLineCallback,
    ) -> Dict[str, Any]:
        self.stop()
        self._callback = callback
        self._stop_event.clear()
        self._status_messages = []
        self._states = {
            self._clean_path(path): WatchedLogState(path=self._clean_path(path))
            for path in log_paths
            if str(path or "").strip()
        }

        if not self._states:
            self._status_messages.append("no_log_paths_configured")

        for state in self._states.values():
            self._initialize_state(state)

        self._thread = threading.Thread(
            target=self._run_loop,
            name="ProBotsLogWatcher",
            daemon=True,
        )
        self._thread.start()
        return {"ok": True, "status": self.get_status()}

    def stop(self) -> Dict[str, Any]:
        thread = self._thread
        if thread is not None and thread.is_alive():
            self._stop_event.set()
            thread.join(timeout=2.0)
        self._thread = None
        self._stop_event.clear()
        return {"ok": True, "status": self.get_status()}

    def get_status(self) -> Dict[str, Any]:
        with self._lock:
            states = {
                path: {
                    "exists": state.exists,
                    "offset": state.offset,
                    "last_message": state.last_message,
                    "lines_seen": state.lines_seen,
                }
                for path, state in self._states.items()
            }
            messages = list(self._status_messages[-20:])
        return {
            "running": self._thread is not None and self._thread.is_alive(),
            "poll_interval_sec": self.poll_interval_sec,
            "states": states,
            "messages": messages,
        }

    def _run_loop(self) -> None:
        while not self._stop_event.is_set():
            for state in list(self._states.values()):
                self._poll_state(state)
            self._stop_event.wait(self.poll_interval_sec)

    def _initialize_state(self, state: WatchedLogState) -> None:
        if not os.path.isfile(state.path):
            state.exists = False
            state.last_message = f"log_file_missing: {state.path}"
            self._record_status(state.last_message)
            return
        state.exists = True
        try:
            state.offset = os.path.getsize(state.path) if self.start_at_end else 0
            state.last_message = "watching"
        except OSError as e:
            state.last_message = f"log_file_stat_failed: {state.path}: {e}"
            self._record_status(state.last_message)

    def _poll_state(self, state: WatchedLogState) -> None:
        if not os.path.isfile(state.path):
            if state.exists:
                state.exists = False
                state.last_message = f"log_file_missing: {state.path}"
                self._record_status(state.last_message)
            elif not state.last_message:
                state.last_message = f"log_file_missing: {state.path}"
                self._record_status(state.last_message)
            return

        if not state.exists:
            state.exists = True
            state.offset = os.path.getsize(state.path) if self.start_at_end else 0
            state.last_message = "log_file_found"
            self._record_status(f"log_file_found: {state.path}")
            return

        try:
            file_size = os.path.getsize(state.path)
            if file_size < state.offset:
                state.offset = 0
                self._record_status(f"log_file_truncated: {state.path}")
            if file_size == state.offset:
                return
            with open(state.path, "rb") as file:
                file.seek(state.offset)
                chunk = file.read()
                state.offset = file.tell()
        except OSError as e:
            state.last_message = f"log_file_read_failed: {state.path}: {e}"
            self._record_status(state.last_message)
            return

        text = self._decode_bytes(chunk)
        for line in text.splitlines():
            clean = line.strip()
            if not clean:
                continue
            state.lines_seen += 1
            state.last_message = clean
            callback = self._callback
            if callable(callback):
                try:
                    callback(state.path, clean)
                except Exception as e:
                    log_print(f"[ProBotsLogWatcher] callback failed: {e}")

    def _decode_bytes(self, chunk: bytes) -> str:
        for encoding in ("utf-8", "cp949"):
            try:
                return chunk.decode(encoding)
            except UnicodeDecodeError:
                continue
        return chunk.decode("utf-8", errors="replace")

    def _record_status(self, message: str) -> None:
        with self._lock:
            self._status_messages.append(str(message))

    def _clean_path(self, value: Any) -> str:
        return os.path.normpath(str(value or "").strip().strip("\"'"))
