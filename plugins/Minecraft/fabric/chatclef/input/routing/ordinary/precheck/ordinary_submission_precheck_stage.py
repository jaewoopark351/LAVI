#20260905_kpopmodder: Preserve ordinary precheck sequencing as a thin facade.

from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.precheck.ordinary_submission_precheck_component_graph import OrdinarySubmissionPrecheckComponentGraph


class OrdinarySubmissionPrecheckStage:
    def __init__(
        self,
        *,
        extension,
        submission_precheck,
        decision_factory,
        router_logger,
    ):
        self._extension = extension
        self._submission_precheck = submission_precheck
        self._decision_factory = decision_factory
        self._router_logger = router_logger
        self._component_graph = OrdinarySubmissionPrecheckComponentGraph(
            extension=extension,
            submission_precheck=submission_precheck,
            decision_factory=decision_factory,
            router_logger=router_logger,
        )
        self._evaluator = self._component_graph.evaluator
        self._diagnostics = self._component_graph.diagnostics

    def rejection(self):
        readiness = self._evaluator.inspect()
        if readiness.ready:
            return None
        self._diagnostics.report(readiness)
        return self._evaluator.rejection(readiness)


__all__ = ("OrdinarySubmissionPrecheckStage",)
