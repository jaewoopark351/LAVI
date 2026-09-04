#20260905_kpopmodder: Prove raw controls are rejected before H5 adaptation or normalization.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.safety import (
    AutoDepositTrustRawInputSafety,
)


class AutoDepositTrustRawInputSafetyContractTests(unittest.TestCase):
    def test_at_and_ascii_space_are_safe_but_controls_and_unicode_spaces_are_not(self):
        safety = AutoDepositTrustRawInputSafety()
        self.assertTrue(safety.is_safe("@auto_deposit_trust area 16x16"))
        for text in (
            "@auto_deposit_trust area 16x16\n",
            "자동보관등록\r영역 16x16",
            "자동보관등록\t영역 16x16",
            "자동보관등록\x00영역 16x16",
            "자동보관등록\x7f영역 16x16",
            "자동보관등록\u200b영역 16x16",
            "자동보관등록\u202e영역 16x16",
            "자동보관등록\u2028영역 16x16",
            "자동보관등록\u2029영역 16x16",
            "auto_deposit_trust\u00a0area 16x16",
            "주변\u200916x16 상자를 자동 보관 대상으로 등록해",
            "주변\u300016x16 상자를 자동 보관 대상으로 등록해",
            "auto_deposit_trust area 16\u202fx\u205f16",
        ):
            with self.subTest(text=repr(text)):
                self.assertFalse(safety.is_safe(text))


if __name__ == "__main__":
    unittest.main()
