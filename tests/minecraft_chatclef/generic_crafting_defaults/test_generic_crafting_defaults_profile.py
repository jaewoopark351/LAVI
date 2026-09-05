#20260905_kpopmodder: Verify exact craft-only defaults without changing global Korean aliases.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults import (
    GenericCraftingDefaultsCandidateDetector,
    GenericCraftingDefaultsProfile,
)
from plugins.Minecraft.fabric.chatclef.input.ownership.item_command import (
    ItemCommandOwnership,
    ItemCommandOwnershipClassifier,
)
from plugins.Minecraft.fabric.chatclef.input.ownership.item_command.item_command_ownership_component_graph import (
    ItemCommandOwnershipComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.input.ownership.item_command.generic_crafting_candidate_ownership_classifier import (
    GenericCraftingCandidateOwnershipClassifier,
)
from plugins.Minecraft.fabric.chatclef.input.ownership.item_command.translation_rejection import (
    ItemCommandTranslationRejectionOwnershipClassifier,
    KoreanItemCommandRejectionRenderer,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.intent.korean_chatclef_rule_parser import (
    KoreanChatClefRuleParser,
)
from plugins.Minecraft.fabric.chatclef.intent.scoped_resolution import (
    ScopedItemResolutionTranslationService,
)
from plugins.Minecraft.tools.generate_chatclef_korean_item_artifacts import (
    CURATED_ALIASES,
)


class GenericCraftingDefaultsProfileTests(unittest.TestCase):
    def setUp(self):
        self.profile = GenericCraftingDefaultsProfile()
        self.detector = GenericCraftingDefaultsCandidateDetector(
            profile=self.profile
        )
        self.classifier = ItemCommandOwnershipClassifier()

    def test_exact_profile_uses_canonical_non_oak_targets(self):
        expected = {
            "다락문": ("generic_trapdoor", "trapdoor"),
            "지도": ("generic_empty_map", "map"),
            "압력판": (
                "generic_wooden_pressure_plate",
                "wooden_pressure_plate",
            ),
            "발판": ("generic_foot_plate", "wooden_pressure_plate"),
            "버튼": ("generic_wooden_button", "wooden_button"),
        }

        self.assertEqual("generic_crafting_defaults_v1", self.profile.policy_id)
        self.assertEqual(
            expected,
            {
                rule.item_phrase: (rule.rule_id, rule.canonical_target)
                for rule in self.profile.rules
            },
        )
        self.assertNotIn("oak_trapdoor", {value[1] for value in expected.values()})

    def test_ownership_facade_delegates_to_independent_policies(self):
        self.assertIsInstance(
            self.classifier._component_graph,
            ItemCommandOwnershipComponentGraph,
        )
        self.assertIsInstance(
            self.classifier._generic_crafting_classifier,
            GenericCraftingCandidateOwnershipClassifier,
        )
        self.assertIsInstance(
            self.classifier._translation_rejection_classifier,
            ItemCommandTranslationRejectionOwnershipClassifier,
        )
        self.assertIsInstance(
            self.classifier._rejection_renderer,
            KoreanItemCommandRejectionRenderer,
        )

    def test_deterministic_craft_candidates_keep_quantity(self):
        cases = {
            "다락문 만들어줘": ("trapdoor", 1),
            "지도 3개 만들어줘": ("map", 3),
            "압력판 두 개 제작해 주세요": ("wooden_pressure_plate", 2),
            "발판 4개 만들어줘": ("wooden_pressure_plate", 4),
            "버튼 ５개 제작해줘": ("wooden_button", 5),
            "지도 ＋２개 만들어줘": ("map", 2),
        }
        for text, expected in cases.items():
            with self.subTest(text=text):
                candidate = self.detector.inspect(text)
                decision = self.classifier.classify(candidate)
                self.assertEqual(ItemCommandOwnership.OWNED_VALID, decision.ownership)
                self.assertEqual(expected[0], candidate.rule.canonical_target)
                self.assertEqual(expected[1], candidate.deterministic_intent.quantity)

    def test_acquire_mining_specific_and_everyday_phrases_are_unrelated(self):
        texts = [
            "다락문 구해줘",
            "지도 캐줘",
            "철 다락문 만들어줘",
            "참나무 압력판 만들어줘",
            "지도 제작대 만들어줘",
            "채워진 지도 만들어줘",
            "탐험 지도 만들어줘",
            "웹 버튼 만들어줘",
            "성공의 발판 만들어줘",
            "get trapdoor 1",
        ]
        for text in texts:
            with self.subTest(text=text):
                decision = self.classifier.classify(self.detector.inspect(text))
                self.assertEqual(ItemCommandOwnership.UNRELATED, decision.ownership)

    def test_invalid_quantity_shapes_are_owned_fail_closed(self):
        texts = [
            "다락문 2개 3개 만들어줘",
            "지도 두 개 세 개 만들어줘",
            "압력판 2개 두 개 만들어줘",
            "발판 ٢개 만들어줘",
            "버튼 ۲개 만들어줘",
            "지도 २개 만들어줘",
            "다락문 0개 만들어줘",
            "지도 -1개 만들어줘",
            "버튼 2147483648개 만들어줘",
        ]
        for text in texts:
            with self.subTest(text=text):
                decision = self.classifier.classify(self.detector.inspect(text))
                self.assertEqual(
                    ItemCommandOwnership.OWNED_INVALID,
                    decision.ownership,
                )

    def test_scoped_resolution_does_not_enable_global_aliases(self):
        service = ChatClefNaturalLanguageService()
        global_snapshot = dict(CURATED_ALIASES)

        for rule in self.profile.rules:
            with self.subTest(item_phrase=rule.item_phrase):
                ordinary = service.translate(f"{rule.item_phrase} 만들어줘")
                scoped = service.translate_with_item_resolution_profile(
                    f"{rule.item_phrase} 만들어줘",
                    self.profile,
                )
                self.assertFalse(ordinary.executable)
                self.assertTrue(scoped.executable)
                self.assertEqual(
                    f"get {rule.canonical_target} 1",
                    scoped.command,
                )

        self.assertEqual(global_snapshot, CURATED_ALIASES)
        for rule in self.profile.rules:
            self.assertNotIn(rule.item_phrase, CURATED_ALIASES)

    def test_scoped_translation_service_isolated_from_legacy_translate(self):
        parser = _RecordingScopedRuleParser()
        service = ChatClefNaturalLanguageService(scoped_rule_parser=parser)

        ordinary = service.translate("다이아몬드 가져와줘")

        self.assertTrue(ordinary.executable)
        self.assertEqual([], parser.calls)
        self.assertIsInstance(
            service._scoped_translation_service,
            ScopedItemResolutionTranslationService,
        )

        scoped = service.translate_with_item_resolution_profile(
            "다락문 만들어줘",
            self.profile,
        )

        self.assertTrue(scoped.executable)
        self.assertEqual(["다락문 만들어줘"], parser.calls)

    def test_specific_material_aliases_keep_existing_precedence(self):
        service = ChatClefNaturalLanguageService()
        cases = {
            "철 다락문 만들어줘": "get iron_trapdoor 1",
            "참나무 다락문 만들어줘": "get oak_trapdoor 1",
            "빈 지도 만들어줘": "get map 1",
            "지도 제작대 만들어줘": "get cartography_table 1",
            "돌 압력판 만들어줘": "get stone_pressure_plate 1",
            "참나무 압력판 만들어줘": "get oak_pressure_plate 1",
            "돌 버튼 만들어줘": "get stone_button 1",
            "참나무 버튼 만들어줘": "get oak_button 1",
        }
        for text, command in cases.items():
            with self.subTest(text=text):
                result = service.translate_with_item_resolution_profile(
                    text,
                    self.profile,
                )
                self.assertTrue(result.executable)
                self.assertEqual(command, result.command)

class _RecordingScopedRuleParser:
    def __init__(self):
        self.calls = []
        self._parser = KoreanChatClefRuleParser()

    def parse(self, text):
        self.calls.append(text)
        return self._parser.parse(text)


if __name__ == "__main__":
    unittest.main()
