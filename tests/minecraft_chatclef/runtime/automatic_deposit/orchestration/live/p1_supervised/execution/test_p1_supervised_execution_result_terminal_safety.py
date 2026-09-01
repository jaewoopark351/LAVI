#20260901_kpopmodder: Preserve every common-runner terminal safety fact in the sealed P1 result.
from __future__ import annotations

import unittest
from dataclasses import replace

from .p1_supervised_execution_result import seal_p1_supervised_execution_result


class P1SupervisedExecutionResultTerminalSafetyTests(unittest.TestCase):
    def test_preserves_common_runner_terminal_safety_observations(self):
        result = seal_p1_supervised_execution_result(_safe_common_result())

        self.assertEqual("unknown", result.adapter_command_request_count)
        self.assertIs(True, result.connection_state_verified)
        self.assertEqual("", result.connection_state_error)
        self.assertIs(False, result.observer_timeout)
        self.assertIs(True, result.active_request_clear)
        self.assertEqual("same_snapshot", result.active_clear_observation)
        self.assertIs(True, result.gameplay_effect_observed)
        self.assertIs(True, result.end_to_end_success)

    def test_preserves_observable_adapter_request_count(self):
        common_result = _safe_common_result()
        common_result["observation"]["adapter_command_request_count"] = 2

        result = seal_p1_supervised_execution_result(common_result)

        self.assertEqual(2, result.adapter_command_request_count)

    def test_malformed_terminal_safety_values_are_not_coerced_to_safe_values(self):
        common_result = _safe_common_result()
        observation = common_result["observation"]
        observation.update(
            {
                "adapter_command_request_count": "1",
                "connection_state_verified": "true",
                "connection_state_error": None,
                "observer_timeout": "false",
                "active_request_clear": 1,
                "end_to_end_success": "true",
            }
        )

        result = seal_p1_supervised_execution_result(common_result)

        self.assertIsNone(result.adapter_command_request_count)
        self.assertIsNone(result.connection_state_verified)
        self.assertIsNone(result.connection_state_error)
        self.assertIsNone(result.observer_timeout)
        self.assertIsNone(result.active_request_clear)
        self.assertIsNone(result.end_to_end_success)

    def test_terminal_safety_fields_participate_in_the_integrity_seal(self):
        result = seal_p1_supervised_execution_result(_safe_common_result())

        with self.assertRaises(ValueError):
            replace(result, observer_timeout=True)

    def test_padded_request_identity_is_not_normalized_into_evidence(self):
        common_result = _safe_common_result()
        common_result["observation"]["submitted_request_id"] = (
            " lavi-gui-request-17"
        )

        result = seal_p1_supervised_execution_result(common_result)

        self.assertEqual("", result.submitted_request_id)


def _safe_common_result() -> dict[str, object]:
    return {
        "preflight": {"status": "ok"},
        "observation": {
            "submission_outcome": "accepted",
            "gradio_submit_call_count": 1,
            "adapter_command_request_count": "unknown",
            "automatic_resubmit_count": 0,
            "automatic_rerun_count": 0,
            "submitted_request_id": "lavi-gui-request-17",
            "connection_state_verified": True,
            "connection_state_error": "",
            "terminal_lifecycle_observed": True,
            "terminal_request_id": "lavi-gui-request-17",
            "terminal_status": "completed",
            "active_request_clear": True,
            "active_clear_observation": "same_snapshot",
            "observer_timeout": False,
            "runtime_reported_completion": True,
            "gameplay_observation_complete": True,
            "gameplay_effect_observed": True,
            "expected_gameplay_effect_verified": True,
            "partial_gameplay_effect_observed": False,
            "unexpected_effect_observed": False,
            "prohibited_effect_absence_verified": True,
            "end_to_end_success": True,
            "reconciliation_required": True,
        },
    }


if __name__ == "__main__":
    unittest.main()
