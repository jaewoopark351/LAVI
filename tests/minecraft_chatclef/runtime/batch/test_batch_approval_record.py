#20260818_kpopmodder: Verify batch approval is exact, ordered, and target-bound.
from __future__ import annotations

import unittest

from .batch_approval_record import validate_batch_approval_record
from .live_get_batch_commands import COMMANDS


class BatchApprovalRecordTests(unittest.TestCase):
    def test_exact_four_step_approval_is_accepted(self):
        steps, validation_error = validate_batch_approval_record(
            _approval_fixture(),
            COMMANDS,
            _environment_fixture(),
        )

        self.assertEqual("", validation_error)
        self.assertEqual(list(COMMANDS), [step["command"] for step in steps])
        self.assertEqual(4, len({step["invocation_id"] for step in steps}))

    def test_changed_order_is_rejected_before_execution(self):
        approval = _approval_fixture()
        approval["commands"][0], approval["commands"][1] = (
            approval["commands"][1],
            approval["commands"][0],
        )

        _steps, error = validate_batch_approval_record(
            approval,
            COMMANDS,
            _environment_fixture(),
        )

        self.assertIn("command mismatch", error)

    def test_duplicate_invocation_is_rejected(self):
        approval = _approval_fixture()
        approval["commands"][1]["invocation_id"] = "batch-invocation-1"

        _steps, error = validate_batch_approval_record(
            approval,
            COMMANDS,
            _environment_fixture(),
        )

        self.assertIn("duplicated", error)

    def test_target_mismatch_is_rejected(self):
        approval = _approval_fixture()
        approval["world"] = "personal-world"

        _steps, error = validate_batch_approval_record(
            approval,
            COMMANDS,
            _environment_fixture(),
        )

        self.assertEqual("batch approval field mismatch: world", error)

    def test_external_identity_fields_require_exact_trimmed_strings(self):
        cases = (
            ("approval_source", 1),
            ("approval_source", " operator-console"),
            ("gradio_url", 1),
            ("backend", True),
            ("instance", "LAVI_TEST_Fabric01 "),
            ("world", 1.0),
        )
        for field, value in cases:
            with self.subTest(field=field, value=value):
                approval = _approval_fixture()
                approval[field] = value

                _steps, error = validate_batch_approval_record(
                    approval,
                    COMMANDS,
                    _environment_fixture(),
                )

                self.assertTrue(error)

    def test_command_and_invocation_require_exact_trimmed_strings(self):
        cases = (
            ("command", 123),
            ("command", f" {COMMANDS[0]}"),
            ("invocation_id", 123),
            ("invocation_id", " batch-invocation-1"),
        )
        for field, value in cases:
            with self.subTest(field=field, value=value):
                approval = _approval_fixture()
                approval["commands"][0][field] = value

                _steps, error = validate_batch_approval_record(
                    approval,
                    COMMANDS,
                    _environment_fixture(),
                )

                self.assertTrue(error)


def _environment_fixture() -> dict[str, object]:
    return {
        "gradio_url": "http://127.0.0.1:47860",
        "expected_backend": "fabric_chatclef",
        "expected_instance": "LAVI_TEST_Fabric01",
        "expected_world": "test-world",
    }


def _approval_fixture() -> dict[str, object]:
    return {
        "approval_source": "operator-console",
        "gradio_url": "http://127.0.0.1:47860",
        "backend": "fabric_chatclef",
        "instance": "LAVI_TEST_Fabric01",
        "world": "test-world",
        "one_shot": True,
        "automatic_rerun_disabled": True,
        "commands": [
            {"command": command, "invocation_id": f"batch-invocation-{index}"}
            for index, command in enumerate(COMMANDS, start=1)
        ],
    }


if __name__ == "__main__":
    unittest.main()
