#20260905_kpopmodder: Isolate reconciled-without-submission route decisions.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class MinecraftReconciledRouteDecisionFactory:
    def __init__(self, response_renderer):
        self._response_renderer = response_renderer

    def build(self, request_id: str) -> MinecraftChatClefInputRouteDecision:
        message = (
            "The previous Fabric ChatClef request reached a matching terminal "
            "result. The current command was not submitted; send it again as "
            "a fresh explicit command if it is still wanted."
        )
        result = {
            "ok": False,
            "request_id": request_id,
            "error": "current_command_not_submitted",
            "message": message,
            "details": {
                "reconciliation_completed": True,
                "current_command_submitted": False,
            },
        }
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_submission_reconciled_command_not_submitted",
            response_text=(
                self._response_renderer.render_reconciled_without_submission()
            ),
            result=result,
        )


__all__ = ("MinecraftReconciledRouteDecisionFactory",)
