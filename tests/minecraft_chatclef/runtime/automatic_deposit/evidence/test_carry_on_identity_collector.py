#20260831_kpopmodder: Lock Carry On identity derivation from loader, JAR, and config evidence.
from __future__ import annotations

import unittest
from dataclasses import replace
from pathlib import Path

from .carry_on_identity_collector import (
    collect_automatic_deposit_carry_on_identity,
)
from .carry_on_configuration_snapshot_collector import (
    collect_automatic_deposit_carry_on_configuration_snapshot,
)
from .carry_on_presence import AutomaticDepositCarryOnPresence
from .evidence_test_fixture import (
    HermeticEvidenceFileSystem,
    fabric_mod_jar,
    runtime_status,
)
from .mods_directory_snapshot_collector import (
    collect_automatic_deposit_mods_directory_snapshot,
)
from .runtime_artifact_snapshot_collector import (
    collect_automatic_deposit_runtime_artifact_snapshot,
)


class AutomaticDepositCarryOnIdentityCollectorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.instance = Path("C:/minecraft/instance")
        self.mods = self.instance / "mods"
        self.chatclef = self.mods / "chatclef.jar"
        self.carry_on = self.mods / "carryon.jar"
        self.client_config = self.instance / "config" / "carryon-client.json"
        self.common_config = self.instance / "config" / "carryon-common.json"
        self.options = self.instance / "options.txt"
        self.files = HermeticEvidenceFileSystem(self.instance, self.mods)
        self.files.add_file(
            self.chatclef,
            fabric_mod_jar("altoclef", "1.20.1-0.18.23"),
            in_mods=True,
        )

    def test_installed_identity_is_derived_from_matching_evidence(self):
        self.files.add_file(
            self.carry_on,
            fabric_mod_jar("carryon", "2.1.2.7"),
            in_mods=True,
        )
        self.files.add_file(self.client_config, b'{"renderArms":true}\n')
        self.files.add_file(
            self.common_config,
            b'{"settings":{"maxDistance":2.5}}\n',
        )
        self.files.add_file(
            self.options,
            b"key_key.carry.desc:key.keyboard.unknown\n",
        )
        configuration = self._configuration_snapshot()

        collection = collect_automatic_deposit_carry_on_identity(
            self._mods_snapshot(),
            self._runtime_snapshot(carry_on_path=self.carry_on),
            configuration_snapshot=configuration,
        )

        self.assertTrue(collection.ok)
        self.assertEqual(
            AutomaticDepositCarryOnPresence.INSTALLED,
            collection.identity.presence,
        )
        self.assertEqual(
            configuration.fingerprint,
            collection.identity.config_fingerprint,
        )
        self.assertEqual(
            self._runtime_snapshot(carry_on_path=self.carry_on).snapshot_fingerprint,
            collection.runtime_snapshot_fingerprint,
        )
        with self.assertRaises(ValueError):
            replace(collection, reason="forged")

    def test_absence_requires_both_loader_and_fully_classified_disk_absence(self):
        collection = collect_automatic_deposit_carry_on_identity(
            self._mods_snapshot(),
            self._runtime_snapshot(),
        )

        self.assertTrue(collection.ok)
        self.assertEqual(
            AutomaticDepositCarryOnPresence.ABSENT,
            collection.identity.presence,
        )
        self.assertFalse(collection.identity.loaded)

    def test_unclassified_jar_blocks_false_absence(self):
        self.files.add_file(
            self.mods / "unknown.jar",
            b"not-a-zip",
            in_mods=True,
        )

        collection = collect_automatic_deposit_carry_on_identity(
            self._mods_snapshot(),
            self._runtime_snapshot(),
        )

        self.assertFalse(collection.ok)
        self.assertEqual(
            "CARRY_ON_ABSENCE_BLOCKED_BY_UNCLASSIFIED_JAR",
            collection.reason,
        )

    def test_loader_and_disk_version_mismatch_is_inconclusive(self):
        self.files.add_file(
            self.carry_on,
            fabric_mod_jar("carryon", "2.1.2.7"),
            in_mods=True,
        )

        collection = collect_automatic_deposit_carry_on_identity(
            self._mods_snapshot(),
            self._runtime_snapshot(
                carry_on_path=self.carry_on,
                carry_on_version="2.1.2.8",
            ),
        )

        self.assertFalse(collection.ok)
        self.assertEqual(
            "CARRY_ON_RUNTIME_AND_DISK_VERSION_MISMATCH",
            collection.reason,
        )

    def test_installed_artifact_is_preserved_when_configuration_is_unproven(self):
        self.files.add_file(
            self.carry_on,
            fabric_mod_jar("carryon", "2.1.2.7"),
            in_mods=True,
        )

        collection = collect_automatic_deposit_carry_on_identity(
            self._mods_snapshot(),
            self._runtime_snapshot(carry_on_path=self.carry_on),
            configuration_snapshot=None,
        )

        self.assertFalse(collection.ok)
        self.assertEqual(
            "CARRY_ON_CONFIGURATION_SNAPSHOT_NOT_TYPED",
            collection.reason,
        )
        self.assertEqual(
            AutomaticDepositCarryOnPresence.INSTALLED,
            collection.identity.presence,
        )
        self.assertTrue(collection.identity.loaded)
        self.assertTrue(collection.identity.jar_sha256)
        self.assertFalse(collection.identity.config_fingerprint)

    def _mods_snapshot(self):
        snapshot, reason = collect_automatic_deposit_mods_directory_snapshot(
            self.instance,
            self.mods,
            directory_reader=self.files.read_directory,
            bytes_reader=self.files.read_bytes,
            stat_reader=self.files.read_stat,
        )
        self.assertEqual(reason, "MODS_DIRECTORY_SNAPSHOT_COLLECTED")
        self.assertIsNotNone(snapshot)
        return snapshot

    def _runtime_snapshot(
        self,
        *,
        carry_on_path: Path | None = None,
        carry_on_version: str = "2.1.2.7",
    ):
        snapshot, reason = collect_automatic_deposit_runtime_artifact_snapshot(
            runtime_status(
                self.chatclef,
                carry_on_path=carry_on_path,
                carry_on_version=carry_on_version,
            )
        )
        self.assertEqual(reason, "RUNTIME_ARTIFACT_SNAPSHOT_COLLECTED")
        self.assertIsNotNone(snapshot)
        return snapshot

    def _configuration_snapshot(self):
        snapshot, reason = (
            collect_automatic_deposit_carry_on_configuration_snapshot(
                self.instance,
                (self.options, self.client_config, self.common_config),
                bytes_reader=self.files.read_bytes,
                stat_reader=self.files.read_stat,
            )
        )
        self.assertEqual(reason, "CARRY_ON_CONFIGURATION_SNAPSHOT_COLLECTED")
        self.assertIsNotNone(snapshot)
        return snapshot


if __name__ == "__main__":
    unittest.main()
