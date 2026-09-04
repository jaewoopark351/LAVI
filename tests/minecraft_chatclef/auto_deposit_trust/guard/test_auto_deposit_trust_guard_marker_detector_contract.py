#20260905_kpopmodder: Lock marker detection independently from guard validation.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.guard import (
    AutoDepositTrustGuardFields,
    AutoDepositTrustGuardMarkerDetector,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import ChatClefIntentDTO


class AutoDepositTrustGuardMarkerDetectorContractTests(unittest.TestCase):
    def test_either_canonical_marker_is_detected_even_when_payload_is_malformed(self):
        detector = AutoDepositTrustGuardMarkerDetector()

        self.assertTrue(
            detector.has_marker(
                ChatClefIntentDTO(source=AutoDepositTrustGuardFields.GUARD_SOURCE)
            )
        )
        self.assertTrue(
            detector.has_marker(
                ChatClefIntentDTO(
                    slots={AutoDepositTrustGuardFields.GUARD_SLOT: False}
                )
            )
        )
        self.assertFalse(detector.has_marker(ChatClefIntentDTO()))


if __name__ == "__main__":
    unittest.main()
