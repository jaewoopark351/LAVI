#20260708_kpopmodder: Added ProBots launcher for SC2 log commentary without UI or game control.
from __future__ import annotations

import os
import subprocess
import threading
import time
from collections import deque
from typing import Any, Callable, Dict, Iterable, List, Optional

from core.logger import log_print
from core.process import launch_process


LineCallback = Callable[[str, str], None]


class ProBotsLauncher:
    """Launch ProBots/SC2AIApp as an external process without automating its UI."""

    def __init__(
        self,
        app_path: str = "",
        args: Optional[Iterable[str]] = None,
        working_directory: str = "",
        tail_size: int = 40,
    ):
        self.app_path = str(app_path or "")
        self.args = [str(item) for item in (args or [])]
        self.working_directory = str(working_directory or "")
        self.process = None
        self.started_at = 0.0
        self.last_error = ""
        self.stdout_tail = deque(maxlen=max(1, int(tail_size)))
        self.stderr_tail = deque(maxlen=max(1, int(tail_size)))
        self._stdout_thread = None
        self._stderr_thread = None

    def validate_path(self, app_path: Optional[str] = None) -> Dict[str, Any]:
        resolved = self._clean_path(app_path if app_path is not None else self.app_path)
        if not resolved:
            return {"ok": False, "error": "probots_app_path_missing", "path": ""}
        if not os.path.isfile(resolved):
            return {
                "ok": False,
                "error": "probots_app_not_found",
                "path": resolved,
            }
        return {"ok": True, "path": resolved}

    def start(
        self,
        app_path: Optional[str] = None,
        args: Optional[Iterable[str]] = None,
        working_directory: Optional[str] = None,
        capture_output: bool = True,
        line_callback: Optional[LineCallback] = None,
        env: Optional[Dict[str, str]] = None,
    ) -> Dict[str, Any]:
        if self.is_running():
            return {"ok": True, "running": True, "status": self.get_status()}

        if app_path is not None:
            self.app_path = str(app_path or "")
        if args is not None:
            self.args = [str(item) for item in args]
        if working_directory is not None:
            self.working_directory = str(working_directory or "")

        validation = self.validate_path()
        if not validation.get("ok"):
            self.last_error = str(validation.get("error", "probots_app_invalid"))
            return {"ok": False, "running": False, **validation}

        app_path = validation["path"]
        cwd = self._resolve_working_directory(app_path)
        command = [app_path] + list(self.args)
        stdout = subprocess.PIPE if capture_output else None
        stderr = subprocess.PIPE if capture_output else None

        try:
            self.process = launch_process(
                command,
                cwd=cwd or None,
                stdout=stdout,
                stderr=stderr,
                env=env,
                text=True,
                encoding="utf-8",
                errors="replace",
                bufsize=1,
            )
        except Exception as e:
            self.last_error = str(e)
            log_print(f"[ProBotsLauncher] launch failed: {e}")
            return {"ok": False, "running": False, "error": str(e)}

        self.started_at = time.time()
        self.last_error = ""
        if capture_output:
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

        return {"ok": True, "running": True, "status": self.get_status()}

    def start_sc2aiapp(
        self,
        config: Dict[str, Any],
        capture_output: bool = True,
        line_callback: Optional[LineCallback] = None,
    ) -> Dict[str, Any]:
        validation = self.validate_sc2aiapp_config(config)
        if not validation.get("ok"):
            self.last_error = str(validation.get("error", "sc2aiapp_config_invalid"))
            return {"ok": False, "running": False, **validation}

        if bool(config.get("kill_existing_processes_before_launch", False)):
            kill_results = self._kill_existing_processes(("SC2AIApp.exe", "SC2_x64.exe"))
        else:
            kill_results = []

        app_path = str(validation["sc2aiapp_path"])
        env = self.build_sc2aiapp_env(config)
        #20260708_kpopmodder: SC2AIApp only launched SC2_x64 on this PC when
        # SC2PATH pointed to the full SC2_x64.exe path, not the StarCraft II root.
        result = self.start(
            app_path=app_path,
            working_directory=os.path.dirname(app_path),
            capture_output=capture_output,
            line_callback=line_callback,
            env=env,
        )
        result["validation"] = validation
        result["kill_results"] = kill_results
        return result

    def validate_sc2aiapp_config(self, config: Dict[str, Any]) -> Dict[str, Any]:
        paths = self._resolve_sc2aiapp_paths(config)
        checks = [
            ("sc2aiapp_path", paths["sc2aiapp_path"], os.path.isfile),
            ("starcraft2_exe_path", paths["starcraft2_exe_path"], os.path.isfile),
            ("starcraft2_support64_path", paths["starcraft2_support64_path"], os.path.isdir),
            ("starcraft2_base_path", paths["starcraft2_base_path"], os.path.isdir),
        ]
        missing = []
        for key, path, predicate in checks:
            if not path:
                missing.append({"key": key, "path": "", "reason": "missing"})
            elif not predicate(path):
                missing.append({"key": key, "path": path, "reason": "not_found"})

        maps_path = paths.get("maps_path", "")
        if maps_path and not os.path.isdir(maps_path):
            missing.append({"key": "maps_path", "path": maps_path, "reason": "not_found"})

        if missing:
            return {
                "ok": False,
                "error": "sc2aiapp_path_validation_failed",
                "missing": missing,
                **paths,
            }
        return {"ok": True, **paths}

    def build_sc2aiapp_env(self, config: Dict[str, Any]) -> Dict[str, str]:
        paths = self._resolve_sc2aiapp_paths(config)
        env = os.environ.copy()
        support64_path = paths["starcraft2_support64_path"]
        base_path = paths["starcraft2_base_path"]
        existing_path = env.get("PATH", "")
        env["SC2PATH"] = paths["starcraft2_exe_path"]
        env["PATH"] = ";".join(
            item for item in (support64_path, base_path, existing_path) if item
        )
        return env

    def stop(self, timeout_sec: float = 5.0) -> Dict[str, Any]:
        process = self.process
        if process is None:
            return {"ok": True, "running": False, "status": self.get_status()}

        if self.is_running():
            try:
                process.terminate()
                process.wait(timeout=max(0.1, float(timeout_sec)))
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=1.0)
            except Exception as e:
                self.last_error = str(e)
                return {"ok": False, "error": str(e), "status": self.get_status()}

        self.process = None
        return {"ok": True, "running": False, "status": self.get_status()}

    def is_running(self) -> bool:
        process = self.process
        return bool(process is not None and process.poll() is None)

    def get_status(self) -> Dict[str, Any]:
        process = self.process
        return {
            "app_path": self._clean_path(self.app_path),
            "running": self.is_running(),
            "pid": getattr(process, "pid", None) if process is not None else None,
            "returncode": process.poll() if process is not None else None,
            "uptime_sec": (
                round(time.time() - self.started_at, 3)
                if self.started_at and self.is_running()
                else 0.0
            ),
            "last_error": self.last_error,
            "stdout_tail": list(self.stdout_tail),
            "stderr_tail": list(self.stderr_tail),
        }

    def _resolve_sc2aiapp_paths(self, config: Dict[str, Any]) -> Dict[str, str]:
        sc2aiapp_path = self._clean_path(
            config.get("sc2aiapp_path") or config.get("probots_app_path")
        )
        starcraft2_exe_path = self._clean_path(config.get("starcraft2_exe_path"))
        support64_path = self._clean_path(config.get("starcraft2_support64_path"))
        base_path = self._clean_path(config.get("starcraft2_base_path"))
        maps_path = self._clean_path(config.get("maps_path"))
        return {
            "sc2aiapp_path": sc2aiapp_path,
            "starcraft2_exe_path": starcraft2_exe_path,
            "starcraft2_support64_path": support64_path,
            "starcraft2_base_path": base_path,
            "maps_path": maps_path,
        }

    def _kill_existing_processes(self, process_names: Iterable[str]) -> List[Dict[str, Any]]:
        results = []
        for process_name in process_names:
            name = str(process_name or "").strip()
            if not name:
                continue
            try:
                completed = subprocess.run(
                    ["taskkill", "/IM", name, "/F"],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    check=False,
                )
                results.append(
                    {
                        "process": name,
                        "returncode": completed.returncode,
                        "ok": completed.returncode == 0,
                    }
                )
            except Exception as e:
                results.append({"process": name, "ok": False, "error": str(e)})
                log_print(f"[ProBotsLauncher] taskkill failed for {name}: {e}")
        return results

    def _resolve_working_directory(self, app_path: str) -> str:
        configured = self._clean_path(self.working_directory)
        if configured:
            return configured
        return os.path.dirname(app_path)

    def _start_reader_thread(self, stream, tail, label: str, callback):
        if stream is None:
            return None
        thread = threading.Thread(
            target=self._read_stream,
            args=(stream, tail, label, callback),
            name=f"ProBotsLauncher.{label}",
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
                if callable(callback):
                    callback(label, line)
        except Exception as e:
            message = f"{label}_read_failed: {e}"
            tail.append(message)
            log_print(f"[ProBotsLauncher] {message}")

    def _clean_path(self, value: Any) -> str:
        return os.path.normpath(str(value or "").strip().strip("\"'")) if value else ""
