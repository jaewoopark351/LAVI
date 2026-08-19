#20260819_kpopmodder: Verify only completed runtime and full gameplay evidence may advance.
from __future__ import annotations

import unittest

from .batch_advancement_policy import batch_advancement_error
from .batch_fixture_factory import (
    complete_gameplay_checkpoint_fixture,
    completed_command_result_fixture,
)
from .gameplay_checkpoint import apply_batch_gameplay_checkpoint


class BatchAdvancementPolicyTests(unittest.TestCase):
    def test_completed_runtime_with_full_gameplay_evidence_may_advance(self):
        command_result = completed_command_result_fixture()
        checkpoint_result = _apply_complete_checkpoint(command_result)

        self.assertEqual(
            "",
            batch_advancement_error(command_result, checkpoint_result),
        )

    def test_non_completed_terminal_never_advances_after_checkpoint(self):
        for terminal_status in (
            "rejected",
            "failed",
            "cancelled",
            "deadline_exceeded",
            "unknown",
        ):
            with self.subTest(terminal_status=terminal_status):
                command_result = completed_command_result_fixture()
                observation = command_result["observation"]
                observation["terminal_status"] = terminal_status
                observation["runtime_reported_completion"] = False
                checkpoint_result = _apply_complete_checkpoint(command_result)

                self.assertEqual(
                    "terminal_status_not_completed",
                    batch_advancement_error(command_result, checkpoint_result),
                )

    def test_completed_runtime_requires_checkpoint_success(self):
        command_result = completed_command_result_fixture()
        checkpoint = complete_gameplay_checkpoint_fixture()
        checkpoint["gameplay_observation_complete"] = False
        checkpoint["prohibited_effect_absence_verified"] = None
        observation = command_result["observation"]
        checkpoint_result = apply_batch_gameplay_checkpoint(
            observation,
            checkpoint,
        )

        self.assertIn(
            "did not prove",
            batch_advancement_error(command_result, checkpoint_result),
        )


def _apply_complete_checkpoint(
    command_result: dict[str, object],
) -> dict[str, object]:
    observation = command_result["observation"]
    if not isinstance(observation, dict):
        raise TypeError("fixture observation must be a dictionary")
    return apply_batch_gameplay_checkpoint(
        observation,
        complete_gameplay_checkpoint_fixture(),
    )


if __name__ == "__main__":
    unittest.main()
