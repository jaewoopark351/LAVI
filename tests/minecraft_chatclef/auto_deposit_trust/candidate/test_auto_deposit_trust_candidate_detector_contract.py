#20260905_kpopmodder: Lock the total, coarse, side-effect-free H5 candidate boundary.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.candidate import (
    AutoDepositTrustCandidateDetector,
)


class AutoDepositTrustCandidateDetectorContractTests(unittest.TestCase):
    def test_detects_exact_natural_and_guarded_h5_candidates(self):
        detector = AutoDepositTrustCandidateDetector()

        for text in (
            "auto_deposit_trust area 16x16",
            "@auto_deposit_trust 반경 16x16",
            "자동보관등록 영역 16x16",
            "캐릭터 주변 16x16 범위의 상자를 자동 보관 대상으로 등록해",
            "주변 8x8 상자를 등록해",
            "상자 등록해",
            "주변 16x16 상자를 등록할까?",
            "@auto_deposit_\x00trust area 16x16",
            "자동보\x00관등록 영역 16x16",
            "@auto_deposit_\u200btrust area 16x16",
            "자동보\u200b관등록 영역 16x16",
            "@auto_deposit_#trust area 16x16",
            "자동보\\관등록 영역 16x16",
            "자동보/관등록 영역 16x16",
            "자동보:관등록 영역 16x16",
            "자동보=관등록 영역 16x16",
            "자동보[관]등록 영역 16x16",
            "자동보--관등록 영역 16x16",
            "자동보?관등록 영역 16x16",
            "자동보$관등록 영역 16x16",
            "자동보(관)등록 영역 16x16",
            "@auto_deposit|trust area 16x16",
            "주?변 16x16 상?자를 자동 보?관 대상으로 등록해",
            "자동보™관등록 영역 16x16",
            "자동보℡관등록 영역 16x16",
            "자동보₹관등록 영역 16x16",
            "@auto_™deposit_trust area 16x16",
            "자동보ß관등록 영역 16x16",
            "자동보ſ관등록 영역 16x16",
            "자동보K관등록 영역 16x16",
            "자동보ﬀ관등록 영역 16x16",
        ):
            with self.subTest(text=text):
                self.assertTrue(detector.is_candidate(text))

    def test_unrelated_conversation_is_not_a_candidate(self):
        detector = AutoDepositTrustCandidateDetector()

        for text in (None, "", "주변에 상자가 많네", "다이아몬드 캐 와", "회원 등록해"):
            with self.subTest(text=text):
                self.assertFalse(detector.is_candidate(text))

    def test_boundary_returns_false_when_string_conversion_fails(self):
        class BrokenText:
            def __str__(self):
                raise RuntimeError("broken")

        self.assertFalse(AutoDepositTrustCandidateDetector().is_candidate(BrokenText()))


if __name__ == "__main__":
    unittest.main()
