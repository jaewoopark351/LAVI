#20260908_kpopmodder: Construct STATUS route decisions without owning parsing or state reads.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)

from .command_status_emergency_decision import (
    COMMAND_STATUS_EMERGENCY_DECISION,
)


class CommandStatusRouteDecisionFactory:
    _CAUTIOUS_TEXT = "지금 마인크래프트 작업 상태를 확인하지 못했어"

    def __init__(self, *, emergency_decision=COMMAND_STATUS_EMERGENCY_DECISION):
        self._emergency_decision = emergency_decision

    def status(
        self,
        *,
        response_text: object,
        state: object,
        acknowledgement: object,
        presentation_detail_log: object = "",
    ) -> MinecraftChatClefInputRouteDecision:
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_command_status_query",
            response_text=str(response_text or ""),
            result={
                "ok": True,
                "status": str(state or "unavailable"),
                "read_only": True,
                "command_submitted": False,
            },
            route_kind="command_status_query",
            response_kind="command_status",
            response_publication_acknowledgement=acknowledgement,
            presentation_detail_log=str(presentation_detail_log or ""),
        )

    def cautious(
        self,
        *,
        acknowledgement: object = None,
    ) -> MinecraftChatClefInputRouteDecision:
        return self.status(
            response_text=self._CAUTIOUS_TEXT,
            state="unavailable",
            acknowledgement=acknowledgement,
        )

    @staticmethod
    def conversational_fallthrough() -> MinecraftChatClefInputRouteDecision:
        return MinecraftChatClefInputRouteDecision.not_handled(
            "command_status_conversational_fallthrough"
        )

    def emergency(self):
        return self._emergency_decision


__all__ = ("CommandStatusRouteDecisionFactory",)
