#20260831_kpopmodder: Lock bounded Carry On key-binding and config evidence collection.
from __future__ import annotations

import unittest
from dataclasses import replace
from pathlib import Path

from .carry_on_configuration_snapshot_collector import (
    collect_automatic_deposit_carry_on_configuration_snapshot,
)
from .evidence_test_fixture import HermeticEvidenceFileSystem


class AutomaticDepositCarryOnConfigurationSnapshotTests(unittest.TestCase):
    #20260901_kpopmodder: Use the exact controlled Carry On 2.1.2.7 profile contract.
    def setUp(self) -> None:
        self.instance = Path("C:/minecraft/instance")
        self.mods = self.instance / "mods"
        self.files = HermeticEvidenceFileSystem(self.instance, self.mods)
        self.options = self.instance / "options.txt"
        self.client_config = self.instance / "config" / "carryon-client.json"
        self.common_config = self.instance / "config" / "carryon-common.json"
        self.files.add_file(
            self.options,
            b"key_key.carry.desc:key.keyboard.unknown\n",
        )
        self.files.add_file(self.client_config, b'{"renderArms":true}\n')
        self.files.add_file(
            self.common_config,
            b'{"settings":{"maxDistance":2.5}}\n',
        )

    def test_collects_sealed_options_and_carry_on_config_bundle(self):
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
        self.assertEqual(3, len(snapshot.files))
        with self.assertRaises(ValueError):
            replace(snapshot, fingerprint="f" * 64)

    def test_options_only_is_not_exact_configuration_evidence(self):
        snapshot, reason = (
            collect_automatic_deposit_carry_on_configuration_snapshot(
                self.instance,
                (self.options,),
                bytes_reader=self.files.read_bytes,
                stat_reader=self.files.read_stat,
            )
        )

        self.assertIsNone(snapshot)
        self.assertEqual(reason, "CARRY_ON_CONFIGURATION_BUNDLE_INCOMPLETE")

    def test_unrelated_options_content_is_not_carry_on_binding_evidence(self):
        self.files.add_file(self.options, b"key_key.jump:key.keyboard.space\n")

        snapshot, reason = (
            collect_automatic_deposit_carry_on_configuration_snapshot(
                self.instance,
                (self.options, self.client_config, self.common_config),
                bytes_reader=self.files.read_bytes,
                stat_reader=self.files.read_stat,
            )
        )

        self.assertIsNone(snapshot)
        self.assertEqual(
            "CARRY_ON_OPTIONS_BINDING_MISSING_OR_AMBIGUOUS",
            reason,
        )

    def test_exact_config_path_with_invalid_json_is_not_configuration_evidence(self):
        self.files.add_file(self.common_config, b"not-json")

        snapshot, reason = (
            collect_automatic_deposit_carry_on_configuration_snapshot(
                self.instance,
                (self.options, self.client_config, self.common_config),
                bytes_reader=self.files.read_bytes,
                stat_reader=self.files.read_stat,
            )
        )

        self.assertIsNone(snapshot)
        self.assertEqual("CARRY_ON_CONFIG_JSON_INVALID", reason)

    def test_unrelated_in_instance_file_cannot_pose_as_configuration(self):
        unrelated = self.files.add_file(
            self.instance / "config" / "carryon-extra.json",
            b"{}",
        )

        snapshot, reason = (
            collect_automatic_deposit_carry_on_configuration_snapshot(
                self.instance,
                (
                    self.options,
                    self.client_config,
                    self.common_config,
                    unrelated,
                ),
                bytes_reader=self.files.read_bytes,
                stat_reader=self.files.read_stat,
            )
        )

        self.assertIsNone(snapshot)
        self.assertEqual(reason, "CARRY_ON_CONFIG_PATH_NOT_RECOGNIZED")


if __name__ == "__main__":
    unittest.main()
