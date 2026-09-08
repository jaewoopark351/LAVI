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
        live_proof_validator,
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
            live_proof_validator=live_proof_validator,
            failure_handler=failure_handler,
            router_logger=router_logger,
        )
        self._transport = self._component_graph.transport
        self._reconciler = self._component_graph.reconciler
        self._diagnostics = self._component_graph.diagnostics
        self._decision_builder = self._component_graph.decision_builder
        self._command_feedback = self._component_graph.command_feedback
        self._crafting_feedback = self._command_feedback
        self._start_decision_decorator = (
            self._component_graph.start_decision_decorator
        )

    def submit(
        self,
        *,
        event: object,
        command_text: str,
        translation,
        translation_status: str,
        korean_eligibility_proof: object = None,
    ):
        #20260907_kpopmodder: Reserve before send, then claim START after owner commit.
        grant = self._command_feedback.prepare(
            event=event,
            korean_eligibility_proof=korean_eligibility_proof,
            translation=translation,
        )
        try:
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
            decision = self._decision_builder.build(translation, result)
            publication_acknowledgement = (
                self._command_feedback.claim_start(grant, result)
            )
            return self._start_decision_decorator.decorate(
                decision,
                descriptor=getattr(grant, "descriptor", None),
                start_claimed=(
                    publication_acknowledgement is not None
                    and publication_acknowledgement is not False
                ),
                publication_acknowledgement=(
                    None
                    if publication_acknowledgement is True
                    else publication_acknowledgement
                ),
            )
        finally:
            self._command_feedback.abandon(grant)


__all__ = ("OrdinarySubmissionResultStage",)
