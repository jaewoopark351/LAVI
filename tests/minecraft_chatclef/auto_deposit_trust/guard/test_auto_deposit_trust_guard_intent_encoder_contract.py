#20260905_kpopmodder: Lock canonical UNKNOWN encoding for guarded H5 decisions.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust import (
    AutoDepositTrustGuardFields,
    AutoDepositTrustGuardIntentEncoder,
    AutoDepositTrustIntentClassification,
    AutoDepositTrustIntentDecision,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import ChatClefIntentType


class AutoDepositTrustGuardIntentEncoderContractTests(unittest.TestCase):
    def test_guarded_decision_encodes_exact_source_and_slots(self):
        classification = AutoDepositTrustIntentClassification(
            AutoDepositTrustIntentDecision.NEGATED
        )

        intent = AutoDepositTrustGuardIntentEncoder().encode("등록하지 마", classification)

        self.assertEqual(ChatClefIntentType.UNKNOWN, intent.intent_type)
        self.assertEqual(AutoDepositTrustGuardFields.GUARD_SOURCE, intent.source)
        self.assertEqual(
            {
                AutoDepositTrustGuardFields.GUARD_SLOT: True,
                AutoDepositTrustGuardFields.DECISION_SLOT: "negated",
                AutoDepositTrustGuardFields.REASON_SLOT: classification.reason_code,
                AutoDepositTrustGuardFields.MESSAGE_SLOT: classification.message,
            },
            intent.slots,
        )

    def test_executable_and_no_match_decisions_cannot_be_guard_encoded(self):
        encoder = AutoDepositTrustGuardIntentEncoder()

        for decision in (
            AutoDepositTrustIntentDecision.NO_MATCH,
            AutoDepositTrustIntentDecision.AUTO_DEPOSIT_TRUST_AREA,
        ):
            with self.subTest(decision=decision):
                with self.assertRaisesRegex(ValueError, "requires_guarded_decision"):
                    encoder.encode("text", AutoDepositTrustIntentClassification(decision))


if __name__ == "__main__":
    unittest.main()
