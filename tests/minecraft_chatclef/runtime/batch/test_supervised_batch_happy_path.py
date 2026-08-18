#20260818_kpopmodder: Verify fully evidenced commands advance exactly once in order.
from __future__ import annotations

import json
import unittest

from .batch_fixture_factory import (
    batch_environment_fixture,
    complete_gameplay_checkpoint_fixture,
    complete_inventory_baseline_fixture,
    completed_command_result_fixture,
)
from .supervised_batch_runner import run_supervised_live_batch
from .live_get_batch_commands import COMMANDS


class SupervisedBatchHappyPathTests(unittest.TestCase):
    def test_verified_commands_advance_once_in_order_without_raw_text_results(self):
        environments = [
            batch_environment_fixture(command, f"batch-{index}")
            for index, command in enumerate(COMMANDS, start=1)
        ]
        executed: list[str] = []
        reconciled: list[str] = []

        def execute(environment):
            executed.append(str(environment["invocation_id"]))
            return completed_command_result_fixture()

        def reconcile(environment, _result):
            reconciled.append(str(environment["invocation_id"]))
            return {"ok": True, "reason": "operator evidence reconciled"}

        result = run_supervised_live_batch(
            environments,
            execute_command=execute,
            baseline_provider=lambda _environment: (
                complete_inventory_baseline_fixture()
            ),
            checkpoint_provider=lambda _environment, _result, _baseline: (
                complete_gameplay_checkpoint_fixture()
            ),
            reconcile_guard=reconcile,
        )

        self.assertEqual("completed", result["status"])
        self.assertEqual(4, result["attempted_count"])
        self.assertEqual(4, result["completed_count"])
        self.assertEqual(
            ["batch-1", "batch-2", "batch-3", "batch-4"],
            executed,
        )
        self.assertEqual(executed, reconciled)
        self.assertEqual(0, result["automatic_retry_count"])
        self.assertEqual(0, result["automatic_replay_count"])
        serialized = json.dumps(result, ensure_ascii=False)
        for command in COMMANDS:
            self.assertNotIn(command, serialized)

    def test_each_step_finishes_checkpoint_and_reconciliation_before_next_step(self):
        environments = [
            batch_environment_fixture(command, f"ordered-{index}")
            for index, command in enumerate(COMMANDS, start=1)
        ]
        trace: list[str] = []

        def baseline(environment):
            invocation = str(environment["invocation_id"])
            trace.append(f"baseline:{invocation}")
            return complete_inventory_baseline_fixture()

        def execute(environment):
            trace.append(f"execute:{environment['invocation_id']}")
            return completed_command_result_fixture()

        def checkpoint(environment, _result, _baseline):
            trace.append(f"checkpoint:{environment['invocation_id']}")
            return complete_gameplay_checkpoint_fixture()

        def reconcile(environment, _result):
            trace.append(f"reconcile:{environment['invocation_id']}")
            return {"ok": True, "reason": "reconciled"}

        result = run_supervised_live_batch(
            environments,
            execute_command=execute,
            baseline_provider=baseline,
            checkpoint_provider=checkpoint,
            reconcile_guard=reconcile,
        )

        expected_trace: list[str] = []
        for index in range(1, 5):
            invocation = f"ordered-{index}"
            expected_trace.extend(
                (
                    f"baseline:{invocation}",
                    f"execute:{invocation}",
                    f"checkpoint:{invocation}",
                    f"reconcile:{invocation}",
                )
            )
        self.assertEqual("completed", result["status"])
        self.assertEqual(expected_trace, trace)


if __name__ == "__main__":
    unittest.main()
