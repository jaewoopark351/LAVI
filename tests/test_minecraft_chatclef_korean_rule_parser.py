#20260803_kpopmodder: Cover deterministic Korean rule parsing before item resolution.
import unittest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_chatclef_rule_parser import (
    KoreanChatClefRuleParser,
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


if __name__ == "__main__":
    unittest.main()
