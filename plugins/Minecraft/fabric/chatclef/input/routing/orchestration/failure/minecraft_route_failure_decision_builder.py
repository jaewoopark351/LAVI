#20260905_kpopmodder: Isolate fail-closed Minecraft route decision construction.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class MinecraftRouteFailureDecisionBuilder:
    def __init__(self, decision_factory):
        self._decision_factory = decision_factory

    def build(
        self,
        reason: str,
        error: Exception,
        translation: dict[str, Any] | None,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._decision_factory.operation_failure(
            reason,
            error,
            translation,
        )


__all__ = ("MinecraftRouteFailureDecisionBuilder",)
