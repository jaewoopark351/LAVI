#20260905_kpopmodder: Install legacy ordinary-route attributes separately.


class OrdinaryMinecraftCommandRouteCompatibilityInstaller:
    def install(self, owner, graph) -> None:
        owner._extension = graph.extension
        owner._translation_boundary = graph.translation_boundary
        owner._submission_precheck = graph.submission_precheck
        owner._submission_boundary = graph.submission_boundary
        owner._submission_route_lock = graph.submission_route_lock
        owner._submission_reconciliation = graph.submission_reconciliation
        owner._decision_factory = graph.decision_factory
        owner._item_command_ownership_classifier = (
            graph.item_command_ownership_classifier
        )
        owner._item_command_rejection_evidence_parser = (
            graph.item_command_rejection_evidence_parser
        )
        owner._live_proof_validator = graph.live_proof_validator
        owner._failure_handler = graph.failure_handler
        owner._router_logger = graph.router_logger
        owner._availability_stage = graph.availability_stage
        owner._reconciliation_stage = graph.reconciliation_stage
        owner._translation_stage = graph.translation_stage
        owner._rejection_stage = graph.rejection_stage
        owner._precheck_stage = graph.precheck_stage
        owner._submission_result_stage = graph.submission_result_stage
        owner._pipeline = graph.pipeline


__all__ = ("OrdinaryMinecraftCommandRouteCompatibilityInstaller",)
