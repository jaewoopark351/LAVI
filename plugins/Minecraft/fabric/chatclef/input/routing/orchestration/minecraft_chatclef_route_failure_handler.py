#20260905_kpopmodder: Preserve route failure handling as a sequencing facade.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)

from .failure import (
    MinecraftRouteFailureDecisionBuilder,
    MinecraftRouteFailureDiagnostics,
)


class MinecraftChatClefRouteFailureHandler:
    def __init__(self, *, decision_factory, router_logger):
        self._decision_factory = decision_factory
        self._router_logger = router_logger
        self._diagnostics = MinecraftRouteFailureDiagnostics(router_logger)
        self._decision_builder = MinecraftRouteFailureDecisionBuilder(
            decision_factory
        )

    def decision(
        self,
        reason: str,
        error: Exception,
        translation: dict[str, Any] | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        self._diagnostics.report(reason, error)
        return self._decision_builder.build(
            reason,
            error,
            translation,
        )


__all__ = ("MinecraftChatClefRouteFailureHandler",)
