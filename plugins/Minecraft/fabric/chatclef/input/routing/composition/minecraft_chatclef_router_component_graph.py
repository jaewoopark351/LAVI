#20260905_kpopmodder: Assemble the router object graph outside its public facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust import (
    AutoDepositTrustExactInputAdapter,
    AutoDepositTrustInputAdmission,
    AutoDepositTrustRawInputSafety,
    AutoDepositTrustRouteCoordinator,
    AutoDepositTrustTranslationAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.diagnostics import (
    MinecraftChatClefInputRouterLogger,
    MinecraftKoreanFeatureAdmissionLogger,
    MinecraftKoreanFeatureAdmissionProjector,
)
from plugins.Minecraft.fabric.chatclef.input.eligibility import (
    KoreanChatMicrophoneEligibilityAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.gating import (
    MinecraftChatClefInputIntentGate,
)
from plugins.Minecraft.fabric.chatclef.input.ownership import (
    ItemCommandOwnershipClassifier,
    ItemCommandTranslationRejectionEvidenceParser,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary import (
    OrdinaryMinecraftCommandRouteCoordinator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration import (
    MinecraftChatClefRouteFailureHandler,
    MinecraftInputRouteOrderingCoordinator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.route_decision_factory import (
    MinecraftChatClefRouteDecisionFactory,
)
from plugins.Minecraft.fabric.chatclef.input.routing.submission import (
    MinecraftChatClefSubmissionReconciliationCoordinator,
    MinecraftChatClefSubmissionRouteLock,
)
from plugins.Minecraft.fabric.chatclef.input.routing.submission_boundary import (
    MinecraftChatClefSubmissionBoundary,
)
from plugins.Minecraft.fabric.chatclef.input.routing.submission_precheck import (
    MinecraftChatClefSubmissionPrecheck,
)
from plugins.Minecraft.fabric.chatclef.input.routing.translation_boundary import (
    MinecraftChatClefTranslationBoundary,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean import (
    TrustedKoreanInputRouteCoordinator,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQueryClassifier,
    CommandStatusRouteOwner,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.diagnostics import (
    CommandStatusPublicationFailureAdapter,
    CommandStatusRouteFailureDiagnostics,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.publication import (
    CommandStatusPublicationCustodyPolicy,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.routing import (
    COMMAND_STATUS_EMERGENCY_DECISION,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.response.trusted_korean import (
    TrustedKoreanCommandFeedbackFacade,
)

from .minecraft_chatclef_router_compatibility_installer import (
    MinecraftChatClefRouterCompatibilityInstaller,
)
from .minecraft_chatclef_router_dependency_resolver import (
    MinecraftChatClefRouterDependencyResolver,
)


class MinecraftChatClefRouterComponentGraph:
    def __init__(
        self,
        *,
        owner: object,
        extension,
        intent_gate=None,
        input_event_normalizer=None,
        auto_deposit_trust_input_admission=None,
        auto_deposit_trust_raw_input_safety=None,
        auto_deposit_trust_exact_input_adapter=None,
        auto_deposit_trust_translation_admission=None,
        auto_deposit_trust_claim_registry=None,
        stop_control_route_owner=None,
        crafting_status_route_owner=None,
        command_status_route_owner=None,
        korean_eligibility_admission=None,
        generic_crafting_defaults_admission=None,
        generic_crafting_defaults_route_owner=None,
        trusted_korean_command_feedback_facade=None,
        feature_admission_logger=None,
        feature_admission_projector=None,
        log_callback=None,
    ):
        self._dependency_resolver = MinecraftChatClefRouterDependencyResolver(
            owner=owner,
            extension=extension,
        )
        self._compatibility_installer = (
            MinecraftChatClefRouterCompatibilityInstaller()
        )
        self.extension = extension
        self.intent_gate = intent_gate or MinecraftChatClefInputIntentGate()
        self.input_event_normalizer = self._dependency_resolver.input_event_normalizer(
            input_event_normalizer
        )
        self.auto_deposit_trust_input_admission = (
            auto_deposit_trust_input_admission
            or AutoDepositTrustInputAdmission()
        )
        self.auto_deposit_trust_raw_input_safety = (
            auto_deposit_trust_raw_input_safety
            or AutoDepositTrustRawInputSafety()
        )
        self.auto_deposit_trust_exact_input_adapter = (
            auto_deposit_trust_exact_input_adapter
            or AutoDepositTrustExactInputAdapter()
        )
        self.auto_deposit_trust_translation_admission = (
            auto_deposit_trust_translation_admission
            or AutoDepositTrustTranslationAdmission()
        )
        self.auto_deposit_trust_claim_registry = (
            self._dependency_resolver.auto_deposit_trust_claim_registry(
                auto_deposit_trust_claim_registry
            )
        )
        self.log_callback = log_callback
        self.router_logger = MinecraftChatClefInputRouterLogger(
            lambda message: owner.log_callback(message)
        )
        self.feature_admission_logger = (
            feature_admission_logger
            or MinecraftKoreanFeatureAdmissionLogger(log_callback)
        )
        self.feature_admission_projector = (
            feature_admission_projector
            or MinecraftKoreanFeatureAdmissionProjector()
        )
        self.trusted_korean_command_feedback_facade = (
            trusted_korean_command_feedback_facade
            or TrustedKoreanCommandFeedbackFacade()
        )
        self.translation_boundary = MinecraftChatClefTranslationBoundary()
        self.submission_precheck = MinecraftChatClefSubmissionPrecheck()
        self.submission_boundary = MinecraftChatClefSubmissionBoundary()
        self.submission_route_lock = MinecraftChatClefSubmissionRouteLock()
        self.submission_reconciliation = (
            MinecraftChatClefSubmissionReconciliationCoordinator(
                extension,
                route_lock=self.submission_route_lock,
            )
        )
        self.decision_factory = MinecraftChatClefRouteDecisionFactory()
        self.item_command_ownership_classifier = ItemCommandOwnershipClassifier()
        self.item_command_rejection_evidence_parser = (
            ItemCommandTranslationRejectionEvidenceParser()
        )
        #20260908_kpopmodder: Share one STATUS acknowledgement-custody policy across every publication boundary.
        self.command_status_publication_custody_policy = (
            CommandStatusPublicationCustodyPolicy()
        )
        self.command_status_emergency_decision = (
            COMMAND_STATUS_EMERGENCY_DECISION
        )
        self.command_status_publication_failure_adapter = (
            CommandStatusPublicationFailureAdapter(
                custody_policy=self.command_status_publication_custody_policy,
            )
        )
        self.command_status_route_failure_diagnostics = (
            CommandStatusRouteFailureDiagnostics(self.router_logger.log)
        )
        self.korean_eligibility_admission = (
            korean_eligibility_admission
            or KoreanChatMicrophoneEligibilityAdmission()
        )
        self.trusted_input_route_coordinator = TrustedKoreanInputRouteCoordinator(
            owner=owner,
            input_event_normalizer=self.input_event_normalizer,
            eligibility_admission=self.korean_eligibility_admission,
            feedback_facade=self.trusted_korean_command_feedback_facade,
            feature_admission_logger=self.feature_admission_logger,
            feature_admission_projector=self.feature_admission_projector,
            route_callback=lambda value, **kwargs: owner.route(value, **kwargs),
            close_feature_dispatch_callback=(
                lambda proof: owner.close_generic_crafting_defaults_dispatch(
                    proof
                )
            ),
            status_publication_custody_policy=(
                self.command_status_publication_custody_policy
            ),
            status_publication_emergency_decision=(
                self.command_status_emergency_decision
            ),
        )
        self.stop_control_route_owner = self._dependency_resolver.stop_route_owner(
            provided=stop_control_route_owner,
            log_callback=log_callback,
        )
        #20260907_kpopmodder: Assemble the trusted zero-command generalized status route.
        self.command_status_route_owner = (
            command_status_route_owner
            or crafting_status_route_owner
            or CommandStatusRouteOwner(
                extension=extension,
                live_proof_validator=(
                    self.trusted_input_route_coordinator.is_live_proof
                ),
                classifier=CommandStatusQueryClassifier(),
                response_renderer=CommandLifecycleResponseRenderer(),
                failure_diagnostics=(
                    self.command_status_route_failure_diagnostics
                ),
                publication_custody_policy=(
                    self.command_status_publication_custody_policy
                ),
            )
        )
        self.crafting_status_route_owner = self.command_status_route_owner
        self.generic_crafting_defaults_route_owner = (
            self._dependency_resolver.generic_crafting_route_owner(
                provided_admission=generic_crafting_defaults_admission,
                provided_owner=generic_crafting_defaults_route_owner,
                trusted_input_route_coordinator=(
                    self.trusted_input_route_coordinator
                ),
                translation_boundary=self.translation_boundary,
                submission_precheck=self.submission_precheck,
                submission_boundary=self.submission_boundary,
                submission_reconciliation=self.submission_reconciliation,
                decision_factory=self.decision_factory,
            )
        )
        self.failure_handler = MinecraftChatClefRouteFailureHandler(
            decision_factory=self.decision_factory,
            router_logger=self.router_logger,
        )
        self.auto_deposit_trust_route_coordinator = (
            AutoDepositTrustRouteCoordinator(
                extension=extension,
                claim_owner=owner,
                input_admission=self.auto_deposit_trust_input_admission,
                raw_input_safety=self.auto_deposit_trust_raw_input_safety,
                exact_input_adapter=self.auto_deposit_trust_exact_input_adapter,
                translation_admission=(
                    self.auto_deposit_trust_translation_admission
                ),
                claim_registry=self.auto_deposit_trust_claim_registry,
                translation_boundary=self.translation_boundary,
                submission_precheck=self.submission_precheck,
                submission_boundary=self.submission_boundary,
                submission_route_lock=self.submission_route_lock,
                submission_reconciliation=self.submission_reconciliation,
                decision_factory=self.decision_factory,
                router_logger=self.router_logger,
            )
        )
        self.ordinary_command_route_coordinator = (
            OrdinaryMinecraftCommandRouteCoordinator(
                extension=extension,
                translation_boundary=self.translation_boundary,
                submission_precheck=self.submission_precheck,
                submission_boundary=self.submission_boundary,
                submission_route_lock=self.submission_route_lock,
                submission_reconciliation=self.submission_reconciliation,
                decision_factory=self.decision_factory,
                item_command_ownership_classifier=(
                    self.item_command_ownership_classifier
                ),
                item_command_rejection_evidence_parser=(
                    self.item_command_rejection_evidence_parser
                ),
                live_proof_validator=(
                    self.trusted_input_route_coordinator.is_live_proof
                ),
                failure_handler=self.failure_handler,
                router_logger=self.router_logger,
            )
        )
        self.route_ordering_coordinator = MinecraftInputRouteOrderingCoordinator(
            input_event_normalizer=self.input_event_normalizer,
            intent_gate=self.intent_gate,
            stop_control_route_owner=self.stop_control_route_owner,
            crafting_status_route_owner=self.crafting_status_route_owner,
            generic_crafting_defaults_route_owner=(
                self.generic_crafting_defaults_route_owner
            ),
            auto_deposit_trust_route_coordinator=(
                self.auto_deposit_trust_route_coordinator
            ),
            ordinary_command_route_coordinator=(
                self.ordinary_command_route_coordinator
            ),
            failure_handler=self.failure_handler,
            status_publication_custody_policy=(
                self.command_status_publication_custody_policy
            ),
            status_publication_emergency_decision=(
                self.command_status_emergency_decision
            ),
        )

    def install_compatibility_seams(self, owner: object) -> None:
        self._compatibility_installer.install(owner, self)

    def validate_generic_crafting_registry(self, admission) -> None:
        self._dependency_resolver.validate_generic_crafting_registry(admission)

    def extension_registry(self, extension, getter_name: str):
        return self._dependency_resolver.extension_registry(
            extension,
            getter_name,
        )


__all__ = ("MinecraftChatClefRouterComponentGraph",)
