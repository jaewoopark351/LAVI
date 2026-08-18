#20260818_kpopmodder: Verify console evidence maps to explicit gameplay fields.
from __future__ import annotations

import unittest

from .console_gameplay_checkpoint import collect_console_gameplay_checkpoint


class ConsoleGameplayCheckpointTests(unittest.TestCase):
    def test_success_confirmation_requires_complete_expected_and_absence_evidence(self):
        answers = iter(("y", "y", "n", "n", "y"))
        output: list[str] = []

        checkpoint = collect_console_gameplay_checkpoint(
            {"command": "조약돌 1개 캐줘"},
            {"observation": {"terminal_status": "completed"}},
            reader=lambda _prompt: next(answers),
            writer=output.append,
        )

        self.assertIs(True, checkpoint["gameplay_observation_complete"])
        self.assertIs(True, checkpoint["expected_gameplay_effect_verified"])
        self.assertIs(False, checkpoint["partial_gameplay_effect_observed"])
        self.assertIs(False, checkpoint["unexpected_effect_observed"])
        self.assertIs(True, checkpoint["prohibited_effect_absence_verified"])
        self.assertTrue(any("조약돌 1개 캐줘" in line for line in output))

    def test_invalid_answer_reprompts_without_resubmitting_a_command(self):
        answers = iter(("", "y", "y", "n", "n", "y"))
        output: list[str] = []

        checkpoint = collect_console_gameplay_checkpoint(
            {"command": "철 1개 캐줘"},
            {"observation": {"terminal_status": "completed"}},
            reader=lambda _prompt: next(answers),
            writer=output.append,
        )

        self.assertIs(True, checkpoint["gameplay_observation_complete"])
        self.assertIn("y 또는 n으로 입력해 주세요.", output)


if __name__ == "__main__":
    unittest.main()
