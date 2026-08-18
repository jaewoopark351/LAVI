#20260815_kpopmodder: Cover current Korean support and unsupported ChatClef command gaps.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent import (
    ChatClefIntentStatus,
    ChatClefNaturalLanguageService,
)


SUPPORTED_KOREAN_TRANSLATION_CASES = {
    "get": [
        ("다이아몬드 가져와줘", "get diamond 1"),
        ("다이아몬드를 가져와줘", "get diamond 1"),
        ("다이아몬드 캐줘", "get diamond 1"),
        ("돌 10개 가져와줘", "get stone 10"),
        ("돌을 10개 캐줘", "get stone 10"),
        ("조약돌 10개 캐줘", "get cobblestone 10"),
        ("레드스톤 5개 구해줘", "get redstone 5"),
        ("석탄 5개 캐와줘", "get coal 5"),
        ("철 10개 캐줘", "get iron_ingot 10"),
        ("다이아몬드 곡괭이 하나 가져와", "get diamond_pickaxe 1"),
    ],
    "food": [
        ("음식 10만큼 모아", "food 10"),
    ],
    "meat": [
        ("고기 8만큼 모아", "meat 8"),
    ],
    "goto": [
        ("100 64 -30으로 이동해", "goto 100 64 -30"),
    ],
    "follow": [
        ("Steve 따라가", "follow Steve"),
    ],
    "idle": [
        ("가만히 있어", "idle"),
    ],
    "stop": [
        ("멈춰", "stop"),
    ],
}

UNSUPPORTED_OR_PLANNED_KOREAN_CASES = {
    "equip": "철 갑옷 입어줘",
    "deposit": "다이아몬드 2개 상자에 넣어줘",
    "give": "Steve에게 다이아몬드 3개 줘",
    "attack": "좀비 2마리 공격해",
    "gamma": "감마 1로 설정해",
    "hero": "주변 몬스터 처리해",
    "locate_structure": "네더 요새 찾아줘",
    "overlay": "오버레이 켜줘",
    "reload_settings": "설정 다시 불러와",
    "resetmemory": "기억 초기화해",
    "scan": "다이아몬드 원석 스캔해",
    "chatclef": "챗클레프 꺼줘",
    "gamer": "엔더드래곤 잡아줘",
}


class KoreanCommandSupportMatrixTests(unittest.TestCase):
    def test_supported_korean_commands_translate_to_prefixless_chatclef_dsl(self):
        service = ChatClefNaturalLanguageService()

        for command_name, cases in SUPPORTED_KOREAN_TRANSLATION_CASES.items():
            for text, expected_command in cases:
                with self.subTest(command_name=command_name, text=text):
                    result = service.translate(text)

                    self.assertTrue(result.executable)
                    self.assertEqual(ChatClefIntentStatus.VALIDATED, result.status)
                    self.assertEqual(expected_command, result.command)
                    self.assertFalse(str(result.command).startswith("@"))

    def test_current_supported_korean_command_set_is_explicit(self):
        self.assertEqual(
            {"get", "food", "meat", "goto", "follow", "idle", "stop"},
            set(SUPPORTED_KOREAN_TRANSLATION_CASES),
        )

    def test_planned_or_java_only_commands_do_not_claim_korean_support_yet(self):
        service = ChatClefNaturalLanguageService()

        for command_name, text in UNSUPPORTED_OR_PLANNED_KOREAN_CASES.items():
            with self.subTest(command_name=command_name, text=text):
                result = service.translate(text)

                self.assertFalse(result.executable)
                self.assertIsNone(result.command)
                self.assertNotEqual(ChatClefIntentStatus.VALIDATED, result.status)

    def test_item_action_gaps_are_visible_before_full_korean_command_coverage(self):
        item_action_commands = {"equip", "deposit", "give"}

        self.assertTrue(
            item_action_commands.issubset(UNSUPPORTED_OR_PLANNED_KOREAN_CASES)
        )
        self.assertTrue(item_action_commands.isdisjoint(SUPPORTED_KOREAN_TRANSLATION_CASES))


if __name__ == "__main__":
    unittest.main()
