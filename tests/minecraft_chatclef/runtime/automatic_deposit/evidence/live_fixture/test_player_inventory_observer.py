#20260901_kpopmodder: Verify saved-player SHA binding rejects cross-read changes.
from __future__ import annotations

import os
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

from ._fixture_builders import write_player_dat
from .player_inventory_observer import observe_saved_player_inventory


class PlayerInventoryObserverTests(unittest.TestCase):
    def test_stable_player_inventory_is_bound_to_file_sha(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "fixture.dat"
            write_player_dat(path, ((0, "minecraft:diamond", 3),))

            observation, reason = observe_saved_player_inventory(path.resolve())

            self.assertEqual("PLAYER_INVENTORY_OBSERVED", reason)
            assert observation is not None
            self.assertEqual((("minecraft:diamond", 3),), observation.inventory_counts)
            self.assertEqual(64, len(observation.player_data_sha256))
            self.assertEqual(64, len(observation.inventory_fingerprint))

    def test_change_between_decoded_snapshot_and_sha_read_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "fixture.dat"
            write_player_dat(path, ((0, "minecraft:diamond", 3),))
            current = path.stat()
            stats = iter(
                (
                    _stat_like(current, mtime_ns=current.st_mtime_ns),
                    _stat_like(current, mtime_ns=current.st_mtime_ns),
                    _stat_like(current, mtime_ns=current.st_mtime_ns + 1),
                    _stat_like(current, mtime_ns=current.st_mtime_ns + 1),
                )
            )

            observation, reason = observe_saved_player_inventory(
                path.resolve(), stat_reader=lambda _path: next(stats)
            )

            self.assertIsNone(observation)
            self.assertEqual(
                "PLAYER_INVENTORY_CHANGED_BETWEEN_OBSERVATIONS", reason
            )


def _stat_like(value: os.stat_result, *, mtime_ns: int) -> object:
    return SimpleNamespace(
        st_dev=value.st_dev,
        st_ino=value.st_ino,
        st_mode=value.st_mode,
        st_size=value.st_size,
        st_mtime_ns=mtime_ns,
    )
