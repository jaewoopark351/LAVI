#20260905_kpopmodder: Project a validated activation context only.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)

from ...generic_crafting_defaults_profile import GenericCraftingDefaultsProfile
from .generic_crafting_activation_validated_input import (
    GenericCraftingActivationValidatedInput,
)


class GenericCraftingActivationContextProjector:
    def __init__(self, normalizer: KoreanTextNormalizer | None = None):
        self.normalizer = normalizer or KoreanTextNormalizer()

    def project(
        self,
        validated_input: GenericCraftingActivationValidatedInput,
    ) -> dict[str, object]:
        rule = validated_input.rule
        intent = validated_input.intent
        raw_text = validated_input.raw_text
        translation_input_text = raw_text.strip()
        return {
            "event_id": validated_input.event_id,
            "source": validated_input.source,
            "provider_id": validated_input.provider_id,
            "event_kind": validated_input.event_kind,
            "final": validated_input.final,
            "raw_event_text": raw_text,
            "translation_input_text": translation_input_text,
            "expected_intent_original_text": self.normalizer.normalize(
                translation_input_text,
                lowercase_english=False,
            ),
            "policy_id": GenericCraftingDefaultsProfile.POLICY_ID,
            "rule_id": rule.rule_id,
            "item_phrase": rule.item_phrase,
            "canonical_target": rule.canonical_target,
            "quantity": intent.quantity,
        }


__all__ = ("GenericCraftingActivationContextProjector",)
