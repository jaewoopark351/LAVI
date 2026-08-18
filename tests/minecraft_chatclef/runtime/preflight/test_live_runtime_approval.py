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

    def test_coercible_or_padded_identity_values_are_rejected(self):
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
        for field, value in (
            ("command", 1),
            ("gradio_url", True),
            ("backend", 1.0),
            ("instance", " LAVI_TEST_Fabric01"),
            ("world", "전용월드 "),
            ("invocation_id", 123),
            ("approval_source", " explicit_user_approval"),
        ):
            with self.subTest(field=field, value=value):
                changed = dict(approval)
                changed[field] = value

                self.assertTrue(validate_approval_record(changed, expected))


if __name__ == "__main__":
    unittest.main()
