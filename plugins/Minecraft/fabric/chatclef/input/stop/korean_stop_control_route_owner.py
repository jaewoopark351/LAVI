#20260905_kpopmodder: Preserve the legacy Korean STOP route owner as a thin facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.response.stop import StopControlResponseRenderer

from .diagnostics import StopInputDecisionLogger
from .korean_stop_input_classifier import KoreanStopInputClassifier
from .routing import (
    KoreanStopControlRoutingCoordinator,
    StopControlClaimSubmissionCoordinator,
    StopControlRouteOutcomeBuilder,
    StopInputClassificationCoordinator,
)
from .stop_control_claim_registry import StopControlClaimRegistry


class KoreanStopControlRouteOwner:
    def __init__(
        self,
        *,
        extension: object = None,
        classifier: KoreanStopInputClassifier | None = None,
        claim_registry: StopControlClaimRegistry | None = None,
        renderer: StopControlResponseRenderer | None = None,
        decision_logger: StopInputDecisionLogger | None = None,
    ):
        self._extension = extension
        self._classifier = classifier or KoreanStopInputClassifier()
        self._claim_registry = claim_registry or StopControlClaimRegistry()
        self._renderer = renderer or StopControlResponseRenderer()
        self._decision_logger = decision_logger or StopInputDecisionLogger()
        self._coordinator = KoreanStopControlRoutingCoordinator(
            claim_registry=self._claim_registry,
            classification_coordinator=StopInputClassificationCoordinator(
                classifier=self._classifier,
                decision_logger=self._decision_logger,
            ),
            submission_coordinator=StopControlClaimSubmissionCoordinator(
                extension=self._extension,
                claim_registry=self._claim_registry,
            ),
            outcome_builder=StopControlRouteOutcomeBuilder(self._renderer),
        )

    @property
    def claim_registry(self) -> StopControlClaimRegistry:
        return self._claim_registry

    def route(
        self,
        event: object,
        korean_eligibility_proof: object,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._coordinator.route(event, korean_eligibility_proof)

    def try_route(self, event: object, korean_eligibility_proof: object):
        return self._coordinator.try_route(event, korean_eligibility_proof)


__all__ = ("KoreanStopControlRouteOwner",)
