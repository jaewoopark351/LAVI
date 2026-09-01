#20260831_kpopmodder: Lock stable metadata-based mods directory discovery.
from __future__ import annotations

import hashlib
import unittest
from dataclasses import replace
from pathlib import Path

from .evidence_test_fixture import HermeticEvidenceFileSystem, fabric_mod_jar
from .mods_directory_snapshot_collector import (
    collect_automatic_deposit_mods_directory_snapshot,
)


class AutomaticDepositModsDirectorySnapshotTests(unittest.TestCase):
    def setUp(self) -> None:
        self.instance = Path("C:/minecraft/instance")
        self.mods = self.instance / "mods"
        self.files = HermeticEvidenceFileSystem(self.instance, self.mods)

    def test_scans_actual_entries_and_hashes_stable_jar_bytes(self):
        chatclef_payload = fabric_mod_jar("altoclef", "1.20.1-0.18.23")
        carry_on_payload = fabric_mod_jar("carryon", "2.1.2.7")
        self.files.add_file(
            self.mods / "chatclef.jar",
            chatclef_payload,
            in_mods=True,
        )
        self.files.add_file(
            self.mods / "carryon.jar",
            carry_on_payload,
            in_mods=True,
        )

        snapshot, reason = collect_automatic_deposit_mods_directory_snapshot(
            self.instance,
            self.mods,
            directory_reader=self.files.read_directory,
            bytes_reader=self.files.read_bytes,
            stat_reader=self.files.read_stat,
        )

        self.assertEqual(reason, "MODS_DIRECTORY_SNAPSHOT_COLLECTED")
        self.assertIsNotNone(snapshot)
        self.assertEqual(
            ("altoclef", "carryon"),
            tuple(sorted(artifact.mod_id for artifact in snapshot.artifacts)),
        )
        self.assertEqual(
            hashlib.sha256(chatclef_payload).hexdigest(),
            next(
                artifact.sha256
                for artifact in snapshot.artifacts
                if artifact.mod_id == "altoclef"
            ),
        )
        with self.assertRaises(ValueError):
            replace(snapshot, mods_directory_fingerprint="f" * 64)

    def test_rejects_jar_that_changes_during_hashing(self):
        jar = self.files.add_file(
            self.mods / "chatclef.jar",
            fabric_mod_jar("altoclef", "1.20.1-0.18.23"),
            in_mods=True,
        )
        self.files.make_unstable(jar)

        snapshot, reason = collect_automatic_deposit_mods_directory_snapshot(
            self.instance,
            self.mods,
            directory_reader=self.files.read_directory,
            bytes_reader=self.files.read_bytes,
            stat_reader=self.files.read_stat,
        )

        self.assertIsNone(snapshot)
        self.assertEqual(reason, "MOD_JAR_STABLE_FILE_CHANGED_DURING_READ")

    def test_rejects_mods_directory_outside_instance(self):
        snapshot, reason = collect_automatic_deposit_mods_directory_snapshot(
            self.instance,
            Path("C:/other/mods"),
            directory_reader=self.files.read_directory,
            bytes_reader=self.files.read_bytes,
            stat_reader=self.files.read_stat,
        )

        self.assertIsNone(snapshot)
        self.assertEqual(reason, "MODS_DIRECTORY_OUTSIDE_INSTANCE")


if __name__ == "__main__":
    unittest.main()
