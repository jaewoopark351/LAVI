#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class KoreanStopControlRoutingCoordinator:
    def __init__(
        self,
        *,
        claim_registry: object,
        classification_coordinator: object,
        submission_coordinator: object,
        outcome_builder: object,
    ):
        self._claim_registry = claim_registry
        self._classification_coordinator = classification_coordinator
        self._submission_coordinator = submission_coordinator
        self._outcome_builder = outcome_builder

    def route(
        self,
        event: object,
        korean_eligibility_proof: object,
    ) -> MinecraftChatClefInputRouteDecision:
        if not self._claim_registry.accepts_claim_authority(
            event=event,
            eligibility_proof=korean_eligibility_proof,
        ):
            return self._outcome_builder.not_handled(
                "invalid_stop_control_authority"
            )

        decision = self._classification_coordinator.classify(event)
        if not decision.valid:
            if not decision.guarded_rejection:
                return self._outcome_builder.not_handled("not_stop_control")
            return self._outcome_builder.local("unsafe_stop_phrase")

        outcome, reason = self._submission_coordinator.submit(
            event=event,
            eligibility_proof=korean_eligibility_proof,
            normalized_phrase=decision.normalized_phrase,
        )
        if outcome is not None:
            return self._outcome_builder.submitted(outcome)
        if reason == "duplicate_or_spent_stop_claim":
            return self._outcome_builder.duplicate(reason)
        return self._outcome_builder.local(reason)

    def try_route(self, event: object, korean_eligibility_proof: object):
        decision = self.route(event, korean_eligibility_proof)
        return decision if decision.handled else None


__all__ = ("KoreanStopControlRoutingCoordinator",)
