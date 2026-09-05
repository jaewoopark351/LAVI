#20260905_kpopmodder: Isolate submission-precheck route decisions.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)

from plugins.Minecraft.fabric.chatclef.input.routing.submission_readiness import MinecraftChatClefSubmissionReadiness


class MinecraftPrecheckRouteDecisionFactory:
    def __init__(self, response_renderer):
        self._response_renderer = response_renderer

    def build(
        self,
        readiness: MinecraftChatClefSubmissionReadiness,
    ) -> MinecraftChatClefInputRouteDecision:
        result = {
            "ok": False,
            "error": readiness.error,
            "message": readiness.message,
            "status": dict(readiness.status),
            "details": dict(readiness.details),
        }
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=readiness.reason,
            response_text=self._response_renderer.render_precheck_rejection(
                readiness.reason,
                readiness.message,
            ),
            result=result,
        )


__all__ = ("MinecraftPrecheckRouteDecisionFactory",)
