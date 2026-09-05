#20260905_kpopmodder: Isolate optional route-owner failure conversion.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class MinecraftOptionalRouteOwnerFailureConverter:
    def __init__(self, failure_handler):
        self._failure_handler = failure_handler

    def convert(
        self,
        failure_reason: str,
        error: Exception,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._failure_handler.decision(failure_reason, error)


__all__ = ("MinecraftOptionalRouteOwnerFailureConverter",)
