#20260905_kpopmodder: Lock the two exact trusted @ forms and raw-text preservation.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.exact_input import (
    AutoDepositTrustExactInputAdapter,
)


class AutoDepositTrustExactInputAdapterContractTests(unittest.TestCase):
    def test_only_two_whole_string_at_forms_become_canonical_prefixless_input(self):
        adapter = AutoDepositTrustExactInputAdapter()
        for text in (
            "@auto_deposit_trust area 16x16",
            "@auto_deposit_trust 반경 16x16",
            " @auto_deposit_trust area 16x16 ",
            "  @auto_deposit_trust 반경 16x16  ",
        ):
            with self.subTest(text=text):
                result = adapter.adapt(text)
                self.assertEqual(text, result.original_text)
                self.assertEqual(
                    "auto_deposit_trust area 16x16",
                    result.translation_input_text,
                )

        for text in (
            "@자동보관등록 영역 16x16",
            "@auto_deposit_trust  area 16x16",
        ):
            with self.subTest(rejected=text):
                rejected_lane = adapter.adapt(text)
                self.assertTrue(rejected_lane.translation_input_text.startswith("@"))


if __name__ == "__main__":
    unittest.main()
