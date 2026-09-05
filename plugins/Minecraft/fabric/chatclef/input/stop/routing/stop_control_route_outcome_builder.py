#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class StopControlRouteOutcomeBuilder:
    def __init__(self, renderer: object):
        self._renderer = renderer

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
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=f"stop_control_{reason}",
            response_text=self._renderer.render_local(reason),
            result=dict(result) if isinstance(result, dict) else {},
        )


__all__ = ("StopControlRouteOutcomeBuilder",)
