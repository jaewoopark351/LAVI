#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260707_kpopmodder: Added safe subprocess adapter for future external Windows SC2 bot executables.
from __future__ import annotations

import os
import threading
import time
from collections import deque
from typing import Any, Dict, Iterable, List, Optional

from core.logger import log_print
from core.process import PIPE, TimeoutExpired, launch_process as _default_launch_process

from .starcraft2_contracts import (
    EngineResultDTO,
    EngineStartCommandDTO,
    EngineStatusDTO,
    StarCraft2Event,
)
from .starcraft2_engine_interface import StarCraft2EngineInterface
from .starcraft2_state import StarCraft2RuntimeState

class ExternalProcessBotEngine(StarCraft2EngineInterface):
    #20260715_kpopmodder: External engines now expose the typed engine contract while preserving process launch behavior.
    engine_name = "external_process"
    config_section = ""
    uses_engine_dto_contract = True

    def __init__(self, tail_size: int = 20, stop_timeout_sec: float = 5.0):
        self.state = StarCraft2RuntimeState(engine=self.engine_name)
        self.process = None
        self._stdout_tail = deque(maxlen=max(1, int(tail_size)))
        self._stderr_tail = deque(maxlen=max(1, int(tail_size)))
        self._stdout_thread = None
        self._stderr_thread = None
        self._stop_timeout_sec = max(float(stop_timeout_sec), 0.1)
        self._started_at = 0.0

    #20260715_kpopmodder: Normalize legacy dict input at the external engine edge.
    def start(
        self,
        command: EngineStartCommandDTO,
        event_callback=None,
    ) -> EngineResultDTO:
        if self.is_running():
            return self._result(True, status=self.get_status())

        command = EngineStartCommandDTO.from_mapping(command)
        config = command.to_dict()
        section = self._section(config)
        command = self._build_command(section)
        if not command:
            return self._result(
                False,
                running=False,
                status=self.get_status(),
                error=self._missing_command_error(),
            )

        working_directory = self._working_directory(section)
        if working_directory and not os.path.isdir(working_directory):
            return self._result(
                False,
                running=False,
                status=self.get_status(),
                error=f"working_directory_not_found: {working_directory}",
            )

        try:
            self.process = _compat_launch_process(
                command,
                cwd=working_directory or None,
                stdout=PIPE,
                stderr=PIPE,
                text=True,
                bufsize=1,
            )
        except Exception as e:
            self.state.mark_error(e)
            return self._result(False, running=False, status=self.get_status(), error=e)

        self._started_at = time.time()
        self.state.mark_started(self.engine_name, config)
        self.state.process_pid = getattr(self.process, "pid", None)
        self._start_reader_threads()
        self._emit(event_callback, "process_started", {"pid": self.state.process_pid})
        return self._result(True, status=self.get_status())

    def stop(self) -> EngineResultDTO:
        process = self.process
        if process is None:
            self.state.mark_stopped()
            return self._result(True, running=False, status=self.get_status())

        if self._process_running(process):
            try:
                process.terminate()
                process.wait(timeout=self._stop_timeout_sec)
            except TimeoutExpired:
                try:
                    process.kill()
                    process.wait(timeout=1.0)
                except Exception as e:
                    self.state.mark_error(e)
                    return self._result(False, status=self.get_status(), error=e)
            except Exception as e:
                self.state.mark_error(e)
                return self._result(False, status=self.get_status(), error=e)

        self.state.mark_stopped()
        self.process = None
        return self._result(True, running=False, status=self.get_status())

    def shutdown(self) -> EngineResultDTO:
        return self.stop()

    def get_status(self) -> EngineStatusDTO:
        process = self.process
        running = self._process_running(process) if process is not None else False
        self.state.running = running
        self.state.update_process(
            process_pid=getattr(process, "pid", None) if process is not None else None,
            stdout_tail=list(self._stdout_tail),
            stderr_tail=list(self._stderr_tail),
        )
        status = self.state.to_dict()
        status["returncode"] = (
            process.poll()
            if process is not None and hasattr(process, "poll")
            else None
        )
        status["uptime_sec"] = (
            round(time.time() - self._started_at, 3)
            if self._started_at and running
            else 0.0
        )
        return EngineStatusDTO.from_mapping(status, engine=self.engine_name)

    def is_running(self) -> bool:
        return self._process_running(self.process)

    def _section(self, config: Dict[str, Any]) -> Dict[str, Any]:
        value = (config or {}).get(self.config_section, {})
        return dict(value) if isinstance(value, dict) else {}

    def _build_command(self, section: Dict[str, Any]) -> List[str]:
        path = str(section.get("path") or "").strip().strip("\"'")
        if not path:
            return []
        return [path] + self._args(section.get("args", []))

    def _missing_command_error(self) -> str:
        return "external_path_missing"

    def _working_directory(self, section: Dict[str, Any]) -> str:
        return str(section.get("working_directory") or "").strip().strip("\"'")

    def _args(self, args: Any) -> List[str]:
        if isinstance(args, (list, tuple)):
            return [str(item) for item in args]
        if args is None or args == "":
            return []
        return [str(args)]

    def _start_reader_threads(self) -> None:
        process = self.process
        if process is None:
            return
        self._stdout_thread = self._start_reader_thread(
            getattr(process, "stdout", None),
            self._stdout_tail,
            "stdout",
        )
        self._stderr_thread = self._start_reader_thread(
            getattr(process, "stderr", None),
            self._stderr_tail,
            "stderr",
        )

    def _start_reader_thread(self, stream, tail, label: str):
        if stream is None:
            return None
        thread = threading.Thread(
            target=self._read_stream_tail,
            args=(stream, tail, label),
            name=f"StarCraft2{self.engine_name}.{label}",
            daemon=True,
        )
        thread.start()
        return thread

    def _read_stream_tail(self, stream, tail, label: str) -> None:
        try:
            for line in stream:
                text = str(line).rstrip()
                if not text:
                    continue
                tail.append(text)
                log_print(f"[StarCraft2:{self.engine_name}:{label}] {text}")
        except Exception as e:
            tail.append(f"stream_read_failed: {e}")

    def _process_running(self, process) -> bool:
        if process is None:
            return False
        poll = getattr(process, "poll", None)
        if not callable(poll):
            return False
        return poll() is None

    #20260715_kpopmodder: External engine callbacks now emit typed SC2 events.
    def _emit(self, callback, event_type: str, details: Dict[str, Any] | None = None):
        if not callable(callback):
            return
        event = StarCraft2Event(
            event_type=str(event_type or ""),
            details=dict(details or {}),
            source="starcraft2",
            engine=self.engine_name,
            time=time.time(),
        )
        try:
            callback(event)
        except Exception as e:
            log_print(f"[StarCraft2:{self.engine_name}] event callback failed: {e}")


def _compat_launch_process(*args, **kwargs):
    #20260717_kpopmodder: Preserve legacy tests that patch external_exe_bot_engine.launch_process.
    from . import external_exe_bot_engine as compatibility_module

    launcher = getattr(compatibility_module, "launch_process", _default_launch_process)
    return launcher(*args, **kwargs)
