#20260905_kpopmodder: Preserve ordinary submission-result sequencing as a facade.

from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_result_component_graph import OrdinarySubmissionResultComponentGraph


class OrdinarySubmissionResultStage:
    def __init__(
        self,
        *,
        extension,
        submission_boundary,
        submission_reconciliation,
        decision_factory,
        failure_handler,
        router_logger,
    ):
        self._extension = extension
        self._submission_boundary = submission_boundary
        self._submission_reconciliation = submission_reconciliation
        self._decision_factory = decision_factory
        self._failure_handler = failure_handler
        self._router_logger = router_logger
        self._component_graph = OrdinarySubmissionResultComponentGraph(
            extension=extension,
            submission_boundary=submission_boundary,
            submission_reconciliation=submission_reconciliation,
            decision_factory=decision_factory,
            failure_handler=failure_handler,
            router_logger=router_logger,
        )
        self._transport = self._component_graph.transport
        self._reconciler = self._component_graph.reconciler
        self._diagnostics = self._component_graph.diagnostics
        self._decision_builder = self._component_graph.decision_builder

    def submit(
        self,
        *,
        event: object,
        command_text: str,
        translation,
        translation_status: str,
    ):
        result, failure = self._transport.submit(
            event=event,
            command_text=command_text,
            translation=translation,
        )
        if failure is not None:
            return failure
        self._reconciler.observe(result)
        self._diagnostics.report(
            translation_status=translation_status,
            translation=translation,
            result=result,
        )
        return self._decision_builder.build(translation, result)


__all__ = ("OrdinarySubmissionResultStage",)
