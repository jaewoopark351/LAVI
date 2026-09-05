#20260905_kpopmodder: Assemble ordinary-route stages outside its public facade.
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.ordinary_route_availability_stage import OrdinaryRouteAvailabilityStage
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.ordinary_minecraft_command_route_pipeline import OrdinaryMinecraftCommandRoutePipeline
from ..precheck import OrdinarySubmissionPrecheckStage
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.ordinary_submission_reconciliation_stage import OrdinarySubmissionReconciliationStage
from ..rejection import OrdinaryTranslationRejectionStage
from ..submission import OrdinarySubmissionResultStage
from ..translation import OrdinaryTranslationStage
from .ordinary_minecraft_command_route_compatibility_installer import (
    OrdinaryMinecraftCommandRouteCompatibilityInstaller,
)


class OrdinaryMinecraftCommandRouteComponentGraph:
    def __init__(
        self,
        *,
        extension,
        translation_boundary,
        submission_precheck,
        submission_boundary,
        submission_route_lock,
        submission_reconciliation,
        decision_factory,
        item_command_ownership_classifier,
        item_command_rejection_evidence_parser,
        live_proof_validator,
        failure_handler,
        router_logger,
    ):
        self._compatibility_installer = (
            OrdinaryMinecraftCommandRouteCompatibilityInstaller()
        )
        self.extension = extension
        self.translation_boundary = translation_boundary
        self.submission_precheck = submission_precheck
        self.submission_boundary = submission_boundary
        self.submission_route_lock = submission_route_lock
        self.submission_reconciliation = submission_reconciliation
        self.decision_factory = decision_factory
        self.item_command_ownership_classifier = item_command_ownership_classifier
        self.item_command_rejection_evidence_parser = (
            item_command_rejection_evidence_parser
        )
        self.live_proof_validator = live_proof_validator
        self.failure_handler = failure_handler
        self.router_logger = router_logger

        self.availability_stage = OrdinaryRouteAvailabilityStage(
            extension=extension,
            translation_boundary=translation_boundary,
            submission_boundary=submission_boundary,
            router_logger=router_logger,
        )
        self.reconciliation_stage = OrdinarySubmissionReconciliationStage(
            submission_reconciliation=submission_reconciliation,
            decision_factory=decision_factory,
            router_logger=router_logger,
        )
        self.translation_stage = OrdinaryTranslationStage(
            extension=extension,
            translation_boundary=translation_boundary,
            decision_factory=decision_factory,
            failure_handler=failure_handler,
            router_logger=router_logger,
        )
        self.rejection_stage = OrdinaryTranslationRejectionStage(
            translation_boundary=translation_boundary,
            decision_factory=decision_factory,
            item_command_ownership_classifier=item_command_ownership_classifier,
            item_command_rejection_evidence_parser=(
                item_command_rejection_evidence_parser
            ),
            live_proof_validator=live_proof_validator,
        )
        self.precheck_stage = OrdinarySubmissionPrecheckStage(
            extension=extension,
            submission_precheck=submission_precheck,
            decision_factory=decision_factory,
            router_logger=router_logger,
        )
        self.submission_result_stage = OrdinarySubmissionResultStage(
            extension=extension,
            submission_boundary=submission_boundary,
            submission_reconciliation=submission_reconciliation,
            decision_factory=decision_factory,
            failure_handler=failure_handler,
            router_logger=router_logger,
        )
        self.pipeline = OrdinaryMinecraftCommandRoutePipeline(
            reconciliation_stage=self.reconciliation_stage,
            translation_stage=self.translation_stage,
            rejection_stage=self.rejection_stage,
            precheck_stage=self.precheck_stage,
            submission_result_stage=self.submission_result_stage,
        )

    def install_compatibility_seams(self, owner) -> None:
        self._compatibility_installer.install(owner, self)


__all__ = ("OrdinaryMinecraftCommandRouteComponentGraph",)
