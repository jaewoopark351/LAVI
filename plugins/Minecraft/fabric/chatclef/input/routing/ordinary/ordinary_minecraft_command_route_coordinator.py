#20260905_kpopmodder: Preserve the ordinary route lock as a thin pipeline facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)

from .composition import OrdinaryMinecraftCommandRouteComponentGraph


class OrdinaryMinecraftCommandRouteCoordinator:
    def __init__(
        self,
        *,
        extension,
        translation_boundary,
        submission_precheck,
        submission_boundary,
        submission_route_lock,
        submission_reconciliation,
        decision_factory,
        item_command_ownership_classifier,
        item_command_rejection_evidence_parser,
        live_proof_validator,
        failure_handler,
        router_logger,
    ):
        self._component_graph = OrdinaryMinecraftCommandRouteComponentGraph(
            extension=extension,
            translation_boundary=translation_boundary,
            submission_precheck=submission_precheck,
            submission_boundary=submission_boundary,
            submission_route_lock=submission_route_lock,
            submission_reconciliation=submission_reconciliation,
            decision_factory=decision_factory,
            item_command_ownership_classifier=(
                item_command_ownership_classifier
            ),
            item_command_rejection_evidence_parser=(
                item_command_rejection_evidence_parser
            ),
            live_proof_validator=live_proof_validator,
            failure_handler=failure_handler,
            router_logger=router_logger,
        )
        self._component_graph.install_compatibility_seams(self)

    def route(
        self,
        event: object,
        command_text: str,
        *,
        korean_eligibility_proof: object = None,
    ) -> MinecraftChatClefInputRouteDecision:
        rejection = self._availability_stage.rejection()
        if rejection is not None:
            return rejection
        with self._submission_route_lock:
            return self.route_locked(
                event,
                command_text,
                korean_eligibility_proof=korean_eligibility_proof,
            )

    def route_locked(
        self,
        event: object,
        command_text: str,
        *,
        korean_eligibility_proof: object = None,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._pipeline.route_locked(
            event,
            command_text,
            korean_eligibility_proof=korean_eligibility_proof,
        )


__all__ = ("OrdinaryMinecraftCommandRouteCoordinator",)
