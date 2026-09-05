#20260905_kpopmodder: Assemble focused ordinary submission precheck components.
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.precheck.ordinary_submission_precheck_diagnostics import OrdinarySubmissionPrecheckDiagnostics
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.precheck.ordinary_submission_precheck_evaluator import OrdinarySubmissionPrecheckEvaluator


class OrdinarySubmissionPrecheckComponentGraph:
    def __init__(
        self,
        *,
        extension,
        submission_precheck,
        decision_factory,
        router_logger,
    ) -> None:
        self.evaluator = OrdinarySubmissionPrecheckEvaluator(
            extension=extension,
            submission_precheck=submission_precheck,
            decision_factory=decision_factory,
        )
        self.diagnostics = OrdinarySubmissionPrecheckDiagnostics(router_logger)


__all__ = ("OrdinarySubmissionPrecheckComponentGraph",)
