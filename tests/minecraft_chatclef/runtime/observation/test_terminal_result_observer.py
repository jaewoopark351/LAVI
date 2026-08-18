#20260818_kpopmodder: Verify one submitted request is observed without replay or cancellation.
from __future__ import annotations

import unittest

from .live_run_observation import new_live_run_observation
from .terminal_result_observer import observe_terminal_result


class TerminalResultObserverTests(unittest.TestCase):
    def test_stale_result_is_ignored_until_matching_same_snapshot_clear(self):
        gateway = _SequenceStatusGateway(
            [
                _status("old-request", "completed", active_request_id="request-1"),
                _status("request-1", "completed", active_request_id=None),
            ]
        )
        observation = _accepted_observation("request-1")

        result = observe_terminal_result(
            gateway,
            observation,
            timeout_sec=10,
            poll_sec=0,
            sleeper=lambda _seconds: None,
        )

        self.assertIs(True, result["terminal_lifecycle_observed"])
        self.assertEqual("completed", result["terminal_status"])
        self.assertEqual("same_snapshot", result["active_clear_observation"])
        self.assertIs(True, result["runtime_reported_completion"])
        self.assertEqual(2, gateway.read_calls)

    def test_runtime_unknown_is_terminal_value_and_requires_reconciliation(self):
        gateway = _SequenceStatusGateway(
            [_status("request-1", "unknown", active_request_id=None)]
        )
        result = observe_terminal_result(
            gateway,
            _accepted_observation("request-1"),
            timeout_sec=10,
            poll_sec=0,
            sleeper=lambda _seconds: None,
        )
        self.assertEqual("unknown", result["terminal_status"])
        self.assertIs(True, result["terminal_lifecycle_observed"])
        self.assertIs(False, result["runtime_reported_completion"])
        self.assertTrue(result["reconciliation_required"])

    def test_blank_present_result_reason_does_not_pass_lifecycle(self):
        gateway = _SequenceStatusGateway(
            [_status("request-1", "completed", active_request_id=None, result_reason="")]
        )
        result = observe_terminal_result(
            gateway,
            _accepted_observation("request-1"),
            timeout_sec=10,
            poll_sec=0,
            sleeper=lambda _seconds: None,
        )
        self.assertIs(False, result["terminal_lifecycle_observed"])
        self.assertIs(False, result["runtime_reported_completion"])
        self.assertTrue(result["reconciliation_required"])

    def test_non_string_result_reason_does_not_pass_lifecycle(self):
        result = observe_terminal_result(
            _SequenceStatusGateway(
                [
                    _status(
                        "request-1",
                        "completed",
                        active_request_id=None,
                        result_reason=1,
                    )
                ]
            ),
            _accepted_observation("request-1"),
            timeout_sec=10,
            poll_sec=0,
            sleeper=lambda _seconds: None,
        )

        self.assertIs(False, result["terminal_lifecycle_observed"])
        self.assertIs(False, result["runtime_reported_completion"])
        self.assertTrue(result["reconciliation_required"])

    def test_disconnected_snapshot_never_completes_terminal_lifecycle(self):
        result = observe_terminal_result(
            _SequenceStatusGateway(
                [
                    _status(
                        "request-1",
                        "completed",
                        active_request_id=None,
                        connected=False,
                        lifecycle_state="disconnected",
                    )
                ]
            ),
            _accepted_observation("request-1"),
            timeout_sec=10,
            poll_sec=0,
            sleeper=lambda _seconds: None,
        )

        self.assertIs(False, result["connection_state_verified"])
        self.assertIn("disconnected", result["connection_state_error"])
        self.assertIsNone(result["terminal_lifecycle_observed"])
        self.assertIsNone(result["runtime_reported_completion"])
        self.assertTrue(result["reconciliation_required"])

    def test_first_status_read_failure_marks_connection_unverified(self):
        result = observe_terminal_result(
            _SequenceStatusGateway([RuntimeError("status failed")]),
            _accepted_observation("request-1"),
            timeout_sec=10,
            poll_sec=0,
            sleeper=lambda _seconds: None,
        )

        self.assertIs(False, result["connection_state_verified"])
        self.assertEqual("runtime status read failed", result["connection_state_error"])
        self.assertTrue(result["reconciliation_required"])

    def test_later_status_read_failure_overrides_prior_connection_success(self):
        result = observe_terminal_result(
            _SequenceStatusGateway(
                [
                    _status(
                        "old-request",
                        "completed",
                        active_request_id="request-1",
                    ),
                    RuntimeError("status failed"),
                ]
            ),
            _accepted_observation("request-1"),
            timeout_sec=10,
            poll_sec=0,
            sleeper=lambda _seconds: None,
        )

        self.assertIs(False, result["connection_state_verified"])
        self.assertEqual("runtime status read failed", result["connection_state_error"])
        self.assertTrue(result["reconciliation_required"])

    def test_missing_active_request_field_does_not_complete_lifecycle(self):
        status = _status("request-1", "completed", active_request_id=None)
        del status["details"]["details"]["commands"]["active_request_id"]

        result = observe_terminal_result(
            _SequenceStatusGateway([status]),
            _accepted_observation("request-1"),
            timeout_sec=10,
            poll_sec=0,
            sleeper=lambda _seconds: None,
        )

        self.assertIs(False, result["terminal_lifecycle_observed"])
        self.assertIsNone(result["active_request_clear"])
        self.assertTrue(result["reconciliation_required"])

    def test_blank_active_request_field_does_not_complete_lifecycle(self):
        result = observe_terminal_result(
            _SequenceStatusGateway(
                [_status("request-1", "completed", active_request_id="   ")]
            ),
            _accepted_observation("request-1"),
            timeout_sec=10,
            poll_sec=0,
            sleeper=lambda _seconds: None,
        )

        self.assertIs(False, result["terminal_lifecycle_observed"])
        self.assertIsNone(result["active_request_clear"])
        self.assertTrue(result["reconciliation_required"])

    def test_observer_timeout_does_not_submit_stop_cancel_or_replay(self):
        gateway = _SequenceStatusGateway(
            [_status("old-request", "completed", active_request_id="request-1")],
            repeat_last=True,
        )
        current = [0.0]

        def monotonic():
            current[0] += 0.6
            return current[0]

        result = observe_terminal_result(
            gateway,
            _accepted_observation("request-1"),
            timeout_sec=1,
            poll_sec=0,
            monotonic=monotonic,
            sleeper=lambda _seconds: None,
        )
        self.assertTrue(result["observer_timeout"])
        self.assertEqual(0, result["automatic_resubmit_count"])
        self.assertTrue(result["reconciliation_required"])
        self.assertEqual(0, gateway.submit_calls)


class _SequenceStatusGateway:
    def __init__(self, statuses, *, repeat_last=False):
        self._statuses = list(statuses)
        self._repeat_last = repeat_last
        self._last = {}
        for status in reversed(self._statuses):
            if isinstance(status, dict):
                self._last = dict(status)
                break
        self.read_calls = 0
        self.submit_calls = 0

    def read_status(self):
        self.read_calls += 1
        if self._statuses:
            current = self._statuses.pop(0)
            if isinstance(current, Exception):
                raise current
            self._last = dict(current)
            return dict(self._last)
        if self._repeat_last:
            return dict(self._last)
        raise AssertionError("unexpected status refresh")


def _accepted_observation(request_id: str) -> dict[str, object]:
    observation = new_live_run_observation()
    observation.update(
        {
            "submission_outcome": "accepted",
            "gradio_submit_call_count": 1,
            "submitted_request_id": request_id,
        }
    )
    return observation


def _status(
    request_id: object,
    terminal_status: object,
    *,
    active_request_id: object,
    result_reason: object = "finished",
    connected: object = True,
    lifecycle_state: object = "connected",
) -> dict[str, object]:
    data = {} if result_reason is None else {"result_reason": result_reason}
    return {
        "details": {
            "backend_id": "fabric_chatclef",
            "enabled": True,
            "connected": connected,
            "lifecycle_state": lifecycle_state,
            "details": {
                "commands": {
                    "active_request_id": active_request_id,
                    "last_result": {
                        "request_id": request_id,
                        "status": terminal_status,
                        "data": data,
                    },
                }
            },
        }
    }


if __name__ == "__main__":
    unittest.main()
