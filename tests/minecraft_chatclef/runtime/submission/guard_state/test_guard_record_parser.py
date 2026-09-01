#20260901_kpopmodder: Specify strict one-shot guard record parsing contracts.
from __future__ import annotations

import unittest

from .guard_record_parser import (
    parse_live_run_block,
    parse_reconciliation_requirement,
)


_INVOCATION_FINGERPRINT = "a" * 64
_COMMAND_FINGERPRINT = "b" * 64


class GuardRecordParserTests(unittest.TestCase):
    def test_live_run_block_requires_the_exact_persisted_shape(self):
        record = {
            "invocation_fingerprint": _INVOCATION_FINGERPRINT,
            "command_fingerprint": _COMMAND_FINGERPRINT,
            "claimed_at_utc": "2026-09-01T03:16:22.464432+00:00",
        }

        identity = parse_live_run_block(record)

        self.assertEqual(_INVOCATION_FINGERPRINT, identity.invocation_fingerprint)
        self.assertEqual(_COMMAND_FINGERPRINT, identity.command_fingerprint)
        self.assertEqual("", identity.reconciliation_reason)

    def test_reconciliation_record_requires_the_exact_persisted_shape(self):
        record = {
            "invocation_fingerprint": _INVOCATION_FINGERPRINT,
            "command_fingerprint": _COMMAND_FINGERPRINT,
            "reason": "gameplay_observation_incomplete",
            "recorded_at_utc": "2026-09-01T03:16:39.959340+00:00",
        }

        identity = parse_reconciliation_requirement(record)

        self.assertEqual(_INVOCATION_FINGERPRINT, identity.invocation_fingerprint)
        self.assertEqual(_COMMAND_FINGERPRINT, identity.command_fingerprint)
        self.assertEqual(
            "gameplay_observation_incomplete",
            identity.reconciliation_reason,
        )

    def test_live_run_block_rejects_malformed_shapes_and_values(self):
        valid = {
            "invocation_fingerprint": _INVOCATION_FINGERPRINT,
            "command_fingerprint": _COMMAND_FINGERPRINT,
            "claimed_at_utc": "2026-09-01T03:16:22.464432+00:00",
        }
        invalid_records = (
            None,
            [],
            {key: value for key, value in valid.items() if key != "claimed_at_utc"},
            {**valid, "unexpected": True},
            {**valid, "invocation_fingerprint": "a" * 63},
            {**valid, "command_fingerprint": "B" * 64},
            {**valid, "claimed_at_utc": "not-a-timestamp"},
            {**valid, "claimed_at_utc": "2026-09-01T03:16:22"},
        )

        for record in invalid_records:
            with self.subTest(record=record), self.assertRaises(ValueError):
                parse_live_run_block(record)

    def test_reconciliation_record_rejects_malformed_reason(self):
        valid = {
            "invocation_fingerprint": _INVOCATION_FINGERPRINT,
            "command_fingerprint": _COMMAND_FINGERPRINT,
            "reason": "gameplay_observation_incomplete",
            "recorded_at_utc": "2026-09-01T03:16:39.959340+00:00",
        }
        invalid_records = (
            {**valid, "reason": ""},
            {**valid, "reason": "x" * 121},
            {**valid, "reason": 123},
            {**valid, "recorded_at_utc": "2026-09-01T03:16:39+09:00"},
        )

        for record in invalid_records:
            with self.subTest(record=record), self.assertRaises(ValueError):
                parse_reconciliation_requirement(record)


if __name__ == "__main__":
    unittest.main()
