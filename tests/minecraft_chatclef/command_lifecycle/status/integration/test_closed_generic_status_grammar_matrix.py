#20260908_kpopmodder: Lock the contract-owned generic STATUS grammar independently from implementation constants.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQuery,
    CommandStatusQueryClassifier,
    GenericCommandStatusQuestionMatcher,
)


EXACT_GENERIC_BODIES = (
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


class ClosedGenericStatusGrammarMatrixTests(unittest.TestCase):
    def test_exact_body_now_and_longest_addressee_cross_product(self):
        classifier = CommandStatusQueryClassifier()

        self.assertEqual(
            frozenset(EXACT_GENERIC_BODIES),
            GenericCommandStatusQuestionMatcher._BODIES,
        )
        self.assertEqual(32, len(EXACT_GENERIC_BODIES))
        for body in EXACT_GENERIC_BODIES:
            for now in ("", "지금 "):
                for addressee in ("", "마크 ai ", "마인크래프트 ", "마크 "):
                    with self.subTest(
                        body=body,
                        now=now,
                        addressee=addressee,
                    ):
                        self.assertEqual(
                            CommandStatusQuery("any", bool(addressee)),
                            classifier.classify(f"{addressee}{now}{body}?"),
                        )

    def test_terminal_punctuation_is_soft_but_internal_or_chained_syntax_is_not(self):
        classifier = CommandStatusQueryClassifier()

        for text in (
            "뭐 해",
            "뭐 해?",
            "뭐 해?!",
            "  마크 ＡＩ   지금 무슨 작업 중이야，。！？  ",
        ):
            with self.subTest(text=text, accepted=True):
                self.assertIsNotNone(classifier.classify(text))
        for text in (
            "뭐, 해?",
            "뭐\t해?",
            "뭐\n해?",
            "마크 마크 지금 뭐 해?",
            "지금 지금 뭐 해?",
            "뭐 해 그리고 철 10개 구해줘",
            '"뭐 해"',
            "뭐 해; 철 캐줘",
        ):
            with self.subTest(text=text, accepted=False):
                self.assertIsNone(classifier.classify(text))


if __name__ == "__main__":
    unittest.main()
