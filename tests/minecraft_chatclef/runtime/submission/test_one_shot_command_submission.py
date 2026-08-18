#20260818_kpopmodder: Verify submit uncertainty and replay prevention with offline gateways.
from __future__ import annotations

import unittest

from .one_shot_command_submission import (
    submit_command_once,
)


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
            {"ok": False, "status": {"status": "rejected", "request_id": "request-1"}}
        )
        observation = submit_command_once(gateway, "돌 1개 가져와줘")
        self.assertEqual("submit_response_not_accepted", observation["submission_outcome"])
        self.assertEqual(1, gateway.submit_call_count)

    def test_explicit_unknown_response_requires_reconciliation(self):
        gateway = _SubmissionGateway(
            {"ok": False, "status": {"status": "unknown", "request_id": "request-1"}}
        )

        observation = submit_command_once(gateway, "test command")

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

class _SubmissionGateway:
    def __init__(self, submit_result):
        self._submit_result = submit_result
        self.submit_call_count = 0

    def submit_korean_command(self, _command):
        self.submit_call_count += 1
        if isinstance(self._submit_result, Exception):
            raise self._submit_result
        return dict(self._submit_result)

def _accepted_payload() -> dict[str, object]:
    return {
        "ok": True,
        "status": {"status": "accepted", "request_id": "request-1"},
    }
if __name__ == "__main__":
    unittest.main()
