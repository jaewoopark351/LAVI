#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.presentation.stop_control import (
    StopControlPresentationDetailProjector,
)


class StopControlRouteOutcomeBuilder:
    def __init__(self, renderer: object, presentation_detail_projector=None):
        self._renderer = renderer
        self._presentation_details = (
            presentation_detail_projector
            or StopControlPresentationDetailProjector()
        )

    def not_handled(self, reason: str) -> MinecraftChatClefInputRouteDecision:
        return MinecraftChatClefInputRouteDecision.not_handled(reason)

    def duplicate(self, reason: str) -> MinecraftChatClefInputRouteDecision:
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason,
            response_text="",
        )

    def local(self, reason: str) -> MinecraftChatClefInputRouteDecision:
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=str(reason),
            response_text=self._renderer.render_local(reason),
            result={"ok": False, "status": "rejected", "reason": reason},
        )

    def submitted(self, outcome: object) -> MinecraftChatClefInputRouteDecision:
        reason = str(
            getattr(outcome, "reason", "control_send_rejected")
            or "control_send_rejected"
        )
        accepted = getattr(outcome, "accepted", None)
        if type(accepted) is not bool or (reason == "accepted") is not accepted:
            reason = (
                "control_send_unknown"
                if accepted is True
                else "control_send_rejected"
            )
        result = getattr(outcome, "result", {})
        #20260908_kpopmodder: Let the verified terminal own the sole visible response for an accepted trusted STOP.
        response_text = (
            "" if reason == "accepted" else self._renderer.render_local(reason)
        )
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=f"stop_control_{reason}",
            response_text=response_text,
            result=dict(result) if isinstance(result, dict) else {},
            route_kind="stop_control",
            response_kind=("command_start" if reason == "accepted" else "immediate"),
            presentation_detail_log=self._presentation_details.project(),
        )


__all__ = ("StopControlRouteOutcomeBuilder",)
