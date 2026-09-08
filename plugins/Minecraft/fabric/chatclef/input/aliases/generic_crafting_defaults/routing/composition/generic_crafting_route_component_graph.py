#20260905_kpopmodder: Assemble Feature-B route stages outside its public owner facade.
from plugins.Minecraft.fabric.chatclef.input.ownership.item_command import (
    ItemCommandOwnershipClassifier,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandFeedbackStartDecisionDecorator,
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackSubmissionObserver,
)

from ...generic_crafting_defaults_candidate_detector import (
    GenericCraftingDefaultsCandidateDetector,
)
from ...generic_crafting_defaults_profile import GenericCraftingDefaultsProfile
from ..activation import (
    GenericCraftingActivationAdmissionStage,
    GenericCraftingScopedCapabilityAvailabilityStage,
)
from ..candidate import (
    GenericCraftingCandidateDetectionStage,
    GenericCraftingCandidateOwnershipClassificationStage,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing.generic_crafting_route_failure_handler import GenericCraftingRouteFailureHandler
from ..generic_crafting_activation_stage import GenericCraftingActivationStage
from ..generic_crafting_candidate_ownership_stage import (
    GenericCraftingCandidateOwnershipStage,
)
from ..generic_crafting_dispatch_lifecycle import GenericCraftingDispatchLifecycle
from ..generic_crafting_reconciliation_stage import (
    GenericCraftingReconciliationStage,
)
from ..generic_crafting_route_pipeline import GenericCraftingRoutePipeline
from ..generic_crafting_submission_stage import GenericCraftingSubmissionStage
from ..generic_crafting_translation_stage import GenericCraftingTranslationStage
from ..lifecycle import (
    GenericCraftingDispatchCloseLifecycle,
    GenericCraftingReceiptCleanup,
    GenericCraftingRouteExecutionLifecycle,
)
from ..submission import (
    GenericCraftingSubmissionDelivery,
    GenericCraftingSubmissionReadinessStage,
    GenericCraftingSubmissionReconciliationObserver,
    GenericCraftingSubmissionResultProjector,
)
from ..translation import (
    GenericCraftingTranslationInvoker,
    GenericCraftingTranslationResultStage,
)
from .generic_crafting_route_compatibility_installer import (
    GenericCraftingRouteCompatibilityInstaller,
)


class GenericCraftingRouteComponentGraph:
    def __init__(
        self,
        *,
        extension,
        admission,
        translation_boundary,
        submission_precheck,
        submission_boundary,
        submission_reconciliation,
        decision_factory,
        profile=None,
        candidate_detector=None,
        ownership_classifier=None,
    ):
        self._compatibility_installer = (
            GenericCraftingRouteCompatibilityInstaller()
        )
        self.extension = extension
        self.admission = admission
        self.translation_boundary = translation_boundary
        self.submission_precheck = submission_precheck
        self.submission_boundary = submission_boundary
        self.submission_reconciliation = submission_reconciliation
        self.decision_factory = decision_factory
        self.profile = profile or GenericCraftingDefaultsProfile()
        self.candidate_detector = (
            candidate_detector
            or GenericCraftingDefaultsCandidateDetector(profile=self.profile)
        )
        self.ownership_classifier = (
            ownership_classifier or ItemCommandOwnershipClassifier()
        )
        self.candidate_detection_stage = GenericCraftingCandidateDetectionStage(
            self.candidate_detector
        )
        self.candidate_ownership_classification_stage = (
            GenericCraftingCandidateOwnershipClassificationStage(
                self.ownership_classifier
            )
        )
        self.candidate_ownership_stage = GenericCraftingCandidateOwnershipStage(
            candidate_detector=self.candidate_detector,
            ownership_classifier=self.ownership_classifier,
            detection_stage=self.candidate_detection_stage,
            ownership_stage=self.candidate_ownership_classification_stage,
        )
        self.reconciliation_stage = GenericCraftingReconciliationStage(
            submission_reconciliation=self.submission_reconciliation,
            decision_factory=self.decision_factory,
        )
        self.capability_stage = (
            GenericCraftingScopedCapabilityAvailabilityStage(
                extension=self.extension,
                translation_boundary=self.translation_boundary,
                submission_boundary=self.submission_boundary,
                decision_factory=self.decision_factory,
            )
        )
        self.activation_admission_stage = GenericCraftingActivationAdmissionStage(
            admission=self.admission,
            decision_factory=self.decision_factory,
        )
        self.activation_stage = GenericCraftingActivationStage(
            extension=self.extension,
            admission=self.admission,
            translation_boundary=self.translation_boundary,
            submission_boundary=self.submission_boundary,
            decision_factory=self.decision_factory,
            capability_stage=self.capability_stage,
            admission_stage=self.activation_admission_stage,
        )
        self.translation_invoker = GenericCraftingTranslationInvoker(
            extension=self.extension,
            translation_boundary=self.translation_boundary,
            profile=self.profile,
        )
        self.translation_result_stage = GenericCraftingTranslationResultStage(
            translation_boundary=self.translation_boundary,
            decision_factory=self.decision_factory,
        )
        self.translation_stage = GenericCraftingTranslationStage(
            extension=self.extension,
            translation_boundary=self.translation_boundary,
            decision_factory=self.decision_factory,
            profile=self.profile,
            translation_invoker=self.translation_invoker,
            result_stage=self.translation_result_stage,
        )
        self.submission_readiness_stage = (
            GenericCraftingSubmissionReadinessStage(
                extension=self.extension,
                submission_precheck=self.submission_precheck,
                decision_factory=self.decision_factory,
            )
        )
        self.submission_delivery = GenericCraftingSubmissionDelivery(
            extension=self.extension,
            submission_boundary=self.submission_boundary,
        )
        self.submission_reconciliation_observer = (
            GenericCraftingSubmissionReconciliationObserver(
                self.submission_reconciliation
            )
        )
        self.submission_result_projector = (
            GenericCraftingSubmissionResultProjector(self.decision_factory)
        )
        self.command_feedback_descriptor_factory = CommandFeedbackDescriptorFactory()
        self.command_feedback_observer = CommandFeedbackSubmissionObserver(
            extension=self.extension,
            admission_coordinator=CommandFeedbackAdmissionCoordinator(
                live_proof_validator=lambda _proof, _event: False,
                descriptor_factory=self.command_feedback_descriptor_factory,
            ),
        )
        self.command_feedback_start_decorator = (
            CommandFeedbackStartDecisionDecorator(
                CommandLifecycleResponseRenderer()
            )
        )
        self.submission_stage = GenericCraftingSubmissionStage(
            extension=self.extension,
            submission_precheck=self.submission_precheck,
            submission_boundary=self.submission_boundary,
            submission_reconciliation=self.submission_reconciliation,
            decision_factory=self.decision_factory,
            readiness_stage=self.submission_readiness_stage,
            delivery=self.submission_delivery,
            reconciliation_observer=(
                self.submission_reconciliation_observer
            ),
            result_projector=self.submission_result_projector,
            command_feedback=self.command_feedback_observer,
            descriptor_factory=self.command_feedback_descriptor_factory,
            start_decision_decorator=self.command_feedback_start_decorator,
        )
        self.receipt_cleanup = GenericCraftingReceiptCleanup(
            self.admission.activation_registry
        )
        self.dispatch_close_lifecycle = GenericCraftingDispatchCloseLifecycle(
            self.admission.activation_registry
        )
        self.dispatch_lifecycle = GenericCraftingDispatchLifecycle(
            self.admission.activation_registry,
            receipt_cleanup=self.receipt_cleanup,
            dispatch_close_lifecycle=self.dispatch_close_lifecycle,
        )
        self.failure_handler = GenericCraftingRouteFailureHandler(
            self.decision_factory
        )
        self.execution_lifecycle = GenericCraftingRouteExecutionLifecycle(
            failure_handler=self.failure_handler,
            receipt_cleanup=self.receipt_cleanup,
        )
        self.pipeline = GenericCraftingRoutePipeline(
            candidate_ownership_stage=self.candidate_ownership_stage,
            reconciliation_stage=self.reconciliation_stage,
            activation_stage=self.activation_stage,
            translation_stage=self.translation_stage,
            submission_stage=self.submission_stage,
            dispatch_lifecycle=self.dispatch_lifecycle,
            decision_factory=self.decision_factory,
            execution_lifecycle=self.execution_lifecycle,
        )

    def install_compatibility_seams(self, owner) -> None:
        self._compatibility_installer.install(owner, self)


__all__ = ("GenericCraftingRouteComponentGraph",)
