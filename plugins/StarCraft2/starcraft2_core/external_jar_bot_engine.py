#20260707_kpopmodder: Added Java jar subprocess adapter for future Ketroc-style SC2 bots.
from __future__ import annotations

from typing import Any, Dict, List

from .external_exe_bot_engine import ExternalProcessBotEngine


class ExternalJarBotEngine(ExternalProcessBotEngine):
    engine_name = "external_jar"
    config_section = "external_jar"

    def _build_command(self, section: Dict[str, Any]) -> List[str]:
        java_path = str(section.get("java_path") or "java").strip().strip("\"'")
        jar_path = str(section.get("jar_path") or "").strip().strip("\"'")
        if not jar_path:
            return []
        return [java_path or "java", "-jar", jar_path] + self._args(
            section.get("args", [])
        )

    def _missing_command_error(self) -> str:
        return "external_jar_path_missing"

