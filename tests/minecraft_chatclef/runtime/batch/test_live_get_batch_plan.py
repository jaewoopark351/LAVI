#20260819_kpopmodder: Lock the approved GET batch as immutable typed values.
from __future__ import annotations

import dataclasses
import unittest

from .live_get_batch_commands import COMMANDS, GET_BATCH_PLAN
from .live_get_batch_step import LiveGetBatchStep


class LiveGetBatchPlanTests(unittest.TestCase):
    def test_plan_contains_frozen_typed_steps(self):
        self.assertEqual(4, len(GET_BATCH_PLAN))
        self.assertTrue(all(isinstance(step, LiveGetBatchStep) for step in GET_BATCH_PLAN))
        self.assertEqual(tuple(step.command for step in GET_BATCH_PLAN), COMMANDS)
        self.assertTrue(
            all(
                step.gameplay_test_objective == "get_acquisition_delta"
                for step in GET_BATCH_PLAN
            )
        )

        with self.assertRaises(dataclasses.FrozenInstanceError):
            GET_BATCH_PLAN[0].command = "변조된 명령"


if __name__ == "__main__":
    unittest.main()
