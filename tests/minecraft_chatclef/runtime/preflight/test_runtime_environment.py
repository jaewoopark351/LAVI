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
        self.assertEqual(
            "get_acquisition_delta",
            environment["gameplay_test_objective"],
        )
        self.assertEqual("korean", environment["transport"])

    def test_invalid_gameplay_timing_fails_environment_validation_later(self):
        environment = load_live_runtime_environment(
            {"LAVI_MINECRAFT_GAMEPLAY_OBSERVATION_TIMEOUT_SEC": "0"}
        )

        self.assertEqual(-1.0, environment["gameplay_observation_timeout_sec"])

    def test_explicit_gameplay_objective_is_preserved_for_batch_policy(self):
        environment = load_live_runtime_environment(
            {"LAVI_MINECRAFT_GAMEPLAY_TEST_OBJECTIVE": "movement_and_mining"}
        )

        self.assertEqual(
            "movement_and_mining",
            environment["gameplay_test_objective"],
        )

    def test_explicit_raw_transport_is_preserved_for_approval_binding(self):
        environment = load_live_runtime_environment(
            {"LAVI_MINECRAFT_RUNTIME_COMMAND_TRANSPORT": "raw"}
        )

        self.assertEqual("raw", environment["transport"])


if __name__ == "__main__":
    unittest.main()
