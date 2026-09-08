#20260905_kpopmodder: Preserve the legacy H5 route API as a composition-only facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandFeedbackStartDecisionDecorator,
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackSubmissionObserver,
)

from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.routing.auto_deposit_trust_route_execution_lifecycle import AutoDepositTrustRouteExecutionLifecycle
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.routing.auto_deposit_trust_route_sequence import AutoDepositTrustRouteSequence


class AutoDepositTrustRouteCoordinator:
    def __init__(
        self,
        *,
        extension,
        claim_owner: object,
        input_admission,
        raw_input_safety,
        exact_input_adapter,
        translation_admission,
        claim_registry,
        translation_boundary,
        submission_precheck,
        submission_boundary,
        submission_route_lock,
        submission_reconciliation,
        decision_factory,
        router_logger,
    ):
        self._extension = extension
        self._claim_owner = claim_owner
        self._input_admission = input_admission
        self._raw_input_safety = raw_input_safety
        self._exact_input_adapter = exact_input_adapter
        self._translation_admission = translation_admission
        self._claim_registry = claim_registry
        self._translation_boundary = translation_boundary
        self._submission_precheck = submission_precheck
        self._submission_boundary = submission_boundary
        self._submission_route_lock = submission_route_lock
        self._submission_reconciliation = submission_reconciliation
        self._decision_factory = decision_factory
        self._router_logger = router_logger
        feedback_descriptors = CommandFeedbackDescriptorFactory()
        feedback_observer = CommandFeedbackSubmissionObserver(
            extension=extension,
            admission_coordinator=CommandFeedbackAdmissionCoordinator(
                live_proof_validator=lambda _proof, _event: False,
                descriptor_factory=feedback_descriptors,
            ),
        )
        self._route_sequence = AutoDepositTrustRouteSequence(
            extension=extension,
            input_admission=input_admission,
            raw_input_safety=raw_input_safety,
            exact_input_adapter=exact_input_adapter,
            translation_admission=translation_admission,
            translation_boundary=translation_boundary,
            submission_precheck=submission_precheck,
            submission_boundary=submission_boundary,
            submission_reconciliation=submission_reconciliation,
            decision_factory=decision_factory,
            command_feedback=feedback_observer,
            descriptor_factory=feedback_descriptors,
            start_decision_decorator=CommandFeedbackStartDecisionDecorator(
                CommandLifecycleResponseRenderer()
            ),
        )
        self._execution_lifecycle = AutoDepositTrustRouteExecutionLifecycle(
            claim_owner=claim_owner,
            claim_registry=claim_registry,
            submission_route_lock=submission_route_lock,
            decision_factory=decision_factory,
            router_logger=router_logger,
        )

    def route(self, event: object) -> MinecraftChatClefInputRouteDecision:
        return self._execution_lifecycle.execute(event, self._route_sequence)

    def route_claimed(
        self,
        event: object,
        receipt: object,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._route_sequence.route_claimed(event, receipt)


__all__ = ("AutoDepositTrustRouteCoordinator",)
