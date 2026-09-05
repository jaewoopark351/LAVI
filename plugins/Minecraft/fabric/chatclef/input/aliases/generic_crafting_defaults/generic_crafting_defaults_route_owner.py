#20260905_kpopmodder: Preserve Feature-B route ownership as a thin API facade.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.input.ownership.item_command import (
    ItemCommandOwnershipClassifier,
)
from plugins.Minecraft.fabric.chatclef.input.routing import (
    MinecraftChatClefRouteDecisionFactory,
    MinecraftChatClefSubmissionBoundary,
    MinecraftChatClefSubmissionPrecheck,
    MinecraftChatClefSubmissionReconciliationCoordinator,
    MinecraftChatClefTranslationBoundary,
)

from .generic_crafting_defaults_admission import GenericCraftingDefaultsAdmission
from .generic_crafting_defaults_candidate_detector import (
    GenericCraftingDefaultsCandidateDetector,
)
from .generic_crafting_defaults_profile import GenericCraftingDefaultsProfile
from .routing.composition import (
    GenericCraftingRouteComponentGraph,
)


class GenericCraftingDefaultsRouteOwner:
    def __init__(
        self,
        *,
        extension: Any,
        admission: GenericCraftingDefaultsAdmission,
        translation_boundary: MinecraftChatClefTranslationBoundary,
        submission_precheck: MinecraftChatClefSubmissionPrecheck,
        submission_boundary: MinecraftChatClefSubmissionBoundary,
        submission_reconciliation: MinecraftChatClefSubmissionReconciliationCoordinator,
        decision_factory: MinecraftChatClefRouteDecisionFactory,
        profile: GenericCraftingDefaultsProfile | None = None,
        candidate_detector: GenericCraftingDefaultsCandidateDetector | None = None,
        ownership_classifier: ItemCommandOwnershipClassifier | None = None,
    ):
        self._component_graph = GenericCraftingRouteComponentGraph(
            extension=extension,
            admission=admission,
            translation_boundary=translation_boundary,
            submission_precheck=submission_precheck,
            submission_boundary=submission_boundary,
            submission_reconciliation=submission_reconciliation,
            decision_factory=decision_factory,
            profile=profile,
            candidate_detector=candidate_detector,
            ownership_classifier=ownership_classifier,
        )
        self._component_graph.install_compatibility_seams(self)

    @property
    def activation_registry(self):
        return self._dispatch_lifecycle.activation_registry

    def try_route(
        self,
        event: object,
        korean_eligibility_proof: object,
    ):
        return self._pipeline.try_route(event, korean_eligibility_proof)

    def close_dispatch(self, korean_eligibility_proof: object) -> None:
        self._dispatch_lifecycle.close_dispatch(korean_eligibility_proof)

    def _scoped_seams_available(self) -> bool:
        return self._activation_stage.scoped_seams_available()
