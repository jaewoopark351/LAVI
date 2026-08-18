#20260818_kpopmodder: Verify submit uncertainty and replay prevention with offline gateways.
#20260819_kpopmodder: Reject contradictory canonical submit-result mirrors in one-shot runs.
from __future__ import annotations

import unittest

from .one_shot_command_submission import (
    submit_command_once,
)


_DEFAULT_REPORTED_COUNT = object()


class OneShotCommandSubmissionTests(unittest.TestCase):
    def test_accepted_response_records_one_submit(self):
        gateway = _SubmissionGateway(_accepted_payload())
        observation = submit_command_once(gateway, "돌 1개 가져와줘")
        self.assertEqual("accepted", observation["submission_outcome"])
        self.assertEqual("request-1", observation["submitted_request_id"])
        self.assertEqual(1, observation["gradio_submit_call_count"])
        self.assertEqual(0, observation["automatic_resubmit_count"])

    def test_lost_response_is_unknown_and_never_replayed(self):
        gateway = _SubmissionGateway(ConnectionResetError("response lost"))
        observation = submit_command_once(gateway, "돌 1개 가져와줘")
        self.assertEqual("submission_outcome_unknown", observation["submission_outcome"])
        self.assertEqual(1, gateway.submit_call_count)
        self.assertEqual(0, observation["automatic_resubmit_count"])
        self.assertTrue(observation["reconciliation_required"])

    def test_explicit_rejection_is_not_retried(self):
        gateway = _SubmissionGateway(
            _result_payload(
                ok=False,
                status="rejected",
                error_code="invalid_request",
                message="rejected",
            )
        )
        observation = submit_command_once(gateway, "돌 1개 가져와줘")
        self.assertEqual(
            "submit_response_not_accepted",
            observation["submission_outcome"],
        )
        self.assertEqual(1, gateway.submit_call_count)

    def test_explicit_unknown_response_requires_reconciliation(self):
        gateway = _SubmissionGateway(
            _result_payload(
                ok=False,
                status="unknown",
                error_code="internal_error",
                message="outcome unknown",
                data={
                    "submission_outcome": "submission_outcome_unknown",
                    "reconciliation_required": True,
                },
            )
        )

        observation = submit_command_once(gateway, "test command")

        self.assertEqual(
            "submission_outcome_unknown",
            observation["submission_outcome"],
        )
        self.assertTrue(observation["reconciliation_required"])

    def test_contradictory_mirrors_are_unknown_and_never_replayed(self):
        cases = {}

        nested_false = _accepted_payload()
        nested_false["status"]["ok"] = False
        cases["nested_false"] = nested_false

        rejected_nested_true = _result_payload(
            ok=False,
            status="rejected",
            error_code="invalid_request",
            message="rejected",
        )
        rejected_nested_true["status"]["ok"] = True
        cases["rejected_nested_true"] = rejected_nested_true

        accepted_with_error = _accepted_payload()
        accepted_with_error["status"]["error_code"] = "internal_error"
        accepted_with_error["error"] = "internal_error"
        cases["accepted_with_error"] = accepted_with_error

        for name, payload in cases.items():
            with self.subTest(name=name):
                gateway = _SubmissionGateway(payload)
                observation = submit_command_once(gateway, "test command")
                self.assertEqual(
                    "submission_outcome_unknown",
                    observation["submission_outcome"],
                )
                self.assertTrue(observation["reconciliation_required"])
                self.assertEqual(1, gateway.submit_call_count)
                self.assertEqual(0, observation["automatic_resubmit_count"])

    def test_string_or_integer_nested_ok_is_unknown(self):
        for value in ("false", 0, 1):
            with self.subTest(value=value):
                payload = _accepted_payload()
                payload["status"]["ok"] = value
                observation = submit_command_once(
                    _SubmissionGateway(payload),
                    "test command",
                )
                self.assertEqual(
                    "submission_outcome_unknown",
                    observation["submission_outcome"],
                )
                self.assertTrue(observation["reconciliation_required"])

    def test_malformed_status_response_requires_reconciliation(self):
        gateway = _SubmissionGateway({"ok": False, "status": "rejected"})

        observation = submit_command_once(gateway, "test command")

        self.assertEqual(
            "submission_outcome_unknown",
            observation["submission_outcome"],
        )
        self.assertTrue(observation["reconciliation_required"])

    def test_malformed_submit_count_is_not_coerced_to_one(self):
        for value in (True, "1", 1.0, -1, None, RuntimeError("count failed")):
            with self.subTest(value=value):
                gateway = _SubmissionGateway(
                    _accepted_payload(),
                    reported_count=value,
                )
                observation = submit_command_once(gateway, "test command")
                self.assertEqual("unknown", observation["gradio_submit_call_count"])
                self.assertEqual(1, gateway.actual_submit_calls)


class _SubmissionGateway:
    def __init__(self, submit_result, *, reported_count=_DEFAULT_REPORTED_COUNT):
        self._submit_result = submit_result
        self._reported_count = reported_count
        self.actual_submit_calls = 0

    @property
    def submit_call_count(self):
        if self._reported_count is _DEFAULT_REPORTED_COUNT:
            return self.actual_submit_calls
        if isinstance(self._reported_count, Exception):
            raise self._reported_count
        return self._reported_count

    def submit_korean_command(self, _command):
        self.actual_submit_calls += 1
        if isinstance(self._submit_result, Exception):
            raise self._submit_result
        return dict(self._submit_result)


def _accepted_payload() -> dict[str, object]:
    return _result_payload()


def _result_payload(
    *,
    ok=True,
    status="accepted",
    error_code=None,
    message="accepted",
    data=None,
) -> dict[str, object]:
    details = dict(data or {})
    return {
        "ok": ok,
        "status": {
            "request_id": "request-1",
            "ok": ok,
            "status": status,
            "error_code": error_code,
            "message": message,
            "data": dict(details),
        },
        "error": error_code,
        "message": message,
        "details": dict(details),
    }


if __name__ == "__main__":
    unittest.main()
