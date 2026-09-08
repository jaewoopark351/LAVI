#20260908_kpopmodder: Characterize existing target-qualified STATUS grammar after extraction.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.classification.target import (
    TargetCommandStatusQuestionMatcher,
)


class TargetCommandStatusQuestionMatcherTests(unittest.TestCase):
    def test_preserves_supported_family_and_target_extraction(self):
        matcher = TargetCommandStatusQuestionMatcher()
        expected = {
            "다이아 곡괭이 만드는 중이야": ("item_get", "다이아 곡괭이"),
            "철 10개 구하고 있어": ("item_get", "철 10개"),
            "참나무 원목 보관하는 중이야": ("item_deposit", "참나무 원목"),
            "다이아 흉갑 장착하고 있어": ("item_equip", "다이아 흉갑"),
            "참나무 원목 2개 건네는 중이야": ("item_give", "참나무 원목 2개"),
            "Steve를 따라가는 중이야": ("movement_follow", "Steve를"),
            "지금 다이아 곡괭이 만드는 중이야": ("item_get", "다이아 곡괭이"),
        }
        for text, match in expected.items():
            with self.subTest(text=text):
                self.assertEqual(match, matcher.match(text))

    def test_rejects_empty_targets_and_imperatives(self):
        matcher = TargetCommandStatusQuestionMatcher()
        for text in ("만드는 중이야", "철 10개 구해줘", "Steve 따라가줘"):
            with self.subTest(text=text):
                self.assertIsNone(matcher.match(text))


if __name__ == "__main__":
    unittest.main()
