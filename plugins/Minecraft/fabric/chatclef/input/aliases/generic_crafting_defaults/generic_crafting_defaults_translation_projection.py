#20260905_kpopmodder: Copy the exact immutable authorization projection from a validated translation.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


@dataclass(frozen=True)
class GenericCraftingDefaultsTranslationProjection:
    binding_version: str
    status: str
    executable: bool
    command: str
    intent_type: str
    intent_source: str
    intent_language: str
    intent_original_text: str
    item_phrase: str
    quantity: int
    resolved_target: str

    BINDING_VERSION = "generic_crafting_defaults_translation_v1"

    @classmethod
    def from_value(
        cls,
        value: Any,
    ) -> "GenericCraftingDefaultsTranslationProjection":
        translation = ChatClefTranslationResultDTO.from_mapping(value)
        intent = translation.intent
        if (
            translation.status is not ChatClefIntentStatus.VALIDATED
            or translation.executable is not True
            or intent is None
            or intent.intent_type is not ChatClefIntentType.GET_ITEM
            or intent.source != "rule"
            or intent.language != "ko"
            or intent.food_units is not None
            or intent.x is not None
            or intent.y is not None
            or intent.z is not None
            or intent.player_name != ""
            or bool(intent.slots)
            or type(intent.quantity) is not int
            or type(translation.command) is not str
            or type(translation.resolved_target) is not str
        ):
            raise ValueError("invalid_generic_crafting_translation_projection")
        return cls(
            binding_version=cls.BINDING_VERSION,
            status=translation.status.value,
            executable=True,
            command=translation.command,
            intent_type=intent.intent_type.value,
            intent_source=intent.source,
            intent_language=intent.language,
            intent_original_text=intent.original_text,
            item_phrase=intent.item_phrase,
            quantity=intent.quantity,
            resolved_target=translation.resolved_target,
        )
