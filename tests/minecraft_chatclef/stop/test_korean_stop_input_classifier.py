#20260905_kpopmodder: Verify exact whole-utterance STOP grammar and raw guards.
import unittest

from plugins.Minecraft.fabric.chatclef.input.stop import (
    KoreanStopInputClassifier,
    StopInputDecisionKind,
)


class KoreanStopInputClassifierTests(unittest.TestCase):
    def setUp(self):
        self.classifier = KoreanStopInputClassifier()

    def test_accepts_ten_exact_phrases_and_one_adjacent_terminal_suffix(self):
        phrases = (
            "멈춰",
            "멈춰줘",
            "중지",
            "정지",
            "스톱",
            "그만",
            "마크 AI 멈춰",
            "마크 ai 멈춰줘",
            "마인크래프트 AI 멈춰",
            "마인크래프트 ai 멈춰줘",
        )
        for phrase in phrases:
            with self.subTest(phrase=phrase):
                self.assertTrue(self.classifier.classify(phrase).valid)
                self.assertTrue(self.classifier.classify(phrase + "!").valid)

    def test_guards_questions_quotes_compounds_and_internal_whitespace(self):
        values = (
            "멈춰?",
            "멈춰？",
            "“멈춰?”",
            "멈춰!!",
            "멈춰 !",
            "다이아를 캐고 멈춰",
            "멈춰 그리고 지도 만들어줘",
            "마크\tAI 멈춰",
            "멈춰\u200b",
            "\x1c멈춰",
        )
        for value in values:
            with self.subTest(value=value):
                self.assertEqual(
                    self.classifier.classify(value).kind,
                    StopInputDecisionKind.GUARDED_STOP_LIKE_REJECTION,
                )

    def test_unrelated_conversation_is_not_owned(self):
        for value in ("버튼 디자인은 어떤 게 좋아?", "그만큼 만들어줘", "stop", ""):
            with self.subTest(value=value):
                self.assertEqual(
                    self.classifier.classify(value).kind,
                    StopInputDecisionKind.UNRELATED,
                )

    def test_phrase_table_is_immutable(self):
        with self.assertRaises(TypeError):
            self.classifier._PHRASES["멈춰"] = "changed"

        self.assertTrue(self.classifier.classify("멈춰").valid)


if __name__ == "__main__":
    unittest.main()
