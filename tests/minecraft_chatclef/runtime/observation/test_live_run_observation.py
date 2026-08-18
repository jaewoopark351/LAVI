#20260818_kpopmodder: Verify runtime completion and gameplay E2E stay distinct.
from __future__ import annotations

import unittest

from .gameplay_outcome import apply_gameplay_evidence, evaluate_end_to_end_success
from .live_run_observation import new_live_run_observation


class GameplayOutcomeEvaluationTests(unittest.TestCase):
    def test_complete_expected_and_prohibited_absence_is_e2e_success(self):
        observation = new_live_run_observation()
        observation["runtime_reported_completion"] = True

        apply_gameplay_evidence(
            observation,
            observation_complete=True,
            effect_observed=True,
            expected_effect_verified=True,
            partial_effect_observed=False,
            unexpected_effect_observed=False,
            prohibited_effect_absence_verified=True,
        )

        self.assertIs(True, observation["end_to_end_success"])

    def test_incomplete_observation_does_not_infer_prohibited_absence(self):
        observation = new_live_run_observation()
        observation["runtime_reported_completion"] = True

        apply_gameplay_evidence(
            observation,
            observation_complete=False,
            effect_observed=True,
            expected_effect_verified=True,
            partial_effect_observed=False,
            unexpected_effect_observed=False,
            prohibited_effect_absence_verified=None,
        )

        self.assertIsNone(observation["end_to_end_success"])
        self.assertTrue(observation["reconciliation_required"])

    def test_partial_effect_prevents_e2e_success(self):
        observation = new_live_run_observation()
        observation.update(
            {
                "runtime_reported_completion": True,
                "gameplay_observation_complete": True,
                "expected_gameplay_effect_verified": True,
                "partial_gameplay_effect_observed": True,
                "unexpected_effect_observed": False,
                "prohibited_effect_absence_verified": True,
            }
        )
        self.assertIs(False, evaluate_end_to_end_success(observation))


if __name__ == "__main__":
    unittest.main()
