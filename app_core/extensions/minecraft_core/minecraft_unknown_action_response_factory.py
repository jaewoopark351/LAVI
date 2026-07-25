#20260725_kpopmodder: Added response factory for unsupported Minecraft extension actions.
from __future__ import annotations

from typing import Dict

from .minecraft_command_dispatch_result import MinecraftCommandDispatchResult


class MinecraftUnknownActionResponseFactory:
    def build(self, action: str) -> MinecraftCommandDispatchResult:
        return MinecraftCommandDispatchResult(
            {"ok": False, "action": action or "", "error": "unknown_action"},
            action,
        )
