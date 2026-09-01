# 20260901_kpopmodder: Require one sealed direct observation for a manual P1 action.
from __future__ import annotations

import unittest
from dataclasses import replace

from .manual_operator_action_collector import (
    collect_manual_operator_action_observation,
)


class ManualOperatorActionObservationTests(unittest.TestCase):
    def test_collects_raw_action_without_turning_a_fingerprint_into_an_id(self):
        observation, reason = collect_manual_operator_action_observation(
            run_id="run-P1",
            row_id="P1",
            harness_operation_id="operation-P1",
            action_text="@store_home",
            observation_source="OPERATOR_ACTION_OBSERVER",
            observer_identity_fingerprint="a" * 64,
            operator_confirmed=True,
        )

        self.assertEqual("MANUAL_OPERATOR_ACTION_OBSERVED", reason)
        self.assertIsNotNone(observation)
        self.assertEqual("@store_home", observation.action_text)
        self.assertEqual("operation-P1", observation.harness_operation_id)
        self.assertFalse(hasattr(observation, "java_operation_id"))
        self.assertFalse(hasattr(observation, "command_request_id"))
        with self.assertRaisesRegex(ValueError, "integrity mismatch"):
            replace(observation, action_text="@stop")

    def test_rejects_unconfirmed_or_untyped_manual_action_fields(self):
        cases = (
            (
                {"operator_confirmed": False},
                "MANUAL_OPERATOR_ACTION_NOT_CONFIRMED",
            ),
            (
                {"action_text": ""},
                "MANUAL_OPERATOR_ACTION_TEXT_INVALID",
            ),
            (
                {"harness_operation_id": 17},
                "MANUAL_OPERATOR_ACTION_HARNESS_OPERATION_ID_INVALID",
            ),
            (
                {"observer_identity_fingerprint": "not-a-sha"},
                "MANUAL_OPERATOR_ACTION_OBSERVER_IDENTITY_INVALID",
            ),
        )
        baseline = {
            "run_id": "run-P1",
            "row_id": "P1",
            "harness_operation_id": "operation-P1",
            "action_text": "@store_home",
            "observation_source": "OPERATOR_ACTION_OBSERVER",
            "observer_identity_fingerprint": "a" * 64,
            "operator_confirmed": True,
        }
        for overrides, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                observation, reason = collect_manual_operator_action_observation(
                    **(baseline | overrides)
                )

                self.assertIsNone(observation)
                self.assertEqual(expected_reason, reason)


if __name__ == "__main__":
    unittest.main()
