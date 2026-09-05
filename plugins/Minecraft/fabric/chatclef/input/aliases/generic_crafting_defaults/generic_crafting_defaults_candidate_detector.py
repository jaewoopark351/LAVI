#20260905_kpopmodder: Detect exact deterministic craft candidates without invoking the LLM extractor.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.korean_acquisition_verb_matcher import (
    KoreanAcquisitionVerbMatcher,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_chatclef_rule_parser import (
    KoreanChatClefRuleParser,
)

from .generic_crafting_defaults_candidate import GenericCraftingDefaultsCandidate
from .generic_crafting_defaults_profile import GenericCraftingDefaultsProfile
from .generic_crafting_quantity_shape_guard import (
    GenericCraftingQuantityShapeGuard,
)


class GenericCraftingDefaultsCandidateDetector:
    def __init__(
        self,
        profile: GenericCraftingDefaultsProfile | None = None,
        quantity_guard: GenericCraftingQuantityShapeGuard | None = None,
        rule_parser: KoreanChatClefRuleParser | None = None,
        acquisition_verbs: KoreanAcquisitionVerbMatcher | None = None,
    ):
        self._profile = profile or GenericCraftingDefaultsProfile()
        self._quantity_guard = quantity_guard or GenericCraftingQuantityShapeGuard()
        self._rule_parser = rule_parser or KoreanChatClefRuleParser()
        self._acquisition_verbs = acquisition_verbs or KoreanAcquisitionVerbMatcher()

    def inspect(self, text: object) -> GenericCraftingDefaultsCandidate:
        quantity_shape = self._quantity_guard.inspect(text)
        intent = self._rule_parser.parse(text)
        rule = self._profile.rule_for(intent.item_phrase)
        if rule is None and not quantity_shape.valid:
            stripped_intent = self._rule_parser.parse(quantity_shape.text_without_tokens)
            rule = self._profile.rule_for(stripped_intent.item_phrase)
        return GenericCraftingDefaultsCandidate(
            rule=rule,
            deterministic_intent=intent,
            verb_class=self._acquisition_verbs.classify(text),
            quantity_shape=quantity_shape,
        )
