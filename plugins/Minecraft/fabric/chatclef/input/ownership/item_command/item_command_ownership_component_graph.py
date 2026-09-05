#20260905_kpopmodder: Assemble item-command ownership collaborators only.
from plugins.Minecraft.fabric.chatclef.input.ownership.item_command.generic_crafting_candidate_ownership_classifier import GenericCraftingCandidateOwnershipClassifier
from plugins.Minecraft.fabric.chatclef.input.ownership.item_command.translation_rejection import (
    ItemCommandTranslationRejectionOwnershipClassifier,
    KoreanItemCommandRejectionRenderer,
)


class ItemCommandOwnershipComponentGraph:
    def __init__(
        self,
        *,
        generic_crafting_classifier=None,
        translation_rejection_classifier=None,
        rejection_renderer=None,
    ):
        self.rejection_renderer = (
            rejection_renderer or KoreanItemCommandRejectionRenderer()
        )
        self.generic_crafting_classifier = (
            generic_crafting_classifier
            or GenericCraftingCandidateOwnershipClassifier()
        )
        self.translation_rejection_classifier = (
            translation_rejection_classifier
            or ItemCommandTranslationRejectionOwnershipClassifier(
                self.rejection_renderer
            )
        )


__all__ = ("ItemCommandOwnershipComponentGraph",)
