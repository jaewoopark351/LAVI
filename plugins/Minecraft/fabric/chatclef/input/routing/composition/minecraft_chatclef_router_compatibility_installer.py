#20260905_kpopmodder: Install legacy router attributes separately from graph assembly.


class MinecraftChatClefRouterCompatibilityInstaller:
    def install(self, owner, graph) -> None:
        owner.extension = graph.extension
        owner.intent_gate = graph.intent_gate
        owner._input_event_normalizer = graph.input_event_normalizer
        owner._auto_deposit_trust_input_admission = (
            graph.auto_deposit_trust_input_admission
        )
        owner._auto_deposit_trust_raw_input_safety = (
            graph.auto_deposit_trust_raw_input_safety
        )
        owner._auto_deposit_trust_exact_input_adapter = (
            graph.auto_deposit_trust_exact_input_adapter
        )
        owner._auto_deposit_trust_translation_admission = (
            graph.auto_deposit_trust_translation_admission
        )
        owner.auto_deposit_trust_claim_registry = (
            graph.auto_deposit_trust_claim_registry
        )
        owner.log_callback = graph.log_callback
        owner._router_logger = graph.router_logger
        owner._feature_admission_logger = graph.feature_admission_logger
        owner._feature_admission_projector = graph.feature_admission_projector
        owner._trusted_korean_command_feedback_facade = (
            graph.trusted_korean_command_feedback_facade
        )
        owner._translation_boundary = graph.translation_boundary
        owner._submission_precheck = graph.submission_precheck
        owner._submission_boundary = graph.submission_boundary
        owner._submission_route_lock = graph.submission_route_lock
        owner.submission_reconciliation = graph.submission_reconciliation
        owner._decision_factory = graph.decision_factory
        owner._item_command_ownership_classifier = (
            graph.item_command_ownership_classifier
        )
        owner._item_command_rejection_evidence_parser = (
            graph.item_command_rejection_evidence_parser
        )
        owner._korean_eligibility_admission = graph.korean_eligibility_admission
        owner._stop_control_route_owner = graph.stop_control_route_owner
        owner._generic_crafting_defaults_route_owner = (
            graph.generic_crafting_defaults_route_owner
        )
        owner._trusted_input_route_coordinator = (
            graph.trusted_input_route_coordinator
        )
        owner._auto_deposit_trust_route_coordinator = (
            graph.auto_deposit_trust_route_coordinator
        )
        owner._ordinary_command_route_coordinator = (
            graph.ordinary_command_route_coordinator
        )
        owner._route_failure_handler = graph.failure_handler
        owner._route_ordering_coordinator = graph.route_ordering_coordinator


__all__ = ("MinecraftChatClefRouterCompatibilityInstaller",)
