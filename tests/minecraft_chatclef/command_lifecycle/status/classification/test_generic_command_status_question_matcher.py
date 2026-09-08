#20260908_kpopmodder: Verify every closed normalized generic STATUS body production.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.classification.generic import (
    GenericCommandStatusQuestionMatcher,
)


class GenericCommandStatusQuestionMatcherTests(unittest.TestCase):
    def test_matches_every_body_with_optional_exact_now_prefix(self):
        matcher = GenericCommandStatusQuestionMatcher()
        bodies = (
            "뭐 해",
            "뭐해",
            "뭐 해요",
            "뭐해요",
            "뭐 하고 있어",
            "뭐하고 있어",
            "뭐 하고 있어요",
            "뭐하고 있어요",
            "뭐 하는 중이야",
            "뭐 하는 중이에요",
            "무슨 작업 하고 있어",
            "무슨 작업을 하고 있어",
            "무슨 작업 하고 있어요",
            "무슨 작업을 하고 있어요",
            "어떤 작업 하고 있어",
            "어떤 작업을 하고 있어",
            "어떤 작업 하고 있어요",
            "어떤 작업을 하고 있어요",
            "무슨 작업 하는 중이야",
            "무슨 작업을 하는 중이야",
            "무슨 작업 하는 중이에요",
            "무슨 작업을 하는 중이에요",
            "어떤 작업 하는 중이야",
            "어떤 작업을 하는 중이야",
            "어떤 작업 하는 중이에요",
            "어떤 작업을 하는 중이에요",
            "무슨 작업 중이야",
            "무슨 작업 중이에요",
            "어떤 작업 중이야",
            "어떤 작업 중이에요",
            "진행 상황 알려줘",
            "진행 상황 알려주세요",
        )
        for body in bodies:
            for now in ("", "지금 "):
                with self.subTest(body=body, now=now):
                    self.assertTrue(matcher.matches(now + body))

    def test_rejects_unlisted_or_mixed_bodies(self):
        matcher = GenericCommandStatusQuestionMatcher()
        for body in (
            "지금 지금 뭐 해",
            "뭐 하니",
            "무슨 일 하고 있어",
            "뭐 해 그리고 철 10개 구해줘",
            "멈춰줘",
            "철 10개 구해줘",
        ):
            with self.subTest(body=body):
                self.assertFalse(matcher.matches(body))


if __name__ == "__main__":
    unittest.main()
