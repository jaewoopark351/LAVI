#20260905_kpopmodder: Own the immutable five-rule request-local generic crafting profile.
from __future__ import annotations

from types import MappingProxyType

from plugins.Minecraft.fabric.chatclef.intent.scoped_resolution import (
    ScopedItemResolution,
)

from .generic_crafting_default_rule import GenericCraftingDefaultRule


class GenericCraftingDefaultsProfile:
    POLICY_ID = "generic_crafting_defaults_v1"
    _RULES = (
        GenericCraftingDefaultRule("generic_trapdoor", "다락문", "trapdoor"),
        GenericCraftingDefaultRule("generic_empty_map", "지도", "map"),
        GenericCraftingDefaultRule(
            "generic_wooden_pressure_plate",
            "압력판",
            "wooden_pressure_plate",
        ),
        GenericCraftingDefaultRule(
            "generic_foot_plate",
            "발판",
            "wooden_pressure_plate",
        ),
        GenericCraftingDefaultRule(
            "generic_wooden_button",
            "버튼",
            "wooden_button",
        ),
    )
    _BY_PHRASE = MappingProxyType({rule.item_phrase: rule for rule in _RULES})

    @property
    def policy_id(self) -> str:
        return self.POLICY_ID

    @property
    def rules(self) -> tuple[GenericCraftingDefaultRule, ...]:
        return self._RULES

    def rule_for(self, item_phrase: object) -> GenericCraftingDefaultRule | None:
        if type(item_phrase) is not str:
            return None
        return self._BY_PHRASE.get(item_phrase)

    def resolve_exact(self, item_phrase: object) -> ScopedItemResolution | None:
        rule = self.rule_for(item_phrase)
        if rule is None:
            return None
        return ScopedItemResolution(
            profile_id=self.POLICY_ID,
            rule_id=rule.rule_id,
            item_phrase=rule.item_phrase,
            target=rule.canonical_target,
        )
