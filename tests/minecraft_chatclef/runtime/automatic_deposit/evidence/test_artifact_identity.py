#20260831_kpopmodder: Lock discovered source, deployment, and loaded-code identity.
from __future__ import annotations

import unittest
from pathlib import Path

from .artifact_identity_collector import (
    collect_automatic_deposit_artifact_identity,
)
from .artifact_identity_contract import (
    verify_automatic_deposit_artifact_identity,
)
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


class AutomaticDepositArtifactIdentityTests(unittest.TestCase):
    def setUp(self) -> None:
        self.instance = Path("C:/minecraft/instance")
        self.mods = self.instance / "mods"
        self.source = Path("C:/repo/build/chatclef.jar")
        self.deployed = self.mods / "chatclef.jar"
        self.files = HermeticEvidenceFileSystem(self.instance, self.mods)

    def test_matching_exclusive_deployment_is_verified(self):
        payload = fabric_mod_jar("altoclef", "1.20.1-0.18.23")
        self.files.add_file(self.source, payload)
        self.files.add_file(self.deployed, payload, in_mods=True)
        mods_snapshot = self._mods_snapshot()
        runtime_snapshot = self._runtime_snapshot(self.deployed)

        collected = collect_automatic_deposit_artifact_identity(
            self.source,
            mods_snapshot,
            runtime_snapshot,
            bytes_reader=self.files.read_bytes,
            stat_reader=self.files.read_stat,
        )
        verified = verify_automatic_deposit_artifact_identity(collected.identity)

        self.assertTrue(collected.ok)
        self.assertTrue(verified.ok)
        self.assertEqual(
            runtime_snapshot.snapshot_fingerprint,
            collected.runtime_snapshot_fingerprint,
        )
        self.assertEqual(
            mods_snapshot.mods_directory_fingerprint,
            collected.mods_directory_fingerprint,
        )

    def test_hash_mismatch_is_inconclusive(self):
        self.files.add_file(
            self.source,
            fabric_mod_jar("altoclef", "source-version"),
        )
        self.files.add_file(
            self.deployed,
            fabric_mod_jar("altoclef", "1.20.1-0.18.23"),
            in_mods=True,
        )
        collected = collect_automatic_deposit_artifact_identity(
            self.source,
            self._mods_snapshot(),
            self._runtime_snapshot(self.deployed),
            bytes_reader=self.files.read_bytes,
            stat_reader=self.files.read_stat,
        )

        verified = verify_automatic_deposit_artifact_identity(collected.identity)

        self.assertFalse(verified.ok)
        self.assertIn("SOURCE_AND_DEPLOYED_JAR_SHA256_DIFFER", verified.errors)

    def test_duplicate_discovered_chatclef_jars_are_rejected(self):
        payload = fabric_mod_jar("altoclef", "1.20.1-0.18.23")
        self.files.add_file(self.source, payload)
        self.files.add_file(self.deployed, payload, in_mods=True)
        self.files.add_file(self.mods / "chatclef-copy.jar", payload, in_mods=True)

        collected = collect_automatic_deposit_artifact_identity(
            self.source,
            self._mods_snapshot(),
            self._runtime_snapshot(self.deployed),
            bytes_reader=self.files.read_bytes,
            stat_reader=self.files.read_stat,
        )

        self.assertFalse(collected.ok)
        self.assertEqual(
            collected.reason,
            "DISCOVERED_CHATCLEF_JAR_NOT_EXCLUSIVE",
        )

    def test_runtime_code_source_must_be_the_scanned_chatclef_jar(self):
        payload = fabric_mod_jar("altoclef", "1.20.1-0.18.23")
        self.files.add_file(self.source, payload)
        self.files.add_file(self.deployed, payload, in_mods=True)
        other = self.mods / "other-chatclef.jar"

        collected = collect_automatic_deposit_artifact_identity(
            self.source,
            self._mods_snapshot(),
            self._runtime_snapshot(other),
            bytes_reader=self.files.read_bytes,
            stat_reader=self.files.read_stat,
        )

        self.assertFalse(collected.ok)
        self.assertEqual(
            "RUNTIME_CODE_SOURCE_NOT_DISCOVERED_CHATCLEF_JAR",
            collected.reason,
        )

    def test_source_jar_must_stay_stable_while_hashed(self):
        payload = fabric_mod_jar("altoclef", "1.20.1-0.18.23")
        self.files.add_file(self.source, payload)
        self.files.add_file(self.deployed, payload, in_mods=True)
        self.files.make_unstable(self.source)

        collected = collect_automatic_deposit_artifact_identity(
            self.source,
            self._mods_snapshot(),
            self._runtime_snapshot(self.deployed),
            bytes_reader=self.files.read_bytes,
            stat_reader=self.files.read_stat,
        )

        self.assertFalse(collected.ok)
        self.assertEqual(
            "SOURCE_ARTIFACT_STABLE_FILE_CHANGED_DURING_READ",
            collected.reason,
        )

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

    def _runtime_snapshot(self, chatclef_path: Path):
        snapshot, reason = collect_automatic_deposit_runtime_artifact_snapshot(
            runtime_status(chatclef_path)
        )
        self.assertEqual(reason, "RUNTIME_ARTIFACT_SNAPSHOT_COLLECTED")
        self.assertIsNotNone(snapshot)
        return snapshot


if __name__ == "__main__":
    unittest.main()
