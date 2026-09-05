#20260905_kpopmodder: Isolate submitted-command route decisions.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class MinecraftSubmittedRouteDecisionFactory:
    def __init__(self, response_renderer, status_classifier):
        self._response_renderer = response_renderer
        self._status_classifier = status_classifier

    def build(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ) -> MinecraftChatClefInputRouteDecision:
        result_status = self._status_classifier.classify(result)
        details = result.get("details")
        result_details = dict(details) if isinstance(details, Mapping) else {}
        if (
            result_status == "unknown"
            or result_details.get("reconciliation_required") is True
        ):
            reason = "minecraft_submission_outcome_unknown"
        elif result.get("ok") is True:
            reason = "minecraft_command_routed"
        else:
            reason = "minecraft_command_rejected"
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason,
            response_text=self.response_text(translation, result),
            result=dict(result),
            translation=dict(translation),
        )

    def response_text(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ) -> str:
        return self._response_renderer.render_submitted(translation, result)


__all__ = ("MinecraftSubmittedRouteDecisionFactory",)
