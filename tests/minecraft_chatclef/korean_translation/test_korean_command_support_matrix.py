#20260815_kpopmodder: Cover current Korean support and unsupported ChatClef command gaps.
#20260819_kpopmodder: Lock the approved four-command live GET translations.
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
        ("조약돌 1개 캐줘", "get cobblestone 1"),
        ("석탄 1개 캐와줘", "get coal 1"),
        ("철 1개 캐줘", "get iron_ingot 1"),
        ("돌 10개 가져와줘", "get stone 10"),
        ("돌을 10개 캐줘", "get stone 10"),
        ("조약돌 10개 캐줘", "get cobblestone 10"),
        ("레드스톤 5개 구해줘", "get redstone 5"),
        ("석탄 5개 캐와줘", "get coal 5"),
        ("철 10개 캐줘", "get iron_ingot 10"),
        ("철 10개 캐오기", "get iron_ingot 10"),
        ("철 10개 캐와줘", "get iron_ingot 10"),
        ("철갑바 만들어줘", "get iron_chestplate 1"),
        ("철바지 만들어줘", "get iron_leggings 1"),
        ("철 레깅스 만들어줘", "get iron_leggings 1"),
        ("철신발 만들어줘", "get iron_boots 1"),
        ("철모자 만들어줘", "get iron_helmet 1"),
        ("철헬멧 만들어줘", "get iron_helmet 1"),
        ("에메랄드 캐줘", "get emerald 1"),
        ("횃불 만들어줘", "get torch 1"),
        ("참나무 원목 2개 가져와줘", "get oak_log 2"),
        ("oak_log 2개 가져와줘", "get oak_log 2"),
        ("다이아몬드 곡괭이 하나 가져와", "get diamond_pickaxe 1"),
    ],
    "equip": [
        ("철 흉갑 입어줘", "equip iron_chestplate"),
        ("철갑바 장착해줘", "equip iron_chestplate"),
        ("철 레깅스 착용해", "equip iron_leggings"),
    ],
    "deposit": [
        ("다이아몬드 2개 상자에 넣어줘", "deposit diamond 2"),
        ("상자에 다이아몬드 2개 넣어줘", "deposit diamond 2"),
    ],
    "give": [
        ("Steve에게 다이아몬드 3개 줘", "give Steve diamond 3"),
        ("Alex한테 횃불 하나 전달해", "give Alex torch 1"),
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
    "deposit_bare_junk": "잡템 상자에 넣어줘",
    "deposit_missing_quantity": "다이아몬드 상자에 넣어줘",
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
            {
                "deposit",
                "equip",
                "follow",
                "food",
                "get",
                "give",
                "goto",
                "idle",
                "meat",
                "stop",
            },
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

    def test_junk_cleanup_wording_does_not_compile_to_bare_deposit(self):
        service = ChatClefNaturalLanguageService()

        result = service.translate("잡템 상자에 넣어줘")

        self.assertFalse(result.executable)
        self.assertIsNone(result.command)
        self.assertNotEqual(ChatClefIntentStatus.VALIDATED, result.status)


if __name__ == "__main__":
    unittest.main()
