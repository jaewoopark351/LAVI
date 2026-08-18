#20260818_kpopmodder: Verify each batch step receives its own exact flat approval.
from __future__ import annotations

import json
import unittest

from .batch_environment_builder import build_batch_command_environments


class BatchEnvironmentBuilderTests(unittest.TestCase):
    def test_each_environment_has_matching_command_and_invocation_approval(self):
        approval = {
            "approval_source": "operator-console",
            "gradio_url": "http://127.0.0.1:47860",
            "backend": "fabric_chatclef",
            "instance": "LAVI_TEST_Fabric01",
            "world": "test-world",
        }
        steps = [
            {
                "command": "조약돌 1개 캐줘",
                "invocation_id": "batch-1",
                "expected_item_id": "minecraft:cobblestone",
                "expected_item_delta": 1,
                "gameplay_test_objective": "get_acquisition_delta",
            },
            {
                "command": "석탄 1개 캐와줘",
                "invocation_id": "batch-2",
                "expected_item_id": "minecraft:coal",
                "expected_item_delta": 1,
                "gameplay_test_objective": "get_acquisition_delta",
            },
        ]

        environments = build_batch_command_environments(
            {"timeout_sec": 60.0},
            approval,
            steps,
        )

        self.assertEqual(2, len(environments))
        for environment, step in zip(environments, steps, strict=True):
            flat_approval = json.loads(str(environment["approval_json"]))
            self.assertEqual(step["command"], environment["command"])
            self.assertEqual(step["invocation_id"], environment["invocation_id"])
            self.assertEqual(step["command"], flat_approval["command"])
            self.assertEqual(step["invocation_id"], flat_approval["invocation_id"])
            self.assertEqual(step["expected_item_id"], environment["expected_item_id"])
            self.assertEqual(
                step["expected_item_delta"],
                environment["expected_item_delta"],
            )
            self.assertEqual(
                step["gameplay_test_objective"],
                environment["gameplay_test_objective"],
            )
            self.assertIs(True, flat_approval["one_shot"])
            self.assertIs(True, flat_approval["automatic_rerun_disabled"])


if __name__ == "__main__":
    unittest.main()
