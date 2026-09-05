#20260905_kpopmodder: Preserve item ownership APIs as a thin facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.generic_crafting_defaults_candidate import (
    GenericCraftingDefaultsCandidate,
)

from plugins.Minecraft.fabric.chatclef.input.ownership.item_command.item_command_ownership_component_graph import ItemCommandOwnershipComponentGraph
from .item_command_ownership_decision import ItemCommandOwnershipDecision
from .item_command_translation_rejection_evidence import (
    ItemCommandTranslationRejectionEvidence,
)


class ItemCommandOwnershipClassifier:
    def __init__(
        self,
        *,
        generic_crafting_classifier=None,
        translation_rejection_classifier=None,
        rejection_renderer=None,
        component_graph=None,
    ):
        self._component_graph = component_graph or ItemCommandOwnershipComponentGraph(
            generic_crafting_classifier=generic_crafting_classifier,
            translation_rejection_classifier=translation_rejection_classifier,
            rejection_renderer=rejection_renderer,
        )
        self._rejection_renderer = self._component_graph.rejection_renderer
        self._generic_crafting_classifier = (
            self._component_graph.generic_crafting_classifier
        )
        self._translation_rejection_classifier = (
            self._component_graph.translation_rejection_classifier
        )

    def classify(
        self,
        candidate: GenericCraftingDefaultsCandidate,
    ) -> ItemCommandOwnershipDecision:
        return self._generic_crafting_classifier.classify(candidate)

    def classify_translation_rejection(
        self,
        evidence: ItemCommandTranslationRejectionEvidence,
    ) -> ItemCommandOwnershipDecision:
        return self._translation_rejection_classifier.classify(evidence)

    def _rejection_message(self, status: str) -> str:
        return self._rejection_renderer.render(status)


__all__ = ("ItemCommandOwnershipClassifier",)
