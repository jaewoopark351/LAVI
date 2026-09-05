#20260905_kpopmodder: Assemble focused ordinary submission-result components.
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_decision_builder import OrdinarySubmissionDecisionBuilder
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_result_diagnostics import OrdinarySubmissionResultDiagnostics
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_result_reconciler import OrdinarySubmissionResultReconciler
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_transport import OrdinarySubmissionTransport


class OrdinarySubmissionResultComponentGraph:
    def __init__(
        self,
        *,
        extension,
        submission_boundary,
        submission_reconciliation,
        decision_factory,
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


__all__ = ("OrdinarySubmissionResultComponentGraph",)
