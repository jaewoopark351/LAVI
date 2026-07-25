#20260725_kpopmodder: Added plugin status reader so status builder does not call plugins directly.
from __future__ import annotations

from typing import Any, Dict


class MinecraftPluginStatusReader:
    def read(self, plugin: Any) -> Dict[str, Any]:
        if plugin is None:
            return {}
        try:
            status = plugin.get_status()
            if isinstance(status, dict):
                return status
        except Exception as error:
            return {"ok": False, "error": str(error)}
        return {}
