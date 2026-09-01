#20260901_kpopmodder: Prove the sealed execution result preserves only the actual common-runner identity and counts.
from __future__ import annotations

import unittest
from dataclasses import replace

from .._test_fixture import complete_p1_supervised_observation
from .p1_supervised_execution_result import seal_p1_supervised_execution_result


class P1SupervisedExecutionResultTests(unittest.TestCase):
    def test_seals_actual_request_id_and_zero_replay_counts(self):
        result = seal_p1_supervised_execution_result(
            {
                "preflight": {"status": "ok"},
                "observation": complete_p1_supervised_observation(),
            }
        )

        self.assertEqual("lavi-gui-request-17", result.submitted_request_id)
        self.assertEqual(1, result.submit_call_count)
        self.assertEqual(0, result.automatic_resubmit_count)
        self.assertEqual(0, result.automatic_rerun_count)
        with self.assertRaises(ValueError):
            replace(result, submitted_request_id="inferred-from-fingerprint")

    def test_does_not_invent_a_request_id_from_absent_observation(self):
        observation = complete_p1_supervised_observation()
        observation["submitted_request_id"] = "absent"

        result = seal_p1_supervised_execution_result(
            {"preflight": {"status": "ok"}, "observation": observation}
        )

        self.assertEqual("", result.submitted_request_id)


if __name__ == "__main__":
    unittest.main()
