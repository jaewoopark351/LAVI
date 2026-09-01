#20260901_kpopmodder: Reject internally contradictory common-runner terminal evidence before P1 assembly.
from __future__ import annotations

import unittest

from minecraft_chatclef.runtime.automatic_deposit.orchestration.live.p1_supervised.execution.p1_supervised_execution_result import (
    seal_p1_supervised_execution_result,
)

from .p1_supervised_execution_result_contract import (
    p1_supervised_execution_result_error,
)


class P1SupervisedExecutionResultContractTests(unittest.TestCase):
    def test_accepts_one_complete_same_snapshot_common_runner_result(self):
        execution = _execution()

        self.assertEqual("", p1_supervised_execution_result_error(execution))

    def test_rejects_terminal_safety_contradictions_even_when_completion_is_true(self):
        cases = (
            (
                {"adapter_command_request_count": 2},
                "P1_SUPERVISED_ADAPTER_REQUEST_COUNT_VIOLATION",
            ),
            (
                {"connection_state_verified": False},
                "P1_SUPERVISED_RUNTIME_CONNECTION_NOT_VERIFIED",
            ),
            (
                {"connection_state_error": "runtime status read failed"},
                "P1_SUPERVISED_RUNTIME_CONNECTION_ERROR",
            ),
            (
                {"observer_timeout": True},
                "P1_SUPERVISED_OBSERVER_TIMEOUT",
            ),
            (
                {"active_request_clear": False},
                "P1_SUPERVISED_ACTIVE_REQUEST_NOT_CLEAR",
            ),
            (
                {"active_clear_observation": "not_observed"},
                "P1_SUPERVISED_ACTIVE_CLEAR_NOT_SAME_SNAPSHOT",
            ),
        )
        for overrides, expected_error in cases:
            with self.subTest(expected_error=expected_error):
                self.assertEqual(
                    expected_error,
                    p1_supervised_execution_result_error(
                        _execution(**overrides)
                    ),
                )

    def test_rejects_missing_or_malformed_terminal_safety_observations(self):
        cases = (
            (
                {"adapter_command_request_count": "1"},
                "P1_SUPERVISED_ADAPTER_REQUEST_COUNT_VIOLATION",
            ),
            (
                {"connection_state_verified": None},
                "P1_SUPERVISED_RUNTIME_CONNECTION_NOT_VERIFIED",
            ),
            (
                {"connection_state_error": None},
                "P1_SUPERVISED_RUNTIME_CONNECTION_ERROR",
            ),
            (
                {"observer_timeout": None},
                "P1_SUPERVISED_OBSERVER_TIMEOUT",
            ),
            (
                {"active_request_clear": None},
                "P1_SUPERVISED_ACTIVE_REQUEST_NOT_CLEAR",
            ),
            (
                {"terminal_status": " completed "},
                "P1_SUPERVISED_TERMINAL_STATUS_NOT_COMPLETED",
            ),
        )
        for overrides, expected_error in cases:
            with self.subTest(expected_error=expected_error):
                self.assertEqual(
                    expected_error,
                    p1_supervised_execution_result_error(
                        _execution(**overrides)
                    ),
                )

    def test_terminal_receipt_does_not_require_untyped_gameplay_booleans(self):
        execution = _execution(
            gameplay_observation_complete=None,
            gameplay_effect_observed=None,
            expected_gameplay_effect_verified=None,
            partial_gameplay_effect_observed=None,
            unexpected_effect_observed=None,
            prohibited_effect_absence_verified=None,
            end_to_end_success=None,
        )

        self.assertEqual("", p1_supervised_execution_result_error(execution))


def _execution(**overrides):
    observation = {
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
    }
    observation.update(overrides)
    return seal_p1_supervised_execution_result(
        {"preflight": {"status": "ok"}, "observation": observation}
    )


if __name__ == "__main__":
    unittest.main()
