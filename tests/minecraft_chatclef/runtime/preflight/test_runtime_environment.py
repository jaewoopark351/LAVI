#20260818_kpopmodder: Verify bounded automatic batch observation defaults.
from __future__ import annotations

import unittest

from .runtime_environment import load_live_runtime_environment


class RuntimeEnvironmentTests(unittest.TestCase):
    def test_defaults_allow_long_mining_and_one_autosave_window(self):
        environment = load_live_runtime_environment({})

        self.assertEqual(900.0, environment["timeout_sec"])
        self.assertEqual(
            390.0,
            environment["gameplay_observation_timeout_sec"],
        )
        self.assertEqual(2.0, environment["gameplay_observation_poll_sec"])
        self.assertEqual(30.0, environment["gameplay_snapshot_max_age_sec"])

    def test_invalid_gameplay_timing_fails_environment_validation_later(self):
        environment = load_live_runtime_environment(
            {"LAVI_MINECRAFT_GAMEPLAY_OBSERVATION_TIMEOUT_SEC": "0"}
        )

        self.assertEqual(-1.0, environment["gameplay_observation_timeout_sec"])


if __name__ == "__main__":
    unittest.main()
