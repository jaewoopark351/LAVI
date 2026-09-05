#20260905_kpopmodder: Validate one translation projection against its activation receipt.
from __future__ import annotations

from ...generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)
from ...generic_crafting_defaults_translation_projection import (
    GenericCraftingDefaultsTranslationProjection,
)


class GenericCraftingActivationProjectionValidator:
    def matches(
        self,
        projection: GenericCraftingDefaultsTranslationProjection,
        receipt: GenericCraftingDefaultsActivationReceipt,
    ) -> bool:
        return (
            projection.status == "validated"
            and projection.executable is True
            and projection.command
            == f"get {receipt.canonical_target} {receipt.quantity}"
            and projection.intent_type == "get_item"
            and projection.intent_source == "rule"
            and projection.intent_language == "ko"
            and projection.intent_original_text
            == receipt.expected_intent_original_text
            and projection.item_phrase == receipt.item_phrase
            and projection.quantity == receipt.quantity
            and projection.resolved_target == receipt.canonical_target
        )


__all__ = ("GenericCraftingActivationProjectionValidator",)
