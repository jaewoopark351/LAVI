#20260905_kpopmodder: Install Feature-B route compatibility seams separately.


class GenericCraftingRouteCompatibilityInstaller:
    def install(self, owner, graph) -> None:
        owner._extension = graph.extension
        owner._admission = graph.admission
        owner._translation_boundary = graph.translation_boundary
        owner._submission_precheck = graph.submission_precheck
        owner._submission_boundary = graph.submission_boundary
        owner._submission_reconciliation = graph.submission_reconciliation
        owner._decision_factory = graph.decision_factory
        owner._profile = graph.profile
        owner._candidate_detector = graph.candidate_detector
        owner._ownership_classifier = graph.ownership_classifier
        owner._candidate_detection_stage = graph.candidate_detection_stage
        owner._candidate_ownership_classification_stage = (
            graph.candidate_ownership_classification_stage
        )
        owner._candidate_ownership_stage = graph.candidate_ownership_stage
        owner._reconciliation_stage = graph.reconciliation_stage
        owner._capability_stage = graph.capability_stage
        owner._activation_admission_stage = graph.activation_admission_stage
        owner._activation_stage = graph.activation_stage
        owner._translation_invoker = graph.translation_invoker
        owner._translation_result_stage = graph.translation_result_stage
        owner._translation_stage = graph.translation_stage
        owner._submission_readiness_stage = graph.submission_readiness_stage
        owner._submission_delivery = graph.submission_delivery
        owner._submission_reconciliation_observer = (
            graph.submission_reconciliation_observer
        )
        owner._submission_result_projector = graph.submission_result_projector
        owner._submission_stage = graph.submission_stage
        owner._receipt_cleanup = graph.receipt_cleanup
        owner._dispatch_close_lifecycle = graph.dispatch_close_lifecycle
        owner._dispatch_lifecycle = graph.dispatch_lifecycle
        owner._failure_handler = graph.failure_handler
        owner._execution_lifecycle = graph.execution_lifecycle
        owner._pipeline = graph.pipeline


__all__ = ("GenericCraftingRouteCompatibilityInstaller",)
