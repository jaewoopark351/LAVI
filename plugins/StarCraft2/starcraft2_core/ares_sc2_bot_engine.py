#20260708_kpopmodder: Added external Ares-sc2 bot adapter without importing ares into the LAV runtime.
from __future__ import annotations

import os
import shutil
from typing import Any, Dict, List

from .starcraft2_contracts import EngineResultDTO, EngineStartCommandDTO
from .external_exe_bot_engine import ExternalProcessBotEngine


class AresSC2BotEngine(ExternalProcessBotEngine):
    engine_name = "ares_sc2"
    config_section = "ares_sc2"

    def _section(self, config: Dict[str, Any]) -> Dict[str, Any]:
        section = super()._section(config)
        for key in (
            "map_name",
            "race",
            "enemy_race",
            "enemy_difficulty",
            "realtime",
        ):
            if key in (config or {}) and key not in section:
                section[key] = config[key]
        return section

    def _build_command(self, section: Dict[str, Any]) -> List[str]:
        script_path = str(section.get("script_path") or "").strip().strip("\"'")
        if not script_path:
            return []

        args = self._lav_runtime_args(section) + self._args(section.get("args", []))
        if self._use_poetry(section):
            poetry_path = str(section.get("poetry_path") or "poetry").strip().strip("\"'")
            python_command = str(section.get("python_command") or "python").strip()
            return [poetry_path or "poetry", "run", python_command or "python", script_path] + args

        python_path = str(section.get("python_path") or "").strip().strip("\"'")
        return [python_path or "python", script_path] + args

    def _lav_runtime_args(self, section: Dict[str, Any]) -> List[str]:
        args: List[str] = []
        self._append_option(args, "--map", section.get("map_name"))
        self._append_option(args, "--race", section.get("race"))
        self._append_option(args, "--enemy-race", section.get("enemy_race"))
        self._append_option(args, "--enemy-difficulty", section.get("enemy_difficulty"))
        if self._bool(section.get("realtime", False)):
            args.append("--realtime")
        return args

    def _append_option(self, args: List[str], option: str, value: Any) -> None:
        text = str(value or "").strip()
        if text:
            args.extend([option, text])

    def _bool(self, value: Any) -> bool:
        if isinstance(value, bool):
            return value
        return str(value).strip().lower() in {"1", "true", "yes", "on"}

    def _missing_command_error(self) -> str:
        return "ares_sc2_script_missing"

    def _preflight_error(self, section: Dict[str, Any]) -> str:
        script_path = str(section.get("script_path") or "").strip().strip("\"'")
        if not script_path:
            return "ares_sc2_script_missing"
        if not os.path.isfile(script_path):
            return f"ares_sc2_script_not_found: {script_path}"

        working_directory = self._working_directory(section)
        if working_directory and not os.path.isdir(working_directory):
            return f"ares_sc2_working_directory_not_found: {working_directory}"

        if self._use_poetry(section):
            poetry_path = str(section.get("poetry_path") or "poetry").strip().strip("\"'")
            if os.path.isabs(poetry_path):
                if not os.path.isfile(poetry_path):
                    return f"ares_sc2_poetry_not_found: {poetry_path}"
            elif not shutil.which(poetry_path or "poetry"):
                return f"ares_sc2_poetry_not_found: {poetry_path or 'poetry'}"
        else:
            python_path = str(section.get("python_path") or "").strip().strip("\"'")
            if python_path and not os.path.isfile(python_path):
                return f"ares_sc2_python_not_found: {python_path}"

        return ""

    #20260715_kpopmodder: Accept typed commands without changing Ares launch behavior.
    def start(
        self,
        command: EngineStartCommandDTO,
        event_callback=None,
    ) -> EngineResultDTO:
        command = EngineStartCommandDTO.from_mapping(command)
        config = command.to_dict()
        section = self._section(config)
        preflight_error = self._preflight_error(section)
        if preflight_error:
            return self._result(
                False,
                running=False,
                status=self.get_status(),
                error=preflight_error,
            )
        return super().start(command, event_callback=event_callback)

    def _working_directory(self, section: Dict[str, Any]) -> str:
        working_directory = str(section.get("working_directory") or "").strip().strip("\"'")
        if working_directory:
            return working_directory
        script_path = str(section.get("script_path") or "").strip().strip("\"'")
        return os.path.dirname(script_path) if script_path else ""

    def _use_poetry(self, section: Dict[str, Any]) -> bool:
        value = section.get("use_poetry", False)
        if isinstance(value, bool):
            return value
        return str(value).strip().lower() in {"1", "true", "yes", "on"}
