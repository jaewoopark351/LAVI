#20260725_kpopmodder: Added Minecraft extension status builder separate from lifecycle control.
from __future__ import annotations

from typing import Any, Dict


class MinecraftExtensionStatusBuilder:
    def build(
        self,
        *,
        name: str,
        plugin: Any,
        runtime_context: Any,
        initialized: bool,
        started: bool,
    ) -> Dict[str, Any]:
        plugin_status = self._plugin_status(plugin)
        plugin_status["extension"] = {
            "name": name,
            "initialized": initialized,
            "started": started,
        }
        snapshot = getattr(runtime_context, "snapshot", None)
        if callable(snapshot):
            plugin_status["runtime_context"] = snapshot()
        return plugin_status

    def _plugin_status(self, plugin: Any) -> Dict[str, Any]:
        if plugin is None:
            return {}
        try:
            status = plugin.get_status()
            if isinstance(status, dict):
                return status
        except Exception as error:
            return {"ok": False, "error": str(error)}
        return {}
