#20260818_kpopmodder: Lock the exact one-shot approval tuple contract.
from __future__ import annotations

import unittest

from .approval_record import parse_approval_record, validate_approval_record
from .preflight_fixture_factory import live_environment_fixture


class LiveRuntimeApprovalTests(unittest.TestCase):
    def test_exact_approval_requires_one_shot_and_rerun_confirmation(self):
        environment = live_environment_fixture()
        approval, error = parse_approval_record(environment["approval_json"])
        self.assertEqual("", error)
        expected = {
            "command": environment["command"],
            "gradio_url": environment["gradio_url"],
            "backend": environment["expected_backend"],
            "instance": environment["expected_instance"],
            "world": environment["expected_world"],
            "invocation_id": environment["invocation_id"],
        }
        self.assertEqual("", validate_approval_record(approval, expected))
        approval["automatic_rerun_disabled"] = False
        self.assertIn(
            "automatic_rerun_disabled",
            validate_approval_record(approval, expected),
        )


if __name__ == "__main__":
    unittest.main()
