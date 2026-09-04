#20260905_kpopmodder: Lock valid, invalid, and non-guard H5 decode states.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust import (
    AutoDepositTrustGuardDecodeResult,
    AutoDepositTrustGuardFields,
    AutoDepositTrustGuardIntentDecoder,
    AutoDepositTrustGuardIntentEncoder,
    AutoDepositTrustIntentClassification,
    AutoDepositTrustIntentDecision,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import ChatClefIntentDTO


class AutoDepositTrustGuardIntentDecoderContractTests(unittest.TestCase):
    def test_exact_encoded_guard_decodes_as_valid(self):
        classification = AutoDepositTrustIntentClassification(
            AutoDepositTrustIntentDecision.UNSUPPORTED_SIZE
        )
        intent = AutoDepositTrustGuardIntentEncoder().encode("8x8", classification)

        result = AutoDepositTrustGuardIntentDecoder().decode(intent)

        self.assertEqual(AutoDepositTrustGuardDecodeResult.VALID, result.status)
        self.assertEqual(classification, result.classification)
        self.assertEqual(classification.reason_code, result.reason_code)

    def test_contradictory_or_partial_marker_fails_closed(self):
        decoder = AutoDepositTrustGuardIntentDecoder()
        malformed = (
            ChatClefIntentDTO(source=AutoDepositTrustGuardFields.GUARD_SOURCE),
            ChatClefIntentDTO(
                slots={AutoDepositTrustGuardFields.GUARD_SLOT: True}
            ),
            ChatClefIntentDTO(
                source=AutoDepositTrustGuardFields.GUARD_SOURCE,
                slots={
                    AutoDepositTrustGuardFields.GUARD_SLOT: True,
                    AutoDepositTrustGuardFields.DECISION_SLOT: "negated",
                    AutoDepositTrustGuardFields.REASON_SLOT: "wrong",
                    AutoDepositTrustGuardFields.MESSAGE_SLOT: "wrong",
                },
            ),
        )

        for intent in malformed:
            with self.subTest(intent=intent):
                result = decoder.decode(intent)
                self.assertEqual(AutoDepositTrustGuardDecodeResult.INVALID, result.status)
                self.assertEqual(
                    "auto_deposit_trust_area_malformed_guard",
                    result.reason_code,
                )

        self.assertEqual(
            AutoDepositTrustGuardDecodeResult.NOT_GUARD,
            decoder.decode(ChatClefIntentDTO()).status,
        )


if __name__ == "__main__":
    unittest.main()
