#20260905_kpopmodder: Classify generic-crafting candidates only.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.generic_crafting_defaults_candidate import (
    GenericCraftingDefaultsCandidate,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_numeric_constraints import (
    JAVA_INT_MAX,
)

from plugins.Minecraft.fabric.chatclef.input.ownership.item_command.item_command_ownership import ItemCommandOwnership
from plugins.Minecraft.fabric.chatclef.input.ownership.item_command.item_command_ownership_decision import ItemCommandOwnershipDecision


class GenericCraftingCandidateOwnershipClassifier:
    def classify(
        self,
        candidate: GenericCraftingDefaultsCandidate,
    ) -> ItemCommandOwnershipDecision:
        intent = candidate.deterministic_intent
        if (
            not candidate.matches_profile
            or candidate.verb_class != "craft"
            or intent.intent_type is not ChatClefIntentType.GET_ITEM
            or intent.source != "rule"
            or intent.language != "ko"
        ):
            return ItemCommandOwnershipDecision(
                ItemCommandOwnership.UNRELATED,
                "generic_crafting_defaults_unrelated",
                candidate=candidate,
            )
        if not candidate.quantity_shape.valid:
            return ItemCommandOwnershipDecision(
                ItemCommandOwnership.OWNED_INVALID,
                candidate.quantity_shape.reason_code,
                "제작 수량은 지원되는 표현 하나만 사용할 수 있어요.",
                candidate,
            )
        rule = candidate.rule
        if rule is None or intent.item_phrase != rule.item_phrase:
            return ItemCommandOwnershipDecision(
                ItemCommandOwnership.OWNED_INVALID,
                "generic_crafting_item_phrase_mismatch",
                "제작할 아이템 표현을 정확히 확인하지 못했어요.",
                candidate,
            )
        quantity = intent.quantity
        if type(quantity) is not int or quantity < 1 or quantity > JAVA_INT_MAX:
            return ItemCommandOwnershipDecision(
                ItemCommandOwnership.OWNED_INVALID,
                "generic_crafting_invalid_quantity",
                "제작 수량은 1부터 2147483647 사이여야 해요.",
                candidate,
            )
        return ItemCommandOwnershipDecision(
            ItemCommandOwnership.OWNED_VALID,
            "generic_crafting_defaults_candidate",
            candidate=candidate,
        )


__all__ = ("GenericCraftingCandidateOwnershipClassifier",)
