#20260725_kpopmodder: Added status gate for implicit Minecraft chat command routing.
from __future__ import annotations

from collections.abc import Mapping
from typing import Any


class MinecraftImplicitRoutingAvailability:
    def is_available(self, extension: Any) -> bool:
        if extension is None:
            return False

        status_reader = getattr(extension, "get_status", None)
        if not callable(status_reader):
            return False

        try:
            status = status_reader()
        except Exception:
            return False

        if not isinstance(status, Mapping):
            return False
        if status.get("ok") is False:
            return False
        if status.get("enabled") is False:
            return False
        if status.get("allow_actions") is False:
            return False

        extension_status = status.get("extension")
        if isinstance(extension_status, Mapping) and extension_status.get("started") is False:
            return False

        bridge_status = status.get("bridge")
        if not isinstance(bridge_status, Mapping):
            return False
        return bool(bridge_status.get("ok")) and bool(bridge_status.get("in_game"))
