#20260908_kpopmodder: Characterize existing family-specific STATUS grammar after extraction.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.classification.family import (
    FamilyCommandStatusQuestionMatcher,
)


class FamilyCommandStatusQuestionMatcherTests(unittest.TestCase):
    def test_preserves_exact_existing_family_forms(self):
        matcher = FamilyCommandStatusQuestionMatcher()
        expected = {
            "지금 뭐 만들고 있어": "item_get",
            "뭐 만들고 있어": "item_get",
            "지금 뭐 만드는 중이야": "item_get",
            "뭐 만드는 중이야": "item_get",
            "지금 뭐 구하고 있어": "item_get",
            "뭐 구하는 중이야": "item_get",
            "지금 어디로 가고 있어": "movement_goto",
            "어디로 가는 중이야": "movement_goto",
            "아이템 집에 정리하는 중이야": "store_home",
            "집에 정리하고 있어": "store_home",
        }
        for text, family in expected.items():
            with self.subTest(text=text):
                self.assertEqual(family, matcher.match(text))

    def test_does_not_widen_family_specific_grammar(self):
        matcher = FamilyCommandStatusQuestionMatcher()
        for text in ("뭐 만들고 있어요", "어디 가고 있어", "집 정리 중이야"):
            with self.subTest(text=text):
                self.assertIsNone(matcher.match(text))


if __name__ == "__main__":
    unittest.main()
