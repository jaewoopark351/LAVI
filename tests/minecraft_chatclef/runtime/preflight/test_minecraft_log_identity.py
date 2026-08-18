#20260818_kpopmodder: Lock final active integrated-server session identity.
from __future__ import annotations

import unittest
from pathlib import Path
from unittest.mock import patch

from .minecraft_log_identity import inspect_minecraft_log_identity
from .preflight_fixture_factory import log_snapshot_fixture


class MinecraftLogIdentityFixtureTests(unittest.TestCase):
    @patch.object(Path, "is_dir", return_value=True)
    def test_exact_active_instance_and_world_pass(self, _is_dir):
        result = inspect_minecraft_log_identity(
            "C:/fixture/Instances/LAVI_TEST_Fabric01/logs",
            expected_instance="LAVI_TEST_Fabric01",
            expected_world="전용월드",
            snapshot_reader=lambda _path: log_snapshot_fixture(
                "Starting integrated minecraft server version 1.20.1\n"
                "C:\\fixture\\Instances\\LAVI_TEST_Fabric01\\saves\\전용월드\\level.dat\n"
            ),
        )
        self.assertTrue(result["ok"], result)

    @patch.object(Path, "is_dir", return_value=True)
    def test_incomplete_identity_line_fails_closed(self, _is_dir):
        result = inspect_minecraft_log_identity(
            "C:/fixture/Instances/LAVI_TEST_Fabric01/logs",
            expected_instance="LAVI_TEST_Fabric01",
            expected_world="전용월드",
            snapshot_reader=lambda _path: log_snapshot_fixture(
                "Starting integrated minecraft server version 1.20.1\n"
                "C:\\fixture\\Instances\\LAVI_TEST_Fabric01\\saves\\전용월드\\",
                final_line_complete=False,
            ),
        )
        self.assertFalse(result["ok"])
        self.assertIn("no instance/world", result["reason"])

    @patch.object(Path, "is_dir", return_value=True)
    def test_stopped_session_fails_closed(self, _is_dir):
        result = inspect_minecraft_log_identity(
            "C:/fixture/Instances/LAVI_TEST_Fabric01/logs",
            expected_instance="LAVI_TEST_Fabric01",
            expected_world="전용월드",
            snapshot_reader=lambda _path: log_snapshot_fixture(
                "Starting integrated minecraft server version 1.20.1\n"
                "C:\\fixture\\Instances\\LAVI_TEST_Fabric01\\saves\\전용월드\\level.dat\n"
                "Stopping server\n"
            ),
        )
        self.assertFalse(result["ok"])
        self.assertIn("stopped", result["reason"])

    @patch.object(Path, "is_dir", return_value=True)
    def test_ambiguous_active_session_identity_fails_closed(self, _is_dir):
        result = inspect_minecraft_log_identity(
            "C:/fixture/Instances/LAVI_TEST_Fabric01/logs",
            expected_instance="LAVI_TEST_Fabric01",
            expected_world="전용월드",
            snapshot_reader=lambda _path: log_snapshot_fixture(
                "Starting integrated minecraft server version 1.20.1\n"
                "C:\\fixture\\Instances\\LAVI_TEST_Fabric01\\saves\\전용월드\\level.dat\n"
                "C:\\fixture\\Instances\\LAVI_TEST_Fabric01\\saves\\다른월드\\level.dat\n"
            ),
        )
        self.assertFalse(result["ok"])
        self.assertIn("ambiguous", result["reason"])


if __name__ == "__main__":
    unittest.main()
