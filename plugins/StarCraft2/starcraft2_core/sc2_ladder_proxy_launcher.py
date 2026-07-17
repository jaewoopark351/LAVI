#20260709_kpopmodder: Added ladder/proxy process wrapper without faking SC2 API traffic.
from __future__ import annotations

import os
import shlex
import socket
import threading
import time
from collections import deque
from collections.abc import Iterable
from typing import Any, Callable, Dict, List, Optional

from core.logger import log_print
from core.process import PIPE, TimeoutExpired, launch_process
from .starcraft2_contracts import (
    LadderProxyExitEventDTO,
    LadderProxyPortCheckDTO,
    LadderProxyPortsStatusDTO,
    LadderProxyResultDTO,
    LadderProxyStatusDTO,
    LocalMatchLaunchConfigDTO,
)
from .sc2_local_match_command_template import _LocalMatchLaunchDiagnostics


LineCallback = Callable[[str, str], None]
ExitCallback = Callable[[LadderProxyExitEventDTO], None]


class SC2LadderProxyLauncher:
    """Launch and inspect an external SC2 ladder/proxy process.

    This wrapper intentionally does not implement the SC2 API protocol. If
    ports 5677/5678 are open, they must be opened by SC2AIApp/Sc2LadderServer.
    """

    def __init__(self, tail_size: int = 40, runtime_context=None):
        self.process = None
        self.started_at = 0.0
        self.last_error = ""
        #20260715_kpopmodder: RuntimeContext writes belong to StarCraft2FacadeService.
        # Keep the optional argument temporarily so older construction code does not break.
        self.stdout_tail = deque(maxlen=max(1, int(tail_size)))
        self.stderr_tail = deque(maxlen=max(1, int(tail_size)))
        self._stdout_thread = None
        self._stderr_thread = None
        self._monitor_thread = None
        self._diagnostics = _LocalMatchLaunchDiagnostics()

    def validate_config(self, command: LocalMatchLaunchConfigDTO) -> Dict[str, Any]:
        command = LocalMatchLaunchConfigDTO.from_mapping(command)
        executable_path = self._clean_path(command.executable_path)
        working_directory = self._clean_path(command.working_directory)
        if not executable_path:
            return {"ok": False, "error": "ladder_proxy_executable_missing", "path": ""}
        if not os.path.isfile(executable_path):
            return {
                "ok": False,
                "error": "ladder_proxy_executable_not_found",
                "path": executable_path,
            }
        if working_directory and not os.path.isdir(working_directory):
            return {
                "ok": False,
                "error": "ladder_proxy_working_directory_not_found",
                "path": working_directory,
            }
        return {
            "ok": True,
            "executable_path": executable_path,
            "working_directory": working_directory or os.path.dirname(executable_path),
        }

    def start(
        self,
        command: LocalMatchLaunchConfigDTO,
        line_callback: Optional[LineCallback] = None,
        exit_callback: Optional[ExitCallback] = None,
    ) -> LadderProxyResultDTO:
        command = LocalMatchLaunchConfigDTO.from_mapping(command)
        if self.is_running():
            status = self.get_status(command)
            if self._should_restart_unhealthy(command, status):
                log_print(
                    "[SC2LadderProxyLauncher] restarting unhealthy process "
                    f"pid={status.pid} uptime_sec={status.uptime_sec}"
                )
                self.stop()
            else:
                return LadderProxyResultDTO(ok=True, running=True, status=status)

        #20260712_kpopmodder: Clear stale process tails before each new launch so
        # LAN Lobby diagnostics do not report stdout/stderr from an older match.
        self.stdout_tail.clear()
        self.stderr_tail.clear()
        validation = self.validate_config(command)
        if not validation.get("ok"):
            self.last_error = str(validation.get("error", "ladder_proxy_config_invalid"))
            return LadderProxyResultDTO(
                ok=False,
                running=False,
                status=self.get_status(command),
                error=self.last_error,
                details={
                    key: value
                    for key, value in validation.items()
                    if key not in {"ok", "error"}
                },
            )

        command_args = self._with_sc2_executable_arg(
            self._normalize_args(command.args),
            command,
        )

        process_command = [validation["executable_path"]] + command_args
        stdout = PIPE if command.capture_output else None
        stderr = PIPE if command.capture_output else None
        self._diagnostics = _LocalMatchLaunchDiagnostics()
        self._diagnostics.start()
        try:
            #20260715_kpopmodder: Log-only guardrail so old native binaries are
            # visible before launch without changing the process command.
            self._log_executable_diagnostics(
                validation["executable_path"],
                validation["working_directory"],
            )
            log_print(
                "[SC2LadderProxyLauncher] starting "
                f"exe={validation['executable_path']} cwd={validation['working_directory']} "
                f"args={command_args}"
            )
            self.process = launch_process(
                process_command,
                cwd=validation["working_directory"] or None,
                stdout=stdout,
                stderr=stderr,
                env=self._build_env(command),
                text=True,
                encoding="utf-8",
                errors="replace",
                bufsize=1,
            )
        except Exception as e:
            self.last_error = str(e)
            log_print(f"[SC2LadderProxyLauncher] launch failed: {e}")
            return LadderProxyResultDTO(
                ok=False,
                running=False,
                status=self.get_status(command),
                error=str(e),
            )

        self.started_at = time.time()
        self.last_error = ""
        log_print(
            "[SC2LadderProxyLauncher] started "
            f"pid={getattr(self.process, 'pid', None)} capture_output={command.capture_output}"
        )
        if command.capture_output:
            self._stdout_thread = self._start_reader_thread(
                getattr(self.process, "stdout", None),
                self.stdout_tail,
                "stdout",
                line_callback,
            )
            self._stderr_thread = self._start_reader_thread(
                getattr(self.process, "stderr", None),
                self.stderr_tail,
                "stderr",
                line_callback,
            )
        self._monitor_thread = threading.Thread(
            target=self._monitor_process_exit,
            args=(self.process, exit_callback),
            name="SC2LadderProxyLauncher.monitor",
            daemon=True,
        )
        self._monitor_thread.start()
        return LadderProxyResultDTO(
            ok=True,
            running=True,
            status=self.get_status(command),
        )

    def stop(self, timeout_sec: float = 5.0) -> LadderProxyResultDTO:
        process = self.process
        if process is None:
            return LadderProxyResultDTO(
                ok=True,
                running=False,
                status=self.get_status(),
                stopped=True,
            )

        if self.is_running():
            try:
                log_print(
                    "[SC2LadderProxyLauncher] stopping "
                    f"pid={getattr(process, 'pid', None)} timeout_sec={timeout_sec}"
                )
                process.terminate()
                process.wait(timeout=max(0.1, float(timeout_sec)))
            except TimeoutExpired:
                process.kill()
                process.wait(timeout=1.0)
            except Exception as e:
                self.last_error = str(e)
                return LadderProxyResultDTO(
                    ok=False,
                    running=self.is_running(),
                    status=self.get_status(),
                    error=str(e),
                )
        self.process = None
        return LadderProxyResultDTO(
            ok=True,
            running=False,
            status=self.get_status(),
            stopped=True,
        )

    def is_running(self) -> bool:
        process = self.process
        return bool(process is not None and process.poll() is None)

    def check_ports(
        self,
        command: Optional[LocalMatchLaunchConfigDTO] = None,
    ) -> LadderProxyPortsStatusDTO:
        command = LocalMatchLaunchConfigDTO.from_mapping(command)
        ports = list(command.proxy_ports)
        hosts = self._normalize_hosts(command)
        timeout = command.connect_timeout_sec
        checks = []
        for host in hosts:
            for port in ports:
                checks.append(self._check_port(host, port, timeout))
        return LadderProxyPortsStatusDTO(
            ok=any(item.open for item in checks),
            hosts=hosts,
            ports=ports,
            checks=checks,
        )

    def get_status(
        self,
        command: Optional[LocalMatchLaunchConfigDTO] = None,
    ) -> LadderProxyStatusDTO:
        process = self.process
        normalized_command = (
            LocalMatchLaunchConfigDTO.from_mapping(command)
            if command is not None
            else None
        )
        validation = (
            self.validate_config(normalized_command)
            if normalized_command is not None
            else {}
        )
        ports = (
            self.check_ports(normalized_command)
            if normalized_command is not None
            else None
        )
        return LadderProxyStatusDTO(
            running=self.is_running(),
            pid=getattr(process, "pid", None) if process is not None else None,
            returncode=process.poll() if process is not None else None,
            uptime_sec=(
                round(time.time() - self.started_at, 3)
                if self.started_at and self.is_running()
                else 0.0
            ),
            last_error=self.last_error,
            stdout_tail=list(self.stdout_tail),
            stderr_tail=list(self.stderr_tail),
            validation=validation,
            ports=ports,
            launch_diagnostics=self._diagnostics.snapshot(),
        )

    def _check_port(self, host: str, port: int, timeout: float) -> LadderProxyPortCheckDTO:
        try:
            with socket.create_connection((host, int(port)), timeout=max(0.1, timeout)):
                return LadderProxyPortCheckDTO(host=host, port=int(port), open=True)
        except OSError as e:
            return LadderProxyPortCheckDTO(
                host=host,
                port=int(port),
                open=False,
                error=str(e),
            )

    def _should_restart_unhealthy(
        self,
        command: LocalMatchLaunchConfigDTO,
        status: LadderProxyStatusDTO,
    ) -> bool:
        if not command.restart_unhealthy:
            return False
        uptime_sec = status.uptime_sec
        threshold = command.restart_unhealthy_after_sec
        if uptime_sec < max(1.0, threshold):
            return False
        if self._has_startup_progress(status.stdout_tail):
            return False
        return True

    def _has_startup_progress(self, lines: Iterable[Any]) -> bool:
        #20260712_kpopmodder: Local Match can get stuck with only the wrapper
        # banner and an s2client-api "unrecognized argument" line while no SC2
        # client ever appears. Treat later lifecycle lines as proof it moved on.
        progress_markers = (
            "starting the starcraft ii clients",
            "connecting proxy",
            "client changed status",
            "creating the game",
            "starting the bots",
            "starting the match",
            "finished with result",
        )
        for line in lines or []:
            lower = str(line or "").lower()
            if any(marker in lower for marker in progress_markers):
                return True
        return False

    def _build_env(self, command: LocalMatchLaunchConfigDTO) -> Dict[str, str]:
        env = os.environ.copy()
        starcraft2_exe_path = self._clean_path(command.starcraft2_exe_path)
        support64_path = self._clean_path(command.starcraft2_support64_path)
        base_path = self._clean_path(command.starcraft2_base_path)
        working_directory = self._clean_path(command.working_directory)
        if starcraft2_exe_path:
            env["SC2PATH"] = starcraft2_exe_path
        existing_path = env.get("PATH", "")
        #20260710_kpopmodder: LavHumanVsBot launches Java bots with `java -jar`; expose the SC2AIApp bundled JRE when LAV starts the proxy directly.
        bundled_jre = os.path.join(working_directory, "jre", "bin") if working_directory else ""
        if bundled_jre and not os.path.isdir(bundled_jre):
            bundled_jre = ""
        path_parts = [bundled_jre, support64_path, base_path, existing_path]
        env["PATH"] = ";".join(item for item in path_parts if item)
        return env

    def _normalize_hosts(self, command: LocalMatchLaunchConfigDTO) -> List[str]:
        values = [str(item or "").strip() for item in command.check_hosts]
        proxy_host = command.proxy_host.strip()
        hosts = ["127.0.0.1"]
        for item in values + [proxy_host]:
            if item and item not in hosts:
                hosts.append(item)
        return hosts

    def _normalize_ports(self, value: Any) -> List[int]:
        if isinstance(value, str):
            raw_values = [part.strip() for part in value.split(",")]
        elif isinstance(value, Iterable):
            raw_values = list(value)
        else:
            raw_values = [5677, 5678]
        ports = []
        for item in raw_values:
            try:
                port = int(item)
            except (TypeError, ValueError):
                continue
            if 0 < port <= 65535 and port not in ports:
                ports.append(port)
        return ports or [5677, 5678]

    def _normalize_args(self, value: Any) -> List[str]:
        if isinstance(value, str):
            try:
                parts = shlex.split(value, posix=False)
            except ValueError:
                parts = value.split()
            return [str(part).strip().strip("\"'") for part in parts if str(part).strip()]
        if isinstance(value, Iterable):
            return [str(item) for item in value if str(item)]
        return []

    def _with_sc2_executable_arg(
        self,
        args: List[str],
        command: LocalMatchLaunchConfigDTO,
    ) -> List[str]:
        if self._has_sc2_executable_arg(args):
            return list(args)
        starcraft2_exe_path = self._clean_path(command.starcraft2_exe_path)
        if not starcraft2_exe_path:
            return list(args)
        #20260709_kpopmodder: s2client-api ParseSettings reads --executable, not SC2PATH.
        return ["--executable", starcraft2_exe_path] + list(args)

    def _has_sc2_executable_arg(self, args: List[str]) -> bool:
        for arg in args:
            text = str(arg or "").strip().lower()
            if text in {"-e", "--executable"}:
                return True
            if text.startswith("-e=") or text.startswith("--executable="):
                return True
        return False

    def _start_reader_thread(self, stream, tail, label: str, callback):
        if stream is None:
            return None
        thread = threading.Thread(
            target=self._read_stream,
            args=(stream, tail, label, callback),
            name=f"SC2LadderProxyLauncher.{label}",
            daemon=True,
        )
        thread.start()
        return thread

    def _read_stream(self, stream, tail, label: str, callback) -> None:
        try:
            for raw_line in stream:
                line = str(raw_line or "").rstrip()
                if not line:
                    continue
                tail.append(line)
                self._diagnostics.add_line(label, line)
                if callable(callback):
                    callback(label, line)
        except Exception as e:
            message = f"{label}_read_failed: {e}"
            tail.append(message)
            log_print(f"[SC2LadderProxyLauncher] {message}")

    def _monitor_process_exit(self, process, exit_callback: Optional[ExitCallback] = None) -> None:
        if process is None:
            return
        pid = getattr(process, "pid", None)
        try:
            returncode = process.wait()
        except Exception as e:
            log_print(f"[SC2LadderProxyLauncher] wait failed pid={pid}: {e}")
            return
        log_print(
            "[SC2LadderProxyLauncher] exited "
            f"pid={pid} returncode={returncode}"
        )
        diagnostics = self._diagnostics.finalize(returncode)
        if isinstance(diagnostics, dict):
            self.last_error = str(diagnostics.get("launch_result") or self.last_error)
        if callable(exit_callback):
            try:
                exit_callback(
                    LadderProxyExitEventDTO(
                        pid=pid,
                        returncode=returncode,
                        launch_diagnostics=diagnostics,
                    )
                )
            except Exception as e:
                log_print(f"[SC2LadderProxyLauncher] exit callback failed pid={pid}: {e}")

    def _float_value(self, value: Any, default: float) -> float:
        try:
            return float(value)
        except (TypeError, ValueError):
            return float(default)

    def _log_executable_diagnostics(self, executable_path: str, working_directory: str) -> None:
        try:
            stat = os.stat(executable_path)
            marker = self._binary_contains(executable_path, b"BotLaunchDiagnostics")
            log_print(
                "[SC2LadderProxyLauncher] executable_diagnostics "
                f"path={executable_path} cwd={working_directory} "
                f"size_bytes={stat.st_size} mtime={round(stat.st_mtime, 3)} "
                f"has_bot_launch_diagnostics={marker}"
            )
        except Exception as e:
            log_print(
                "[SC2LadderProxyLauncher] executable_diagnostics_failed "
                f"path={executable_path} error={e}"
            )

    def _binary_contains(self, path: str, marker: bytes) -> bool:
        if not marker:
            return False
        overlap = max(0, len(marker) - 1)
        previous = b""
        with open(path, "rb") as file:
            while True:
                chunk = file.read(1024 * 1024)
                if not chunk:
                    return False
                data = previous + chunk
                if marker in data:
                    return True
                previous = data[-overlap:] if overlap else b""

    def _clean_path(self, value: Any) -> str:
        return os.path.normpath(str(value or "").strip().strip("\"'")) if value else ""
