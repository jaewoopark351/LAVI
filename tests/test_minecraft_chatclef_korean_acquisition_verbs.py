#20260815_kpopmodder: Cover shared Korean acquisition verb matching for ChatClef routing.
import unittest

from plugins.Minecraft.fabric.chatclef.intent.korean_acquisition_verb_matcher import (
    KoreanAcquisitionVerbMatcher,
)


class MinecraftChatClefKoreanAcquisitionVerbMatcherTests(unittest.TestCase):
    def test_matches_get_and_mining_command_verbs(self):
        matcher = KoreanAcquisitionVerbMatcher()

        cases = [
            "다이아몬드 가져와줘",
            "다이아몬드 구해",
            "철 10개 얻어줘",
            "돌 10개 캐줘",
            "석탄 5개 캐와줘",
            "조약돌 10개 캐오기",
            "레드스톤 5개 채굴해줘",
            "돌 채굴해서 가져와",
            "철갑바 만들어줘",
            "횃불 제작해줘",
        ]

        for text in cases:
            with self.subTest(text=text):
                self.assertTrue(matcher.matches(text))

    def test_does_not_match_broad_korean_ca_substrings(self):
        matcher = KoreanAcquisitionVerbMatcher()

        cases = [
            "캐나다 여행 얘기하자",
            "캐시가 10개 남았어",
            "캐릭터 설정 이야기하자",
        ]

        for text in cases:
            with self.subTest(text=text):
                self.assertFalse(matcher.matches(text))

    def test_strips_acquisition_verbs_without_leaving_request_noise(self):
        matcher = KoreanAcquisitionVerbMatcher()

        cases = {
            "다이아몬드 가져와줘": "다이아몬드",
            "철 10개 캐줘": "철 10개",
            "석탄 5개 캐와줘": "석탄 5개",
            "돌 채굴해서 가져와": "돌",
            "철갑바 만들어줘": "철갑바",
            "횃불 제작해줘": "횃불",
        }

        for text, expected in cases.items():
            with self.subTest(text=text):
                self.assertEqual(expected, matcher.strip(text))


if __name__ == "__main__":
    unittest.main()
