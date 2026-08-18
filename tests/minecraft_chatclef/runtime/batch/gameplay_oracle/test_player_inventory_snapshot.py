#20260818_kpopmodder: Verify stable snapshot and inventory-count normalization.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from .player_inventory_snapshot import read_stable_player_inventory_snapshot


class PlayerInventorySnapshotTests(unittest.TestCase):
    def test_duplicate_item_stacks_are_summed(self):
        stat = SimpleNamespace(st_size=4, st_mtime_ns=123)

        snapshot = read_stable_player_inventory_snapshot(
            "fixture.dat",
            byte_reader=lambda _path: b"data",
            stat_reader=lambda _path: stat,
            document_reader=lambda _raw: {
                "Inventory": [
                    {"id": "minecraft:diamond", "Count": 1},
                    {"id": "minecraft:diamond", "Count": 2},
                ]
            },
        )

        self.assertIs(True, snapshot["ok"])
        self.assertEqual(3, snapshot["inventory_counts"]["minecraft:diamond"])
        self.assertEqual(123, snapshot["mtime_ns"])

    def test_changed_file_is_not_reported_as_a_stable_snapshot(self):
        stats = iter(
            (
                SimpleNamespace(st_size=4, st_mtime_ns=100),
                SimpleNamespace(st_size=4, st_mtime_ns=101),
            )
        )

        snapshot = read_stable_player_inventory_snapshot(
            "fixture.dat",
            attempts=1,
            byte_reader=lambda _path: b"data",
            stat_reader=lambda _path: next(stats),
            document_reader=lambda _raw: {"Inventory": []},
        )

        self.assertIs(False, snapshot["ok"])
        self.assertIn("changed", snapshot["reason"])


if __name__ == "__main__":
    unittest.main()
