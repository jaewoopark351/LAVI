#20260818_kpopmodder: Verify every uncertain batch boundary stops before the next command.
from __future__ import annotations

import unittest

from .batch_fixture_factory import (
    batch_environment_fixture,
    complete_gameplay_checkpoint_fixture,
    complete_inventory_baseline_fixture,
    completed_command_result_fixture,
)
from .gameplay_oracle.get_item_delta_oracle import get_item_delta_checkpoint
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
        self.assertEqual(0, calls.checkpoint_count)
        self.assertEqual(0, calls.reconciliation_count)
        self.assertEqual(0, result["automatic_retry_count"])
        self.assertEqual(0, result["automatic_replay_count"])

    def test_request_mismatch_stops_without_gameplay_checkpoint(self):
        result_fixture = completed_command_result_fixture()
        result_fixture["observation"]["terminal_request_id"] = "request-2"
        calls = _BatchCalls(result_fixture)

        result = self._run(calls)

        self.assertEqual("terminal_request_id_mismatch", result["stop_reason"])
        self.assertEqual(1, calls.execution_count)
        self.assertEqual(0, calls.checkpoint_count)
        self.assertEqual(0, calls.reconciliation_count)

    def test_malformed_submit_counts_stop_before_gameplay_checkpoint(self):
        cases = (
            ("gradio_submit_call_count", True, "submit_call_count_violation"),
            ("gradio_submit_call_count", "1", "submit_call_count_violation"),
            ("gradio_submit_call_count", 1.0, "submit_call_count_violation"),
            (
                "adapter_command_request_count",
                True,
                "adapter_command_request_count_violation",
            ),
            (
                "adapter_command_request_count",
                "1",
                "adapter_command_request_count_violation",
            ),
            (
                "adapter_command_request_count",
                1.0,
                "adapter_command_request_count_violation",
            ),
        )
        for field, value, reason in cases:
            with self.subTest(field=field, value=value):
                result_fixture = completed_command_result_fixture()
                result_fixture["observation"][field] = value
                calls = _BatchCalls(result_fixture)

                result = self._run(calls)

                self.assertEqual(reason, result["stop_reason"])
                self.assertEqual(1, calls.execution_count)
                self.assertEqual(0, calls.checkpoint_count)

    def test_unverified_runtime_connection_stops_before_gameplay_checkpoint(self):
        result_fixture = completed_command_result_fixture()
        result_fixture["observation"]["connection_state_verified"] = False
        calls = _BatchCalls(result_fixture)

        result = self._run(calls)

        self.assertEqual("runtime_connection_not_verified", result["stop_reason"])
        self.assertEqual(1, calls.execution_count)
        self.assertEqual(0, calls.checkpoint_count)

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

    def test_target_delta_only_checkpoint_never_auto_advances(self):
        checkpoint = get_item_delta_checkpoint(
            before_count=4,
            after_count=5,
            requested_count=1,
        )
        calls = _BatchCalls(
            completed_command_result_fixture(),
            checkpoint=checkpoint,
        )

        result = self._run(calls)

        self.assertEqual("stopped", result["status"])
        self.assertIn("broader gameplay observation incomplete", result["stop_reason"])
        self.assertEqual(1, calls.execution_count)
        self.assertEqual(1, calls.checkpoint_count)
        self.assertEqual(0, calls.reconciliation_count)
        step = result["steps"][0]
        self.assertIs(False, step["gameplay_observation_complete"])
        self.assertIs(True, step["expected_gameplay_effect_verified"])
        self.assertIs(False, step["partial_gameplay_effect_observed"])
        self.assertIsNone(step["prohibited_effect_absence_verified"])

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

    def test_non_completed_terminal_records_effects_then_stops(self):
        checkpoint = {
            "reason": "partial and unexpected effects observed",
            "gameplay_observation_complete": True,
            "gameplay_effect_observed": True,
            "expected_gameplay_effect_verified": False,
            "partial_gameplay_effect_observed": True,
            "unexpected_effect_observed": True,
            "prohibited_effect_absence_verified": False,
        }
        for terminal_status in (
            "failed",
            "cancelled",
            "deadline_exceeded",
            "unknown",
        ):
            with self.subTest(terminal_status=terminal_status):
                result_fixture = completed_command_result_fixture()
                observation = result_fixture["observation"]
                observation["terminal_status"] = terminal_status
                observation["runtime_reported_completion"] = False
                calls = _BatchCalls(result_fixture, checkpoint=checkpoint)

                result = self._run(calls)

                self.assertEqual("stopped", result["status"])
                self.assertEqual(
                    "terminal_status_not_completed",
                    result["stop_reason"],
                )
                self.assertEqual(1, calls.execution_count)
                self.assertEqual(1, calls.checkpoint_count)
                self.assertEqual(0, calls.reconciliation_count)
                step = result["steps"][0]
                self.assertIs(True, step["partial_gameplay_effect_observed"])
                self.assertIs(True, step["unexpected_effect_observed"])
                self.assertIs(
                    False,
                    step["prohibited_effect_absence_verified"],
                )

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

    def test_process_identity_is_fixed_before_the_second_submission(self):
        first_result = completed_command_result_fixture(process_id=4100)
        second_result = completed_command_result_fixture(process_id=4200)
        approved_identity = first_result["preflight"]["observed"][
            "process_identity_fingerprint"
        ]
        submit_count = 0
        execution_count = 0
        expected_identity_inputs: list[object] = []

        def execute(environment):
            nonlocal execution_count, submit_count
            execution_count += 1
            expected_identity_inputs.append(
                environment.get("expected_process_identity_fingerprint")
            )
            result = first_result if execution_count == 1 else second_result
            actual_identity = result["preflight"]["observed"][
                "process_identity_fingerprint"
            ]
            expected_identity = environment.get(
                "expected_process_identity_fingerprint"
            )
            if expected_identity is not None and expected_identity != actual_identity:
                return {
                    "preflight": {
                        "status": "fail",
                        "reason": "listener process identity does not match the approved batch owner",
                        "observed": result["preflight"]["observed"],
                    },
                    "observation": result["observation"],
                }
            submit_count += 1
            return result

        result = run_supervised_live_batch(
            self.environments,
            execute_command=execute,
            baseline_provider=lambda _environment: (
                complete_inventory_baseline_fixture()
            ),
            checkpoint_provider=lambda _environment, _result, _baseline: (
                complete_gameplay_checkpoint_fixture()
            ),
            reconcile_guard=lambda _environment, _result: {
                "ok": True,
                "reason": "reconciled",
            },
        )

        self.assertEqual("stopped", result["status"])
        self.assertEqual("preflight_not_ok", result["stop_reason"])
        self.assertEqual([None, approved_identity], expected_identity_inputs)
        self.assertEqual(1, submit_count)

    def test_baseline_cannot_mutate_the_approved_command_before_submission(self):
        execution_count = 0

        def mutate_baseline(environment):
            environment["command"] = "다이아몬드 64개 캐줘"
            return complete_inventory_baseline_fixture()

        def execute(_environment):
            nonlocal execution_count
            execution_count += 1
            return completed_command_result_fixture()

        result = run_supervised_live_batch(
            self.environments,
            execute_command=execute,
            baseline_provider=mutate_baseline,
            checkpoint_provider=lambda _environment, _result, _baseline: (
                complete_gameplay_checkpoint_fixture()
            ),
            reconcile_guard=lambda _environment, _result: {
                "ok": True,
                "reason": "reconciled",
            },
        )

        self.assertEqual("stopped", result["status"])
        self.assertEqual("baseline_exception: TypeError", result["stop_reason"])
        self.assertEqual(0, execution_count)

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
