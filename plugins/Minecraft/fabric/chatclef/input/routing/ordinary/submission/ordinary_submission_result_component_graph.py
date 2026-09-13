#20260905_kpopmodder: Assemble focused ordinary submission-result components.
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_decision_builder import OrdinarySubmissionDecisionBuilder
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_result_diagnostics import OrdinarySubmissionResultDiagnostics
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_result_reconciler import OrdinarySubmissionResultReconciler
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_transport import OrdinarySubmissionTransport
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandFeedbackStartDecisionDecorator,
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackSubmissionObserver,
)
#20260913_kpopmodder: Observe immediate terminal rendering through the existing logger.
from plugins.Minecraft.fabric.chatclef.diagnostics.goto_terminal import (
    GotoTerminalDiagnosticObserver,
)


class OrdinarySubmissionResultComponentGraph:
    def __init__(
        self,
        *,
        extension,
        submission_boundary,
        submission_reconciliation,
        decision_factory,
        live_proof_validator,
        failure_handler,
        router_logger,
    ) -> None:
        self.transport = OrdinarySubmissionTransport(
            extension=extension,
            submission_boundary=submission_boundary,
            failure_handler=failure_handler,
        )
        self.reconciler = OrdinarySubmissionResultReconciler(
            submission_reconciliation
        )
        self.diagnostics = OrdinarySubmissionResultDiagnostics(
            decision_factory=decision_factory,
            router_logger=router_logger,
        )
        self.decision_builder = OrdinarySubmissionDecisionBuilder(
            decision_factory
        )
        #20260907_kpopmodder: Assemble generalized admission without changing route authority.
        response_renderer = CommandLifecycleResponseRenderer(
            diagnostic_observer=GotoTerminalDiagnosticObserver(
                router_logger.log, role="immediate_coalesced",
            ),
        )
        self.command_feedback = CommandFeedbackSubmissionObserver(
            extension=extension,
            admission_coordinator=CommandFeedbackAdmissionCoordinator(
                live_proof_validator=live_proof_validator,
                descriptor_factory=CommandFeedbackDescriptorFactory(),
            ),
        )
        self.crafting_feedback = self.command_feedback
        self.start_decision_decorator = CommandFeedbackStartDecisionDecorator(
            response_renderer
        )


__all__ = ("OrdinarySubmissionResultComponentGraph",)
