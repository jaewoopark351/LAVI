#20260818_kpopmodder: Verify reconciliation markers stay repository-local and exclusive.
from __future__ import annotations

import unittest
from pathlib import Path
from unittest.mock import Mock

from ..preflight.command_fingerprint import command_fingerprint
from .reconciliation_requirement_recorder import (
    ReconciliationRequirementRecorder,
)


class ReconciliationRequirementRecorderTests(unittest.TestCase):
    def test_record_uses_fingerprint_and_bounded_reason(self):
        repository_root = Path(__file__).resolve().parents[4]
        recorder = ReconciliationRequirementRecorder(
            str(repository_root),
            state_directory=repository_root / "tests" / "tmp_recorder_fixture",
        )
        recorder._write_exclusive = Mock(return_value=True)

        expected_command = command_fingerprint("private-command")
        result = recorder.record(
            "private-invocation",
            expected_command,
            "x" * 200,
        )

        self.assertTrue(result["ok"])
        payload = recorder._write_exclusive.call_args.args[1]
        self.assertNotIn("private-invocation", str(payload))
        self.assertNotIn("private-command", str(payload))
        self.assertEqual(expected_command, payload["command_fingerprint"])
        self.assertEqual(120, len(payload["reason"]))

    def test_state_directory_outside_repository_is_rejected(self):
        repository_root = Path(__file__).resolve().parents[4]
        with self.assertRaises(ValueError):
            ReconciliationRequirementRecorder(
                str(repository_root),
                state_directory=repository_root.parent,
            )


if __name__ == "__main__":
    unittest.main()
