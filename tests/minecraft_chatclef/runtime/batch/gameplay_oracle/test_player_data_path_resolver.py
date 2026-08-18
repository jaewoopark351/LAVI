#20260818_kpopmodder: Verify playerdata resolution cannot escape the approved world.
from __future__ import annotations

import unittest

from .player_data_path_resolver import resolve_player_data_path


class PlayerDataPathResolverTests(unittest.TestCase):
    def test_world_path_traversal_is_rejected_before_filesystem_search(self):
        path, error = resolve_player_data_path(
            {
                "log_dir": "C:/Instances/LAVI_TEST_Fabric01/logs",
                "expected_instance": "LAVI_TEST_Fabric01",
                "expected_world": "../personal-world",
            }
        )

        self.assertIsNone(path)
        self.assertIn("one path component", error)

    def test_instance_mismatch_is_rejected(self):
        path, error = resolve_player_data_path(
            {
                "log_dir": "C:/Instances/other-instance/logs",
                "expected_instance": "LAVI_TEST_Fabric01",
                "expected_world": "test-world",
            }
        )

        self.assertIsNone(path)
        self.assertIn("does not match", error)


if __name__ == "__main__":
    unittest.main()
