#20260803_kpopmodder: Cover Korean natural language translation to ChatClef DSL.
#20260913_kpopmodder: Verify shared GOTO grammar and no LLM recovery of guarded input.
import unittest
from unittest.mock import Mock

from plugins.Minecraft.fabric.chatclef.intent import (
    ChatClefIntentStatus,
    ChatClefNaturalLanguageService,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import ChatClefIntentDTO
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import ChatClefIntentType
from plugins.Minecraft.fabric.chatclef.intent.composite_chatclef_intent_extractor import CompositeChatClefIntentExtractor
from plugins.Minecraft.fabric.chatclef.intent.navigation.goto.guard.goto_guard_fields import (
    DECISION_SLOT,
    GUARD_SLOT,
    GUARD_SOURCE,
    REASON_SLOT,
)


class MinecraftChatClefNaturalLanguageServiceTests(unittest.TestCase):
    def test_translates_supported_korean_commands(self):
        service = ChatClefNaturalLanguageService()
        cases = {
            "다이아몬드 곡괭이 하나 가져와": "get diamond_pickaxe 1",
            "다이아몬드 도끼 하나 가져와": "get diamond_axe 1",
            "다이아 도끼 2개 구해": "get diamond_axe 2",
            "철 삽 하나 만들어": "get iron_shovel 1",
            "네더라이트 검 가져와": "get netherite_sword 1",
            "금 도끼 하나 가져와": "get golden_axe 1",
            "금괴 8개 구해": "get gold_ingot 8",
            "금 주괴 8개 구해": "get gold_ingot 8",
            "철괴 4개 가져와": "get iron_ingot 4",
            "철 10개 캐줘": "get iron_ingot 10",
            "금 3개 캐줘": "get gold_ingot 3",
            "다이아몬드 가져와줘": "get diamond 1",
            "다이아몬드 캐줘": "get diamond 1",
            "다이아몬드를 가져와줘": "get diamond 1",
            "돌 10개 가져와줘": "get stone 10",
            "돌을 10개 캐줘": "get stone 10",
            "조약돌 10개 캐줘": "get cobblestone 10",
            "레드스톤 5개 구해줘": "get redstone 5",
            "석탄 5개 캐와줘": "get coal 5",
            "스테이크 8개 가져와": "get cooked_beef 8",
            "구운 소고기 8개 가져와": "get cooked_beef 8",
            "나무 16개 구해": "get log 16",
            "나무 도끼 하나 가져와": "get wooden_axe 1",
            "음식 10만큼 모아": "food 10",
            "고기 8만큼 모아": "meat 8",
            "100 64 -30으로 이동해": "goto 100 64 -30",
            "Steve 따라가": "follow Steve",
            "가만히 있어": "idle",
            "멈춰": "stop",
        }

        for text, expected_command in cases.items():
            with self.subTest(text=text):
                result = service.translate(text)
                self.assertTrue(result.executable)
                self.assertEqual(ChatClefIntentStatus.VALIDATED, result.status)
                self.assertEqual(expected_command, result.command)

    def test_rejects_ambiguous_unsupported_unknown_and_invalid_inputs(self):
        service = ChatClefNaturalLanguageService()
        cases = {
            "다이아몬드 갑옷 가져와": ChatClefIntentStatus.AMBIGUOUS,
            "구리 검 하나 만들어": ChatClefIntentStatus.UNSUPPORTED,
            "레이저 총 가져와": ChatClefIntentStatus.UNKNOWN,
            "도구 하나 가져와": ChatClefIntentStatus.AMBIGUOUS,
            "아무거나 가져와": ChatClefIntentStatus.UNKNOWN,
            "다이아 도끼 0개": ChatClefIntentStatus.INVALID,
            "다이아 도끼 -1개": ChatClefIntentStatus.INVALID,
            "다이아 도끼 1; stop": ChatClefIntentStatus.INVALID,
            "Steve;stop 따라가": ChatClefIntentStatus.INVALID,
        }

        for text, expected_status in cases.items():
            with self.subTest(text=text):
                result = service.translate(text)
                self.assertFalse(result.executable)
                self.assertIsNone(result.command)
                self.assertEqual(expected_status, result.status)

    def test_item_action_intent_table_is_immutable(self):
        service = ChatClefNaturalLanguageService()

        with self.assertRaises(AttributeError):
            service._ITEM_ACTION_INTENTS.add("changed")

        self.assertEqual(
            "get redstone 5",
            service.translate("레드스톤 5개 구해줘").command,
        )

    def test_translates_all_nine_raw_coordinate_forms_using_existing_string_api(self):
        service = ChatClefNaturalLanguageService()
        for text in (
            "500 90 -928로 가줘",
            "500,90,-928으로 가줘",
            "500, 90, -928 좌표로 가줘",
            "좌표 500, 90, -928로 이동해줘",
            "x 500 y 90 z -928로 가줘",
            "x=500, y=90, z=-928 좌표로 가줘",
            "엑스 500 와이 90 제트 마이너스 928 좌표로 가줘",
            "500 90 마이너스 928로 가줘",
            "(500, 90, -928)으로 이동해",
        ):
            with self.subTest(text=text):
                result = service.translate(text)
                self.assertTrue(result.executable)
                self.assertEqual("goto 500 90 -928", result.command)
                self.assertEqual(text, result.intent.original_text)

    def test_guarded_coordinate_inputs_never_reach_optional_llm(self):
        for enabled in (False, True):
            llm = Mock()
            llm.extract.return_value = ChatClefIntentDTO(
                intent_type=ChatClefIntentType.GOTO, x=500, y=90, z=-928
            )
            service = ChatClefNaturalLanguageService(
                extractor=CompositeChatClefIntentExtractor(llm_extractor=llm if enabled else None)
            )
            for text in (
                "500.5 90 -928로 이동해",
                "500 90로 가줘",
                "500 90 -928로 가지 마",
                "500 90 -928로 가면 어떻게 돼?",
                "오백 구십 마이너스 구백이십팔로 가줘",
                "500 90 -928로 가고 좀비 공격해",
                '"500 90 -928로 가줘"라고 말했어',
            ):
                with self.subTest(enabled=enabled, text=text):
                    result = service.translate(text)
                    self.assertFalse(result.executable)
                    self.assertEqual(ChatClefIntentStatus.INVALID, result.status)
                    self.assertIsNone(result.command)
            llm.extract.assert_not_called()

    def test_malformed_goto_markers_remain_nonexecutable_with_optional_llm(self):
        malformed = (
            ChatClefIntentDTO(source=GUARD_SOURCE),
            ChatClefIntentDTO(slots={GUARD_SLOT: False}),
            ChatClefIntentDTO(source=GUARD_SOURCE, slots={GUARD_SLOT: True, DECISION_SLOT: "valid_xyz", REASON_SLOT: "goto_noncommand"}),
            ChatClefIntentDTO(source=GUARD_SOURCE, slots={GUARD_SLOT: True, DECISION_SLOT: "clarify", REASON_SLOT: ["goto_invalid_coordinates"]}),
            ChatClefIntentDTO(source=GUARD_SOURCE, x=500, slots={GUARD_SLOT: True, DECISION_SLOT: "clarify", REASON_SLOT: "goto_invalid_coordinates"}),
            ChatClefIntentDTO(intent_type=ChatClefIntentType.GOTO, source=GUARD_SOURCE, x=500, y=90, z=-928),
        )
        for enabled in (False, True):
            for intent in malformed:
                with self.subTest(enabled=enabled, intent=intent):
                    rule = Mock()
                    rule.parse.return_value = intent
                    llm = Mock()
                    extractor = CompositeChatClefIntentExtractor(rule_parser=rule, llm_extractor=llm if enabled else None)
                    result = ChatClefNaturalLanguageService(extractor=extractor).translate("500 90 -928로 가줘")
                    self.assertFalse(result.executable)
                    self.assertEqual(ChatClefIntentStatus.INVALID, result.status)
                    self.assertEqual("invalid_goto_guard", result.reason_code)
                    llm.extract.assert_not_called()

    def test_negated_movement_favor_endings_keep_noncommand_marker(self):
        for enabled in (False, True):
            llm = Mock()
            service = ChatClefNaturalLanguageService(
                extractor=CompositeChatClefIntentExtractor(
                    llm_extractor=llm if enabled else None
                )
            )
            for text in (
                "500 90 -928로 이동해주지 마",
                "500 90 -928로 이동해 주지 말아줘",
                "500 90 -928로 가주지 마",
                "500 90 -928로 가 주지 말아줘",
            ):
                with self.subTest(enabled=enabled, text=text):
                    result = service.translate(text)
                    self.assertFalse(result.executable)
                    self.assertEqual("goto_noncommand", result.reason_code)
                    self.assertEqual("noncommand", result.data["goto_decision"])
            llm.extract.assert_not_called()


if __name__ == "__main__":
    unittest.main()
