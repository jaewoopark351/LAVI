#20260803_kpopmodder: Cover deterministic Korean rule parsing before item resolution.
#20260913_kpopmodder: Cover raw complete XYZ grammar and irreversible guarded rejections.
import unittest
from dataclasses import FrozenInstanceError

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_chatclef_rule_parser import (
    KoreanChatClefRuleParser,
)
from plugins.Minecraft.fabric.chatclef.intent.navigation.goto import (
    GotoParseDecision,
    GotoParseResult,
    KoreanGotoCandidateDetector,
    KoreanGotoCoordinateParser,
)
from plugins.Minecraft.fabric.chatclef.intent.navigation.goto.guard import (
    GotoGuardIntentDecoder,
    GotoGuardMarkerDetector,
)


class MinecraftChatClefKoreanRuleParserTests(unittest.TestCase):
    def test_parses_get_item_phrase_and_quantity(self):
        intent = KoreanChatClefRuleParser().parse("다이아몬드 도끼 하나 가져와")

        self.assertEqual(ChatClefIntentType.GET_ITEM, intent.intent_type)
        self.assertEqual("다이아몬드 도끼", intent.item_phrase)
        self.assertEqual(1, intent.quantity)

    def test_parses_mining_phrases_as_get_item(self):
        parser = KoreanChatClefRuleParser()
        cases = {
            "다이아몬드 캐줘": ("다이아몬드", 1),
            "돌 10개 캐줘": ("돌", 10),
            "석탄 5개 캐와줘": ("석탄", 5),
            "레드스톤 5개 채굴해줘": ("레드스톤", 5),
            "철갑바 만들어줘": ("철갑바", 1),
            "횃불 제작해줘": ("횃불", 1),
        }

        for text, expected in cases.items():
            with self.subTest(text=text):
                intent = parser.parse(text)
                self.assertEqual(ChatClefIntentType.GET_ITEM, intent.intent_type)
                self.assertEqual(expected[0], intent.item_phrase)
                self.assertEqual(expected[1], intent.quantity)

    def test_removes_trailing_object_particles_from_item_phrase(self):
        parser = KoreanChatClefRuleParser()
        cases = {
            "철괴를 10개 가져와줘": "철괴",
            "다이아몬드를 가져와줘": "다이아몬드",
            "돌을 10개 캐줘": "돌",
        }

        for text, expected_phrase in cases.items():
            with self.subTest(text=text):
                intent = parser.parse(text)
                self.assertEqual(ChatClefIntentType.GET_ITEM, intent.intent_type)
                self.assertEqual(expected_phrase, intent.item_phrase)

    def test_parses_food_and_meat_units(self):
        parser = KoreanChatClefRuleParser()

        food = parser.parse("음식 10만큼 모아")
        meat = parser.parse("고기 8만큼 모아")

        self.assertEqual(ChatClefIntentType.FOOD, food.intent_type)
        self.assertEqual(10, food.food_units)
        self.assertEqual(ChatClefIntentType.MEAT, meat.intent_type)
        self.assertEqual(8, meat.food_units)

    def test_parses_coordinates_and_preserves_follow_player_case(self):
        parser = KoreanChatClefRuleParser()

        goto = parser.parse("100 64 -30으로 이동해")
        follow = parser.parse("Steve 따라가")

        self.assertEqual(ChatClefIntentType.GOTO, goto.intent_type)
        self.assertEqual((100, 64, -30), (goto.x, goto.y, goto.z))
        self.assertEqual(ChatClefIntentType.FOLLOW, follow.intent_type)
        self.assertEqual("Steve", follow.player_name)

    def test_parses_idle_and_stop(self):
        parser = KoreanChatClefRuleParser()

        self.assertEqual(ChatClefIntentType.IDLE, parser.parse("가만히 있어").intent_type)
        self.assertEqual(ChatClefIntentType.STOP, parser.parse("멈춰").intent_type)

    def test_complete_raw_goto_contract_examples(self):
        parser = KoreanChatClefRuleParser()
        cases = (
            "500 90 -928로 가줘",
            "500,90,-928으로 가줘",
            "500, 90, -928 좌표로 가줘",
            "좌표 500, 90, -928로 이동해줘",
            "x 500 y 90 z -928로 가줘",
            "x=500, y=90, z=-928 좌표로 가줘",
            "엑스 500 와이 90 제트 마이너스 928 좌표로 가줘",
            "500 90 마이너스 928로 가줘",
            "(500, 90, -928)으로 이동해",
        )
        for text in cases:
            with self.subTest(text=text):
                intent = parser.parse(text)
                self.assertEqual(ChatClefIntentType.GOTO, intent.intent_type)
                self.assertEqual((500, 90, -928), (intent.x, intent.y, intent.z))
                self.assertEqual(text, intent.original_text)

    def test_goto_valid_signs_endings_and_java_int_boundaries(self):
        parser = KoreanGotoCoordinateParser()
        cases = (
            (" +500  0 -928로 가주세요! ", (500, 0, -928)),
            ("플러스 500 90 마이너스 928로 가자.", (500, 90, -928)),
            ("X=500, Y=90, Z=-928 좌표로 이동해주세요", (500, 90, -928)),
            ("-2147483648 0 +2147483647로 가", (-2147483648, 0, 2147483647)),
            ("0 0000000000000000000000000 -0로 이동", (0, 0, 0)),
        )
        for text, xyz in cases:
            with self.subTest(text=text):
                result = parser.parse(text)
                self.assertEqual(GotoParseDecision.VALID_XYZ, result.decision)
                self.assertEqual(xyz, result.xyz)

    def test_goto_question_negation_quotation_and_hypothesis_are_noncommands(self):
        parser = KoreanGotoCoordinateParser()
        cases = (
            "500 90 -928로 가지 마",
            "500 90 -928로 가지 말아줘",
            "500 90 -928로 이동하지 마",
            "500 90 -928로 이동해주지 마",
            "500 90 -928로 이동해 주지 말아줘",
            "500 90 -928로 가주지 마",
            "500 90 -928로 가 주지 말아줘",
            "500 90 -928로 안 가",
            "500 90 -928로 가면 어떻게 돼?",
            "500 90 -928로 가나요",
            '"500 90 -928로 가줘"라고 말했어',
            "‘500 90 -928로 가줘’라는 예시",
            "만약 500 90 -928로 가면",
            "500 90 -928로 가줘?",
        )
        for text in cases:
            with self.subTest(text=text):
                result = parser.parse(text)
                self.assertEqual(GotoParseDecision.NONCOMMAND, result.decision)
                self.assertIsNone(result.xyz)

    def test_goto_invalid_coordinates_need_clarification_without_partial_intent(self):
        pure = KoreanGotoCoordinateParser()
        shared = KoreanChatClefRuleParser()
        cases = (
            "500 90로 가줘",
            "500로 가줘",
            "500 90 -928 4로 가줘",
            "500.5 90 -928로 이동해",
            "500 90 -928.5로 가줘",
            "2147483648 90 -928로 가줘",
            "-2147483649 90 -928로 가줘",
            "1,000 90 -928로 가줘",
            "500 90 --928로 가줘",
            "500 90 마이너스 -928로 가줘",
            "x 500 x 90 z -928로 가줘",
            "y 90 x 500 z -928로 가줘",
            "500 90 -928 또는 501 90 -928로 가줘",
            "~ ~ ~로 가줘",
            "^ ^ ^로 이동해",
            "네더 차원 500 90 -928로 가줘",
            "오백 구십 마이너스 구백이십팔로 가줘",
            "500 90 -928로 가고 좀비 공격해",
            "(500, 90, -928으로 이동해",
            "500, 90, -928)으로 이동해",
            "500 (90) -928로 가줘",
            "500 NaN -928로 이동해",
        )
        for text in cases:
            with self.subTest(text=text):
                result = pure.parse(text)
                self.assertEqual(GotoParseDecision.CLARIFY, result.decision)
                self.assertFalse(result.executable)
                intent = shared.parse(text)
                self.assertTrue(GotoGuardMarkerDetector().has_marker(intent))
                self.assertEqual(GotoParseDecision.CLARIFY, GotoGuardIntentDecoder().decode(intent).decision)

    def test_original_control_characters_are_not_stripped_or_repaired(self):
        parser = KoreanGotoCoordinateParser()
        for character in ("\t", "\n", "\r", "\x00", "\x7f", "\u200b", "\u202e"):
            for text in (
                character + "500 90 -928로 가줘",
                "500 90 -928로 가줘" + character,
                "500" + character + "90 -928로 가줘",
            ):
                with self.subTest(text=repr(text)):
                    result = parser.parse(text)
                    self.assertEqual(GotoParseDecision.CLARIFY, result.decision)
                    self.assertEqual("goto_control_character", result.reason_code)

    def test_candidate_detection_preserves_other_commands_and_plain_conversation(self):
        candidate = KoreanGotoCandidateDetector()
        pure = KoreanGotoCoordinateParser()
        shared = KoreanChatClefRuleParser()
        for text in (None, 123, "집에 가줘", "나무 10개 가져와", "돌 10개 캐줘", "Steve 따라가", "오늘은 어떤 날이야"):
            with self.subTest(text=text):
                self.assertFalse(candidate.is_candidate(text))
                self.assertEqual(GotoParseDecision.NOT_CANDIDATE, pure.parse(text).decision)
        self.assertEqual(ChatClefIntentType.GET_ITEM, shared.parse("나무 10개 가져와").intent_type)

    def test_goto_result_is_deeply_immutable_and_rejects_invalid_state(self):
        result = GotoParseResult(GotoParseDecision.VALID_XYZ, xyz=(1, 2, 3))
        with self.assertRaises(FrozenInstanceError):
            result.xyz = (4, 5, 6)
        for xyz in ([1, 2, 3], (True, 2, 3), (1, 2), (1, 2, 2147483648)):
            with self.subTest(xyz=xyz), self.assertRaises((TypeError, ValueError)):
                GotoParseResult(GotoParseDecision.VALID_XYZ, xyz=xyz)
        with self.assertRaises(ValueError):
            GotoParseResult(GotoParseDecision.CLARIFY, xyz=(1, 2, 3))


if __name__ == "__main__":
    unittest.main()
