#20260908_kpopmodder: Recognize only acknowledgement-bearing STATUS route decisions.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication import (
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)


class CommandStatusPublicationCustodyPolicy:
    _ROUTE_KINDS = frozenset(
        {"command_status_query", "crafting_status_query"}
    )

    def matches(self, decision: object) -> bool:
        try:
            if not isinstance(decision, MinecraftChatClefInputRouteDecision):
                return False
            if decision.handled is not True:
                return False
            if decision.route_kind not in self._ROUTE_KINDS:
                return False
            acknowledgement = decision.response_publication_acknowledgement
            if type(acknowledgement) is not CommandFeedbackPublicationAcknowledgement:
                return False
            if not callable(acknowledgement.acknowledge):
                return False
            return acknowledgement.kind == CommandFeedbackPublicationPermit.STATUS
        except Exception:
            return False


__all__ = ("CommandStatusPublicationCustodyPolicy",)
