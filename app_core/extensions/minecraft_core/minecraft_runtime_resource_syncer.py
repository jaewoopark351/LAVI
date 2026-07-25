#20260725_kpopmodder: Added runtime resource sync helper for Minecraft extension diagnostics.
from __future__ import annotations

from typing import Any


class MinecraftRuntimeResourceSyncer:
    def sync(self, runtime_context: Any, plugin: Any) -> None:
        set_resource = getattr(runtime_context, "set_resource", None)
        if not callable(set_resource):
            return
        set_resource("plugin", plugin)
        if plugin is None:
            return
        set_resource("config_manager", getattr(plugin, "config_manager", None))
        set_resource("facade_service", getattr(plugin, "facade_service", None))
