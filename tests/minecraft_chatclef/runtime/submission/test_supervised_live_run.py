#20260818_kpopmodder: Verify supervised composition never bypasses preflight or submits twice.
from __future__ import annotations

import unittest

from ..preflight.preflight_fixture_factory import (
    live_environment_fixture,
    log_identity_result_fixture,
    process_result_fixture,
    runtime_status_fixture,
)
from ..supervised_live_run import run_supervised_live_command


class SupervisedLiveRunTests(unittest.TestCase):
    def test_failed_preflight_submits_zero_commands(self):
        gateway = _SupervisedGateway()
        environment = live_environment_fixture()
        environment["gradio_url"] = ""

        result = run_supervised_live_command(environment, gateway)

        self.assertEqual("fail", result["preflight"]["status"])
        self.assertEqual(0, gateway.submit_call_count)
        self.assertEqual(
            "not_attempted",
            result["observation"]["submission_outcome"],
        )

    def test_successful_supervised_flow_submits_once(self):
        gateway = _SupervisedGateway(
            statuses=[runtime_status_fixture(), runtime_status_fixture()]
        )
        run_guard = _RunGuard()
        reconciliation_recorder = _ReconciliationRecorder()

        def terminal_observer(_gateway, observation, **_kwargs):
            observation["terminal_lifecycle_observed"] = True
            observation["terminal_status"] = "completed"
            observation["runtime_reported_completion"] = True
            return observation

        result = run_supervised_live_command(
            live_environment_fixture(),
            gateway,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
            terminal_observer=terminal_observer,
            run_guard=run_guard,
            reconciliation_recorder=reconciliation_recorder,
        )

        self.assertEqual("ok", result["preflight"]["status"])
        self.assertEqual(1, gateway.submit_call_count)
        self.assertIs(
            True,
            result["observation"]["terminal_lifecycle_observed"],
        )
        self.assertTrue(result["observation"]["reconciliation_required"])
        self.assertEqual(1, run_guard.claim_calls)
        self.assertEqual(1, reconciliation_recorder.record_calls)

    def test_existing_one_shot_block_prevents_submission(self):
        gateway = _SupervisedGateway(
            statuses=[runtime_status_fixture(), runtime_status_fixture()]
        )
        result = run_supervised_live_command(
            live_environment_fixture(),
            gateway,
            process_probe=lambda **_kwargs: process_result_fixture(4100),
            log_identity_inspector=log_identity_result_fixture,
            run_guard=_RunGuard(claim_ok=False),
        )

        self.assertEqual("fail", result["preflight"]["status"])
        self.assertEqual("pre_submit_recheck", result["preflight"]["stage"])
        self.assertEqual(0, gateway.submit_call_count)


class _SupervisedGateway:
    def __init__(self, *, statuses=None):
        self._statuses = list(statuses or [])
        self.submit_call_count = 0

    def submit_korean_command(self, _command):
        self.submit_call_count += 1
        return {
            "ok": True,
            "status": {"status": "accepted", "request_id": "request-1"},
        }

    def read_status(self):
        if not self._statuses:
            raise AssertionError("unexpected status read")
        return dict(self._statuses.pop(0))


class _RunGuard:
    def __init__(self, *, claim_ok=True):
        self._claim_ok = claim_ok
        self.claim_calls = 0

    def claim(self, _invocation_id, _command_fingerprint):
        self.claim_calls += 1
        return {
            "ok": self._claim_ok,
            "reason": "claimed" if self._claim_ok else "existing live-run block",
        }



class _ReconciliationRecorder:
    def __init__(self):
        self.record_calls = 0

    def record(self, _invocation_id, _reason):
        self.record_calls += 1
        return {"ok": True, "reason": "recorded"}


if __name__ == "__main__":
    unittest.main()
