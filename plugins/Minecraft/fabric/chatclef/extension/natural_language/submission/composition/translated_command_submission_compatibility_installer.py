#20260905_kpopmodder: Install translated-submission compatibility seams separately.


class TranslatedCommandSubmissionCompatibilityInstaller:
    def install(self, owner, graph) -> None:
        owner._registry_provider = graph.registry_provider
        owner._command_submitter = graph.command_submitter
        owner._result_recorder = graph.result_recorder
        owner._admission = graph.admission
        owner._result_factory = graph.result_factory
        owner._input_stage = graph.input_stage
        owner._admission_stage = graph.admission_stage
        owner._request_stage = graph.request_stage
        owner._route_claim_lifecycle = graph.route_claim_lifecycle
        owner._pipeline = graph.pipeline


__all__ = ("TranslatedCommandSubmissionCompatibilityInstaller",)
