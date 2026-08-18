#20260818_kpopmodder: Verify every uncertain batch boundary stops before the next command.
from __future__ import annotations

import unittest

from .batch_fixture_factory import (
    batch_environment_fixture,
    complete_gameplay_checkpoint_fixture,
    complete_inventory_baseline_fixture,
    completed_command_result_fixture,
)
from .supervised_batch_runner import run_supervised_live_batch


class SupervisedBatchFailClosedTests(unittest.TestCase):
    def setUp(self):
        self.environments = [
            batch_environment_fixture("철 1개 캐줘", "batch-iron"),
            batch_environment_fixture("다이아몬드 캐줘", "batch-diamond"),
        ]

    def test_observer_timeout_stops_before_second_command(self):
        result_fixture = completed_command_result_fixture()
        observation = result_fixture["observation"]
        observation["terminal_lifecycle_observed"] = None
        observation["terminal_status"] = "absent"
        observation["runtime_reported_completion"] = None
        observation["observer_timeout"] = True
        calls = _BatchCalls(result_fixture)

        result = self._run(calls)

        self.assertEqual("stopped", result["status"])
        self.assertEqual("observer_timeout", result["stop_reason"])
        self.assertEqual(1, calls.execution_count)
        self.assertEqual(0, calls.checkpoint_count)
        self.assertEqual(0, calls.reconciliation_count)

    def test_unverified_baseline_stops_before_first_submission(self):
        calls = _BatchCalls(completed_command_result_fixture())
        calls.baseline_result = {
            "ok": False,
            "reason": "fresh baseline timeout",
        }

        result = self._run(calls)

        self.assertEqual("stopped", result["status"])
        self.assertEqual("fresh baseline timeout", result["stop_reason"])
        self.assertEqual(1, calls.baseline_count)
        self.assertEqual(0, calls.execution_count)
        self.assertEqual(0, result["attempted_count"])

    def test_submission_outcome_unknown_stops_without_replay(self):
        result_fixture = completed_command_result_fixture()
        result_fixture["observation"][
            "submission_outcome"
        ] = "submission_outcome_unknown"
        calls = _BatchCalls(result_fixture)

        result = self._run(calls)

        self.assertEqual("submission_not_accepted", result["stop_reason"])
        self.assertEqual(1, calls.execution_count)
        self.assertEqual(0, result["automatic_retry_count"])
        self.assertEqual(0, result["automatic_replay_count"])

    def test_incomplete_gameplay_checkpoint_stops_before_reconciliation(self):
        checkpoint = complete_gameplay_checkpoint_fixture()
        checkpoint["gameplay_observation_complete"] = False
        checkpoint["prohibited_effect_absence_verified"] = False
        calls = _BatchCalls(completed_command_result_fixture(), checkpoint=checkpoint)

        result = self._run(calls)

        self.assertEqual("stopped", result["status"])
        self.assertIn("did not prove", result["stop_reason"])
        self.assertEqual(1, calls.execution_count)
        self.assertEqual(1, calls.checkpoint_count)
        self.assertEqual(0, calls.reconciliation_count)

    def test_guard_reconciliation_failure_stops_before_second_command(self):
        calls = _BatchCalls(
            completed_command_result_fixture(),
            reconciliation_ok=False,
        )

        result = self._run(calls)

        self.assertEqual("stopped", result["status"])
        self.assertEqual("guard remains", result["stop_reason"])
        self.assertEqual(1, calls.execution_count)
        self.assertEqual(1, calls.reconciliation_count)

    def test_failed_terminal_stops_before_gameplay_checkpoint(self):
        result_fixture = completed_command_result_fixture()
        observation = result_fixture["observation"]
        observation["terminal_status"] = "failed"
        observation["runtime_reported_completion"] = False
        calls = _BatchCalls(result_fixture)

        result = self._run(calls)

        self.assertEqual("stopped", result["status"])
        self.assertEqual("terminal_status_not_completed", result["stop_reason"])
        self.assertEqual(1, calls.execution_count)
        self.assertEqual(0, calls.checkpoint_count)

    def test_non_same_snapshot_clear_stops_before_next_command(self):
        result_fixture = completed_command_result_fixture()
        result_fixture["observation"][
            "active_clear_observation"
        ] = "bounded_followup"
        calls = _BatchCalls(result_fixture)

        result = self._run(calls)

        self.assertEqual("stopped", result["status"])
        self.assertEqual("active_clear_not_same_snapshot", result["stop_reason"])
        self.assertEqual(1, calls.execution_count)
        self.assertEqual(0, calls.checkpoint_count)

    def test_duplicate_invocation_fails_before_any_command(self):
        environments = [
            batch_environment_fixture("철 1개 캐줘", "duplicate"),
            batch_environment_fixture("다이아몬드 캐줘", "duplicate"),
        ]
        calls = _BatchCalls(completed_command_result_fixture())

        result = run_supervised_live_batch(
            environments,
            execute_command=calls.execute,
            baseline_provider=calls.baseline,
            checkpoint_provider=calls.checkpoint,
            reconcile_guard=calls.reconcile,
        )

        self.assertEqual("stopped", result["status"])
        self.assertIn("duplicated", result["stop_reason"])
        self.assertEqual(0, calls.execution_count)

    def _run(self, calls):
        return run_supervised_live_batch(
            self.environments,
            execute_command=calls.execute,
            baseline_provider=calls.baseline,
            checkpoint_provider=calls.checkpoint,
            reconcile_guard=calls.reconcile,
        )


class _BatchCalls:
    def __init__(
        self,
        command_result,
        *,
        checkpoint=None,
        reconciliation_ok=True,
    ):
        self._command_result = command_result
        self._checkpoint = checkpoint or complete_gameplay_checkpoint_fixture()
        self._reconciliation_ok = reconciliation_ok
        self.baseline_result = complete_inventory_baseline_fixture()
        self.execution_count = 0
        self.baseline_count = 0
        self.checkpoint_count = 0
        self.reconciliation_count = 0

    def baseline(self, _environment):
        self.baseline_count += 1
        return dict(self.baseline_result)

    def execute(self, _environment):
        self.execution_count += 1
        return {
            "preflight": dict(self._command_result["preflight"]),
            "observation": dict(self._command_result["observation"]),
        }

    def checkpoint(self, _environment, _result, _baseline):
        self.checkpoint_count += 1
        return dict(self._checkpoint)

    def reconcile(self, _environment, _result):
        self.reconciliation_count += 1
        return {
            "ok": self._reconciliation_ok,
            "reason": "reconciled" if self._reconciliation_ok else "guard remains",
        }


if __name__ == "__main__":
    unittest.main()
