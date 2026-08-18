#20260818_kpopmodder: Verify target delta success, partial, and prohibited outcomes.
from __future__ import annotations

import unittest

from .get_item_delta_oracle import get_item_delta_checkpoint


class GetItemDeltaOracleTests(unittest.TestCase):
    def test_requested_or_larger_delta_is_expected_success(self):
        checkpoint = get_item_delta_checkpoint(
            before_count=5,
            after_count=7,
            requested_count=1,
        )

        self.assertIs(True, checkpoint["expected_gameplay_effect_verified"])
        self.assertIs(False, checkpoint["partial_gameplay_effect_observed"])
        self.assertIs(True, checkpoint["prohibited_effect_absence_verified"])

    def test_target_item_loss_is_a_prohibited_unexpected_effect(self):
        checkpoint = get_item_delta_checkpoint(
            before_count=5,
            after_count=4,
            requested_count=1,
        )

        self.assertIs(False, checkpoint["expected_gameplay_effect_verified"])
        self.assertIs(True, checkpoint["unexpected_effect_observed"])
        self.assertIs(False, checkpoint["prohibited_effect_absence_verified"])


if __name__ == "__main__":
    unittest.main()
