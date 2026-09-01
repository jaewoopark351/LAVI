#20260901_kpopmodder: Specify the public one-shot guard observation entry point.
from __future__ import annotations

import unittest
from pathlib import Path

from . import OneShotGuardState, observe_one_shot_guard_state


class ObserveOneShotGuardStateTests(unittest.TestCase):
    def test_public_entry_point_returns_observation_and_reason(self):
        repository_root = Path(__file__).resolve().parents[5]

        observation, reason = observe_one_shot_guard_state(
            str(repository_root),
            state_directory=(
                repository_root / "tests" / "tmp_guard_state_public_api_fixture"
            ),
            record_exists=lambda _path: False,
            record_reader=lambda _path: self.fail("clear state must not be read"),
        )

        self.assertIs(OneShotGuardState.CLEAR, observation.state)
        self.assertEqual(observation.reason, reason)


if __name__ == "__main__":
    unittest.main()
