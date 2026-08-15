#20260803_kpopmodder: Cover Korean natural language translation to ChatClef DSL.
import unittest

from plugins.Minecraft.fabric.chatclef.intent import (
    ChatClefIntentStatus,
    ChatClefNaturalLanguageService,
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


if __name__ == "__main__":
    unittest.main()
