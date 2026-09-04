#20260905_kpopmodder: Lock the allowlisted H5 size and spacing normalization.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.normalization import (
    AutoDepositTrustInputNormalizer,
)


class AutoDepositTrustInputNormalizerContractTests(unittest.TestCase):
    def test_allowlisted_size_forms_have_one_fixed_representation(self):
        normalizer = AutoDepositTrustInputNormalizer()

        for size in ("16x16", "16 X 16", "16×16", "16 곱하기 16"):
            with self.subTest(size=size):
                self.assertEqual(
                    "주변 16x16 상자 등록해!",
                    normalizer.normalize(f"주변 {size} 상자 등록해!"),
                )

    def test_question_evidence_and_unsupported_sizes_are_not_erased(self):
        normalizer = AutoDepositTrustInputNormalizer()

        self.assertEqual("주변 8x8 상자 등록해?", normalizer.normalize("주변 8x8 상자 등록해?"))
        self.assertEqual("주변 16x8 상자 등록해", normalizer.normalize("주변 16x8 상자 등록해"))

    def test_command_keywords_are_not_case_folded(self):
        normalizer = AutoDepositTrustInputNormalizer()

        cases = {
            "AUTO_DEPOSIT_TRUST area 16x16": "AUTO_DEPOSIT_TRUST area 16x16",
            "Auto_Deposit_Trust AREA 16X16": "Auto_Deposit_Trust AREA 16x16",
            "auto_deposit_trust AREA 16x16": "auto_deposit_trust AREA 16x16",
        }
        for text, expected in cases.items():
            with self.subTest(text=text):
                self.assertEqual(expected, normalizer.normalize(text))

    def test_compatibility_characters_and_inserted_punctuation_are_not_folded(self):
        normalizer = AutoDepositTrustInputNormalizer()

        for text in (
            "ａｕｔｏ＿ｄｅｐｏｓｉｔ＿ｔｒｕｓｔ area 16x16",
            "auto_deposit_trust area ⑯x⑯",
            "auto_deposit_trust，area 16x16",
            "auto_deposit_trust area １６x１６",
            "auto_deposit_trust\u00a0area 16x16",
            "auto_deposit_trust\u2009area 16x16",
            "auto_deposit_trust\u3000area 16x16",
            "auto_deposit_trust area 16\u00a0x\u00a016",
            "자동보관등록 영역 16\u2009곱하기\u300016",
            "\u00a0auto_deposit_trust area 16x16\u00a0",
            "\u3000자동보관등록 영역 16x16\u3000",
        ):
            with self.subTest(text=text):
                self.assertEqual(text, normalizer.normalize(text))


if __name__ == "__main__":
    unittest.main()
