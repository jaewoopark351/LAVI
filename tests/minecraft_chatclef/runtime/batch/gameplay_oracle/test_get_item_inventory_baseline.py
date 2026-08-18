#20260818_kpopmodder: Verify GET baseline capture fails closed before submission.
from __future__ import annotations

import unittest
from pathlib import Path

from .get_item_inventory_baseline import capture_get_item_inventory_baseline


class GetItemInventoryBaselineTests(unittest.TestCase):
    def test_fresh_target_count_is_captured(self):
        baseline = capture_get_item_inventory_baseline(
            _environment(),
            path_resolver=lambda _environment: (Path("fixture.dat"), ""),
            snapshot_reader=lambda _path: {
                "ok": True,
                "mtime_ns": 1_000,
                "inventory_counts": {"minecraft:coal": 5},
            },
            wall_clock_ns=lambda: 1_000,
            monotonic=lambda: 0.0,
        )

        self.assertIs(True, baseline["ok"])
        self.assertEqual(5, baseline["target_count_before"])
        self.assertEqual("minecraft:coal", baseline["target_item_id"])

    def test_ambiguous_playerdata_stops_before_snapshot_read(self):
        reads = 0

        def read_snapshot(_path):
            nonlocal reads
            reads += 1
            return {}

        baseline = capture_get_item_inventory_baseline(
            _environment(),
            path_resolver=lambda _environment: (
                None,
                "expected exactly one playerdata file, observed 2",
            ),
            snapshot_reader=read_snapshot,
        )

        self.assertIs(False, baseline["ok"])
        self.assertEqual(0, reads)


def _environment() -> dict[str, object]:
    return {
        "expected_item_id": "minecraft:coal",
        "expected_item_delta": 1,
        "gameplay_observation_timeout_sec": 1.0,
        "gameplay_observation_poll_sec": 0.1,
        "gameplay_snapshot_max_age_sec": 1.0,
    }


if __name__ == "__main__":
    unittest.main()
