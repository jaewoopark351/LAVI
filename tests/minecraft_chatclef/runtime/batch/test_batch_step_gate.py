#20260818_kpopmodder: Verify batch advancement requires the complete terminal gate.
from __future__ import annotations

import unittest

from .batch_fixture_factory import completed_command_result_fixture
from .batch_step_gate import batch_step_gate_error


class BatchStepGateTests(unittest.TestCase):
    def test_completed_same_snapshot_result_may_reach_checkpoint(self):
        self.assertEqual(
            "",
            batch_step_gate_error(completed_command_result_fixture()),
        )

    def test_matching_failed_terminal_may_reach_read_only_checkpoint(self):
        result = completed_command_result_fixture()
        result["observation"]["terminal_status"] = "failed"
        result["observation"]["runtime_reported_completion"] = False

        self.assertEqual(
            "",
            batch_step_gate_error(result),
        )

    def test_automatic_resubmit_is_never_allowed_to_advance(self):
        result = completed_command_result_fixture()
        result["observation"]["automatic_resubmit_count"] = 1

        self.assertEqual(
            "automatic_resubmit_violation",
            batch_step_gate_error(result),
        )

    def test_automatic_rerun_is_never_allowed_to_reach_checkpoint(self):
        result = completed_command_result_fixture()
        result["observation"]["automatic_rerun_count"] = 1

        self.assertEqual(
            "automatic_rerun_violation",
            batch_step_gate_error(result),
        )

    def test_observable_duplicate_adapter_request_is_rejected(self):
        result = completed_command_result_fixture()
        result["observation"]["adapter_command_request_count"] = 2

        self.assertEqual(
            "adapter_command_request_count_violation",
            batch_step_gate_error(result),
        )

    def test_missing_or_malformed_request_ownership_is_rejected(self):
        cases = (
            ("submitted_request_id", "absent", "submitted_request_id_invalid"),
            ("submitted_request_id", 123, "submitted_request_id_invalid"),
            ("submitted_request_id", " request-1", "submitted_request_id_invalid"),
            ("terminal_request_id", "absent", "terminal_request_id_invalid"),
            ("terminal_request_id", 123, "terminal_request_id_invalid"),
            ("terminal_request_id", "request-1 ", "terminal_request_id_invalid"),
        )
        for field, value, expected_error in cases:
            with self.subTest(field=field, value=value):
                result = completed_command_result_fixture()
                result["observation"][field] = value

                self.assertEqual(expected_error, batch_step_gate_error(result))

    def test_terminal_request_must_match_submitted_request(self):
        result = completed_command_result_fixture()
        result["observation"]["terminal_request_id"] = "request-2"

        self.assertEqual(
            "terminal_request_id_mismatch",
            batch_step_gate_error(result),
        )

    def test_complete_process_identity_is_required(self):
        result = completed_command_result_fixture()
        result["preflight"]["observed"].pop("process_identity_fingerprint")

        self.assertEqual(
            "process_identity_not_verified",
            batch_step_gate_error(result),
        )

    def test_timeout_flag_must_be_exact_false(self):
        result = completed_command_result_fixture()
        result["observation"]["observer_timeout"] = "false"

        self.assertEqual("observer_timeout", batch_step_gate_error(result))

    def test_terminal_status_must_be_an_exact_known_terminal_value(self):
        for value in ("accepted", ["completed"], True):
            with self.subTest(value=value):
                result = completed_command_result_fixture()
                result["observation"]["terminal_status"] = value

                self.assertEqual(
                    "terminal_status_invalid",
                    batch_step_gate_error(result),
                )


if __name__ == "__main__":
    unittest.main()
