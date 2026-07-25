#20260725_kpopmodder: Added reader for current bridge action from Minecraft extension status snapshots.
from __future__ import annotations

from typing import Any, Mapping


class MinecraftConversationStatusActionReader:
    def read(self, status: Any) -> dict[str, Any] | None:
        if not isinstance(status, Mapping):
            return None

        direct = status.get("current_action")
        if isinstance(direct, Mapping):
            return dict(direct)

        bridge = status.get("bridge")
        if isinstance(bridge, Mapping):
            action = bridge.get("current_action")
            if isinstance(action, Mapping):
                return dict(action)
        return None
