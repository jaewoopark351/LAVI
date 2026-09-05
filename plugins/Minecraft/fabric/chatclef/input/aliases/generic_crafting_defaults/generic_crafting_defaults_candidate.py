#20260905_kpopmodder: Carry one deterministic craft-only profile candidate before ownership classification.
from __future__ import annotations

from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)

from .generic_crafting_default_rule import GenericCraftingDefaultRule
from .generic_crafting_quantity_shape import GenericCraftingQuantityShape


@dataclass(frozen=True)
class GenericCraftingDefaultsCandidate:
    rule: GenericCraftingDefaultRule | None
    deterministic_intent: ChatClefIntentDTO
    verb_class: str
    quantity_shape: GenericCraftingQuantityShape

    @property
    def matches_profile(self) -> bool:
        return self.rule is not None
