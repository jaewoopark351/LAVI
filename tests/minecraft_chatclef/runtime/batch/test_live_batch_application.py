#20260818_kpopmodder: Verify one application run executes all approved commands in order.
from __future__ import annotations

import json
import unittest

from .batch_fixture_factory import (
    complete_gameplay_checkpoint_fixture,
    complete_inventory_baseline_fixture,
    completed_command_result_fixture,
)
from .live_batch_application import run_live_get_batch_application
from .live_get_batch_commands import COMMANDS


class LiveBatchApplicationTests(unittest.TestCase):
    def test_one_run_executes_all_four_commands_sequentially(self):
        environment = _environment_fixture()
        executed: list[str] = []
        observed_targets: list[tuple[str, int]] = []
        gateway_urls: list[str] = []
        reconciled: list[str] = []

        def gateway_factory(url):
            gateway_urls.append(url)
            return object()

        def command_runner(command_environment, _gateway):
            executed.append(str(command_environment["command"]))
            observed_targets.append(
                (
                    str(command_environment["expected_item_id"]),
                    int(command_environment["expected_item_delta"]),
                )
            )
            return completed_command_result_fixture()

        def reconcile(command_environment, _result):
            reconciled.append(str(command_environment["invocation_id"]))
            return {"ok": True, "reason": "reconciled"}

        result = run_live_get_batch_application(
            environment,
            json.dumps(_approval_fixture(), ensure_ascii=False),
            gateway_factory=gateway_factory,
            command_runner=command_runner,
            baseline_provider=lambda _environment: (
                complete_inventory_baseline_fixture()
            ),
            checkpoint_provider=lambda _environment, _result, _baseline: (
                complete_gameplay_checkpoint_fixture()
            ),
            reconcile_guard=reconcile,
        )

        self.assertEqual("completed", result["status"])
        self.assertEqual(list(COMMANDS), executed)
        self.assertEqual(
            [
                ("minecraft:cobblestone", 1),
                ("minecraft:coal", 1),
                ("minecraft:iron_ingot", 1),
                ("minecraft:diamond", 1),
            ],
            observed_targets,
        )
        self.assertEqual(4, len(gateway_urls))
        self.assertEqual(4, len(reconciled))
        self.assertTrue(
            all(step["baseline_status"] == "verified" for step in result["steps"])
        )
        self.assertEqual(0, result["automatic_retry_count"])
        self.assertEqual(0, result["automatic_replay_count"])

    def test_missing_opt_in_skips_without_gateway_creation(self):
        environment = _environment_fixture()
        environment["mutating_opt_in"] = False
        gateway_calls = 0

        def gateway_factory(_url):
            nonlocal gateway_calls
            gateway_calls += 1
            return object()

        result = run_live_get_batch_application(
            environment,
            None,
            gateway_factory=gateway_factory,
        )

        self.assertEqual("skipped", result["status"])
        self.assertEqual(0, gateway_calls)
        self.assertEqual(0, result["attempted_count"])

    def test_truthy_non_boolean_opt_in_skips_without_gateway_creation(self):
        for field in ("live_opt_in", "mutating_opt_in"):
            with self.subTest(field=field):
                environment = _environment_fixture()
                environment[field] = "1"
                gateway_calls = 0

                def gateway_factory(_url):
                    nonlocal gateway_calls
                    gateway_calls += 1
                    return object()

                result = run_live_get_batch_application(
                    environment,
                    None,
                    gateway_factory=gateway_factory,
                )

                self.assertEqual("skipped", result["status"])
                self.assertEqual(0, gateway_calls)

    def test_default_batch_path_does_not_use_console_checkpoint(self):
        import inspect

        default = inspect.signature(
            run_live_get_batch_application
        ).parameters["checkpoint_provider"].default

        self.assertEqual(
            "collect_post_terminal_get_item_checkpoint",
            default.__name__,
        )

    def test_invalid_batch_approval_stops_before_gateway_creation(self):
        gateway_calls = 0

        def gateway_factory(_url):
            nonlocal gateway_calls
            gateway_calls += 1
            return object()

        approval = _approval_fixture()
        approval["commands"][2]["command"] = "금 1개 캐줘"
        result = run_live_get_batch_application(
            _environment_fixture(),
            json.dumps(approval, ensure_ascii=False),
            gateway_factory=gateway_factory,
        )

        self.assertEqual("stopped", result["status"])
        self.assertIn("command mismatch", result["stop_reason"])
        self.assertEqual(0, gateway_calls)
        self.assertEqual(0, result["attempted_count"])


def _environment_fixture() -> dict[str, object]:
    return {
        "live_opt_in": True,
        "mutating_opt_in": True,
        "gradio_url": "http://127.0.0.1:47860",
        "expected_backend": "fabric_chatclef",
        "expected_instance": "LAVI_TEST_Fabric01",
        "expected_world": "test-world",
    }


def _approval_fixture() -> dict[str, object]:
    return {
        "approval_source": "operator-console",
        "gradio_url": "http://127.0.0.1:47860",
        "backend": "fabric_chatclef",
        "instance": "LAVI_TEST_Fabric01",
        "world": "test-world",
        "one_shot": True,
        "automatic_rerun_disabled": True,
        "commands": [
            {"command": command, "invocation_id": f"batch-app-{index}"}
            for index, command in enumerate(COMMANDS, start=1)
        ],
    }


if __name__ == "__main__":
    unittest.main()
