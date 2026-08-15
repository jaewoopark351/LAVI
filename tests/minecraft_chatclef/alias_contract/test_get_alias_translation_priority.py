#20260815_kpopmodder: Lock Korean GET alias targets and equipment precedence.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent import (
    ChatClefIntentStatus,
    ChatClefNaturalLanguageService,
)


STANDALONE_ALIAS_CASES = [
    ("다이아 가져와줘", "diamond"),
    ("다이아몬드 가져와줘", "diamond"),
    ("돌 가져와줘", "stone"),
    ("조약돌 가져와줘", "cobblestone"),
    ("레드스톤 가져와줘", "redstone"),
    ("석탄 가져와줘", "coal"),
    ("철 가져와줘", "iron_ingot"),
    ("금 가져와줘", "gold_ingot"),
]

EQUIPMENT_PRECEDENCE_CASES = [
    ("철 곡괭이 가져와줘", "iron_pickaxe"),
    ("철 도끼 가져와줘", "iron_axe"),
    ("금 곡괭이 가져와줘", "golden_pickaxe"),
    ("금 도끼 가져와줘", "golden_axe"),
    ("다이아몬드 곡괭이 가져와줘", "diamond_pickaxe"),
]


class KoreanGetAliasTranslationPriorityTests(unittest.TestCase):
    def test_standalone_aliases_compile_to_concrete_item_targets(self):
        service = ChatClefNaturalLanguageService()

        for text, expected_target in STANDALONE_ALIAS_CASES:
            with self.subTest(text=text):
                result = service.translate(text)

                self.assertTrue(result.executable)
                self.assertEqual(ChatClefIntentStatus.VALIDATED, result.status)
                self.assertEqual(expected_target, result.resolved_target)
                self.assertEqual(f"get {expected_target} 1", result.command)
                self.assertFalse(str(result.command).startswith("@"))

    def test_equipment_phrase_precedence_beats_standalone_material_aliases(self):
        service = ChatClefNaturalLanguageService()

        for text, expected_target in EQUIPMENT_PRECEDENCE_CASES:
            with self.subTest(text=text):
                result = service.translate(text)

                self.assertTrue(result.executable)
                self.assertEqual(ChatClefIntentStatus.VALIDATED, result.status)
                self.assertEqual(expected_target, result.resolved_target)
                self.assertEqual(f"get {expected_target} 1", result.command)

    def test_mining_quantity_uses_same_alias_resolution_as_get(self):
        service = ChatClefNaturalLanguageService()

        result = service.translate("철 10개 캐줘")

        self.assertTrue(result.executable)
        self.assertEqual(ChatClefIntentStatus.VALIDATED, result.status)
        self.assertEqual("iron_ingot", result.resolved_target)
        self.assertEqual("get iron_ingot 10", result.command)


if __name__ == "__main__":
    unittest.main()
