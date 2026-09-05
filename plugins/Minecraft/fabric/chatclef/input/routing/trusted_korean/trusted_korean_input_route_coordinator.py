#20260905_kpopmodder: Sequence focused trusted Korean route collaborators.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)

from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.trusted_korean_input_route_component_graph import TrustedKoreanInputRouteComponentGraph


class TrustedKoreanInputRouteCoordinator:
    def __init__(
        self,
        *,
        owner: object,
        input_event_normalizer,
        eligibility_admission,
        feedback_facade,
        feature_admission_logger,
        feature_admission_projector,
        route_callback,
        close_feature_dispatch_callback,
    ):
        self._components = TrustedKoreanInputRouteComponentGraph(
            owner=owner,
            input_event_normalizer=input_event_normalizer,
            eligibility_admission=eligibility_admission,
            feedback_facade=feedback_facade,
            feature_admission_logger=feature_admission_logger,
            feature_admission_projector=feature_admission_projector,
            route_callback=route_callback,
            close_feature_dispatch_callback=close_feature_dispatch_callback,
        )

    def route(
        self,
        value: object,
        consumed_ingress_evidence: object,
    ) -> MinecraftChatClefInputRouteDecision:
        components = self._components
        admission = components.admission.admit(
            value,
            consumed_ingress_evidence,
        )
        if admission.proof is None:
            if admission.should_fall_through:
                decision = components.route_invoker.route_fallthrough(
                    admission.event
                )
            else:
                decision = components.rejection_factory.create(
                    admission.reason,
                )
            self.log_feature_admission(
                admission.event,
                eligibility_reason=admission.reason,
                proof_issued=False,
                route_decision=decision,
            )
            return decision

        decision = None
        try:
            decision = components.route_invoker.route_trusted(
                admission.event,
                admission.proof,
            )
            decision = components.feedback_renderer.render(decision)
            decision = components.response_authorizer.authorize(
                decision=decision,
                event=admission.event,
                proof=admission.proof,
            )
            return decision
        finally:
            self.log_feature_admission(
                admission.event,
                eligibility_reason=admission.reason,
                proof_issued=True,
                route_decision=decision,
            )
            components.proof_lifecycle_closer.close(admission.proof)

    def is_live_proof(self, proof: object, event: object) -> bool:
        return self._components.proof_validator.is_live(proof, event)

    def log_feature_admission(
        self,
        event: object,
        *,
        eligibility_reason: object,
        proof_issued: bool,
        route_decision: object,
    ) -> None:
        self._components.diagnostics_observer.observe(
            event,
            eligibility_reason=eligibility_reason,
            proof_issued=proof_issued,
            route_decision=route_decision,
        )


__all__ = ("TrustedKoreanInputRouteCoordinator",)
