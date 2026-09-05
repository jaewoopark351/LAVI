#20260905_kpopmodder: Isolate optional route-owner result type validation.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class MinecraftOptionalRouteOwnerResultValidator:
    def validate(
        self,
        decision: object,
    ) -> MinecraftChatClefInputRouteDecision | None:
        if decision is None:
            return None
        if not isinstance(decision, MinecraftChatClefInputRouteDecision):
            raise TypeError("optional route owner returned an invalid decision")
        return decision


__all__ = ("MinecraftOptionalRouteOwnerResultValidator",)
