#20260818_kpopmodder: Verify persistent live-run claims block every automatic rerun.
from __future__ import annotations

import unittest
from pathlib import Path
from unittest.mock import Mock

from .one_shot_run_guard import OneShotRunGuard


class OneShotRunGuardTests(unittest.TestCase):
    def test_first_claim_passes_and_next_invocation_is_blocked(self):
        repository_root = Path(__file__).resolve().parents[4]
        guard = OneShotRunGuard(
            str(repository_root),
            state_directory=(
                repository_root
                / "tests"
                / "tmp_minecraft_chatclef_guard_unit_fixture"
            ),
        )
        guard._write_exclusive = Mock(side_effect=(True, False))

        first = guard.claim("invocation-1", "command-fingerprint")
        second = guard.claim("invocation-2", "other-command-fingerprint")

        self.assertTrue(first["ok"])
        self.assertFalse(second["ok"])
        self.assertIn("reconciled", second["reason"])
        self.assertEqual(2, guard._write_exclusive.call_count)
        first_payload = guard._write_exclusive.call_args_list[0].args[1]
        self.assertNotIn("invocation-1", str(first_payload))
        self.assertNotIn("돌 1개 가져와줘", str(first_payload))
        self.assertEqual("command-fingerprint", first_payload["command_fingerprint"])

    def test_state_directory_outside_repository_is_rejected(self):
        repository_root = Path(__file__).resolve().parents[4]
        with self.assertRaises(ValueError):
            OneShotRunGuard(
                str(repository_root),
                state_directory=repository_root.parent,
            )

    def test_consumed_invocation_is_blocked_before_claim_write(self):
        repository_root = Path(__file__).resolve().parents[4]
        guard = OneShotRunGuard(
            str(repository_root),
            state_directory=(repository_root / "tests" / "tmp_guard_fixture"),
            invocation_ledger=_ConsumedInvocationLedger(),
        )
        guard._write_exclusive = Mock(return_value=True)

        result = guard.claim("already-used", "command-fingerprint")

        self.assertFalse(result["ok"])
        self.assertIn("already completed", result["reason"])
        guard._write_exclusive.assert_not_called()


class _ConsumedInvocationLedger:
    def status(self, _invocation_id):
        return {"ok": True, "consumed": True, "reason": "consumed"}


if __name__ == "__main__":
    unittest.main()
