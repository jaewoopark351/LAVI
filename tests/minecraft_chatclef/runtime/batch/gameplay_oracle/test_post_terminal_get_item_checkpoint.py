#20260818_kpopmodder: Verify automatic post-terminal GET inventory evidence.
from __future__ import annotations

import unittest

from .post_terminal_get_item_checkpoint import (
    collect_post_terminal_get_item_checkpoint,
)


class PostTerminalGetItemCheckpointTests(unittest.TestCase):
    def test_newer_save_proves_target_delta_but_not_complete_gameplay(self):
        checkpoint = collect_post_terminal_get_item_checkpoint(
            _environment(),
            {},
            _baseline(),
            snapshot_reader=lambda _path: {
                "ok": True,
                "mtime_ns": 201,
                "inventory_counts": {"minecraft:coal": 6},
            },
            wall_clock_ns=lambda: 200,
            monotonic=lambda: 0.0,
        )

        self.assertIs(False, checkpoint["gameplay_observation_complete"])
        self.assertIs(True, checkpoint["expected_gameplay_effect_verified"])
        self.assertIsNone(checkpoint["prohibited_effect_absence_verified"])
        self.assertEqual(1, checkpoint["observed_item_delta"])

    def test_save_without_target_increase_remains_incomplete_and_unsuccessful(self):
        checkpoint = collect_post_terminal_get_item_checkpoint(
            _environment(),
            {},
            _baseline(),
            snapshot_reader=lambda _path: {
                "ok": True,
                "mtime_ns": 201,
                "inventory_counts": {"minecraft:coal": 5},
            },
            wall_clock_ns=lambda: 200,
            monotonic=lambda: 0.0,
        )

        self.assertIs(False, checkpoint["gameplay_observation_complete"])
        self.assertIs(False, checkpoint["expected_gameplay_effect_verified"])
        self.assertIsNone(checkpoint["prohibited_effect_absence_verified"])
        self.assertEqual(0, checkpoint["observed_item_delta"])

    def test_no_post_terminal_save_returns_unknown_evidence_without_replay(self):
        now = [0.0]

        checkpoint = collect_post_terminal_get_item_checkpoint(
            _environment(),
            {},
            _baseline(),
            snapshot_reader=lambda _path: {
                "ok": True,
                "mtime_ns": 150,
                "inventory_counts": {"minecraft:coal": 6},
            },
            wall_clock_ns=lambda: 200,
            monotonic=lambda: now[0],
            sleeper=lambda seconds: now.__setitem__(0, now[0] + seconds),
        )

        self.assertIs(False, checkpoint["gameplay_observation_complete"])
        self.assertIsNone(checkpoint["expected_gameplay_effect_verified"])
        self.assertIsNone(checkpoint["prohibited_effect_absence_verified"])

    def test_non_finite_or_boolean_timing_stops_before_snapshot_loop(self):
        invalid_values = (
            True,
            float("nan"),
            float("inf"),
            float("-inf"),
        )
        timing_fields = (
            "gameplay_observation_timeout_sec",
            "gameplay_observation_poll_sec",
        )
        for field in timing_fields:
            for value in invalid_values:
                with self.subTest(field=field, value=value):
                    environment = _environment()
                    environment[field] = value
                    reads = 0

                    def read_snapshot(_path):
                        nonlocal reads
                        reads += 1
                        return {}

                    checkpoint = collect_post_terminal_get_item_checkpoint(
                        environment,
                        {},
                        _baseline(),
                        snapshot_reader=read_snapshot,
                        sleeper=lambda _seconds: self.fail(
                            "invalid timing must not enter the polling loop"
                        ),
                    )

                    self.assertIs(
                        False,
                        checkpoint["gameplay_observation_complete"],
                    )
                    self.assertEqual(
                        "gameplay observation timing is invalid",
                        checkpoint["reason"],
                    )
                    self.assertEqual(0, reads)


def _environment() -> dict[str, object]:
    return {
        "gameplay_observation_timeout_sec": 1.0,
        "gameplay_observation_poll_sec": 0.25,
    }


def _baseline() -> dict[str, object]:
    return {
        "ok": True,
        "player_data_path": "fixture.dat",
        "snapshot_mtime_ns": 100,
        "target_item_id": "minecraft:coal",
        "target_count_before": 5,
        "requested_count": 1,
    }


if __name__ == "__main__":
    unittest.main()
