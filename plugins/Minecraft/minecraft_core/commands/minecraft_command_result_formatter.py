#20260725_kpopmodder: Added command result formatter for Minecraft command routing.
from __future__ import annotations

from typing import Any, Dict


class MinecraftCommandResultFormatter:
    def with_action(self, payload: Dict[str, Any], action: str) -> Dict[str, Any]:
        result = dict(payload or {})
        result.setdefault("action", action)
        return result
