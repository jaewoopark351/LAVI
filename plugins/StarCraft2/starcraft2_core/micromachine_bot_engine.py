#20260708_kpopmodder: Added MicroMachine-specific external executable adapter.
from __future__ import annotations

import os
from typing import Any, Dict, List

from .starcraft2_contracts import EngineResultDTO, EngineStartCommandDTO
from .external_exe_bot_engine import ExternalProcessBotEngine


class MicroMachineBotEngine(ExternalProcessBotEngine):
    #20260708_kpopmodder: Keep MicroMachine as an external C++ bot instead of porting it into LAV Python.
    engine_name = "micromachine"
    config_section = "micromachine"

    #20260715_kpopmodder: Keep MicroMachine preflight while using the DTO engine boundary.
    def start(
        self,
        command: EngineStartCommandDTO,
        event_callback=None,
    ) -> EngineResultDTO:
        command = EngineStartCommandDTO.from_mapping(command)
        config = command.to_dict()
        section = self._section(config)
        error = self._preflight_error(section)
        if error:
            self.state.mark_error(error)
            return self._result(
                False,
                running=False,
                status=self.get_status(),
                error=error,
            )
        return super().start(command, event_callback=event_callback)

    def _section(self, config: Dict[str, Any]) -> Dict[str, Any]:
        section = super()._section(config)
        if not section.get("path"):
            fallback = (config or {}).get("external_exe", {})
            if isinstance(fallback, dict) and fallback.get("path"):
                section["path"] = fallback.get("path")
                section.setdefault("working_directory", fallback.get("working_directory", ""))
                section.setdefault("args", fallback.get("args", []))
        if not section.get("starcraft2_path"):
            section["starcraft2_path"] = (config or {}).get("starcraft2_path", "")
        return section

    def _build_command(self, section: Dict[str, Any]) -> List[str]:
        path = self._path(section)
        if not path:
            return []

        args = self._args(section.get("args", []))
        if self._auto_add_executable_arg(section, args):
            executable = self._resolve_starcraft2_executable(section)
            if executable:
                args = ["-e", executable] + args
        return [path] + args

    def _working_directory(self, section: Dict[str, Any]) -> str:
        configured = super()._working_directory(section)
        if configured:
            return configured
        path = self._path(section)
        if not path:
            return ""
        return os.path.dirname(os.path.abspath(path))

    def _missing_command_error(self) -> str:
        return "micromachine_path_missing"

    def _preflight_error(self, section: Dict[str, Any]) -> str:
        path = self._path(section)
        if not path:
            return ""
        if not os.path.isfile(path):
            return f"micromachine_exe_not_found: {path}"

        working_directory = self._working_directory(section)
        if not working_directory or not os.path.isdir(working_directory):
            return f"micromachine_working_directory_not_found: {working_directory}"

        if not self._requires_bot_config(section):
            return ""

        bot_config_name = str(section.get("bot_config_name") or "BotConfig.txt").strip()
        bot_config_path = os.path.join(working_directory, bot_config_name)
        if not os.path.isfile(bot_config_path):
            return f"micromachine_bot_config_missing: {bot_config_path}"

        args = self._args(section.get("args", []))
        explicit_executable = self._explicit_executable_arg(args)
        if explicit_executable:
            if not os.path.isfile(explicit_executable):
                return f"micromachine_sc2_executable_not_found: {explicit_executable}"
            return ""
        if self._auto_add_executable_arg(section, args):
            executable = self._resolve_starcraft2_executable(section)
            if not executable:
                return (
                    "micromachine_sc2_executable_not_found: "
                    f"{section.get('starcraft2_path', '')}"
                )
        return ""

    def _requires_bot_config(self, section: Dict[str, Any]) -> bool:
        value = section.get("require_bot_config", True)
        if isinstance(value, bool):
            return value
        return str(value).strip().lower() not in {"0", "false", "no", "off"}

    def _path(self, section: Dict[str, Any]) -> str:
        return str(section.get("path") or "").strip().strip("\"'")

    def _auto_add_executable_arg(self, section: Dict[str, Any], args: List[str]) -> bool:
        if self._explicit_executable_arg(args):
            return False
        value = section.get("auto_add_executable_arg", True)
        if isinstance(value, bool):
            return value
        return str(value).strip().lower() not in {"0", "false", "no", "off"}

    def _explicit_executable_arg(self, args: List[str]) -> str:
        for index, arg in enumerate(args):
            text = str(arg).strip()
            if text in {"-e", "--executable"} and index + 1 < len(args):
                return str(args[index + 1]).strip().strip("\"'")
            if text.startswith("-e="):
                return text.split("=", 1)[1].strip().strip("\"'")
            if text.startswith("--executable="):
                return text.split("=", 1)[1].strip().strip("\"'")
        return ""

    def _resolve_starcraft2_executable(self, section: Dict[str, Any]) -> str:
        explicit = str(section.get("starcraft2_executable") or "").strip().strip("\"'")
        if explicit and os.path.isfile(explicit):
            return os.path.normpath(explicit)

        base_path = str(section.get("starcraft2_path") or "").strip().strip("\"'")
        if not base_path:
            return ""
        base_path = os.path.normpath(os.path.expandvars(os.path.expanduser(base_path)))
        if os.path.isfile(base_path):
            return base_path
        if not os.path.isdir(base_path):
            return ""

        direct = self._first_existing_executable(base_path)
        if direct:
            return direct

        versions_dir = os.path.join(base_path, "Versions")
        if not os.path.isdir(versions_dir):
            return ""
        candidates = []
        try:
            version_names = os.listdir(versions_dir)
        except OSError:
            return ""
        for name in version_names:
            version_path = os.path.join(versions_dir, name)
            executable = self._first_existing_executable(version_path)
            if executable:
                candidates.append((self._version_sort_key(name, executable), executable))
        if not candidates:
            return ""
        candidates.sort(key=lambda item: item[0], reverse=True)
        return candidates[0][1]

    def _first_existing_executable(self, directory: str) -> str:
        if not os.path.isdir(directory):
            return ""
        for filename in ("SC2_x64.exe", "SC2.exe"):
            path = os.path.join(directory, filename)
            if os.path.isfile(path):
                return os.path.normpath(path)
        return ""

    def _version_sort_key(self, name: str, executable: str):
        digits = "".join(char for char in str(name) if char.isdigit())
        try:
            version_number = int(digits or "0")
        except ValueError:
            version_number = 0
        try:
            modified_time = os.path.getmtime(executable)
        except OSError:
            modified_time = 0.0
        return (version_number, modified_time)
