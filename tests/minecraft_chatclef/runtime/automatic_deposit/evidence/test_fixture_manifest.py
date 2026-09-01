#20260831_kpopmodder: Lock row-bound operator fixture validation.
from __future__ import annotations

import unittest
from dataclasses import replace

from ..scenario.matrix_catalog import automatic_deposit_matrix_catalog
from .carry_on_identity import AutomaticDepositCarryOnIdentity
from .carry_on_presence import AutomaticDepositCarryOnPresence
from .fixture_manifest import AutomaticDepositFixtureManifest
from .fixture_manifest_fingerprint import automatic_deposit_fixture_fingerprint
from .fixture_manifest_contract import (
    verify_automatic_deposit_fixture_manifest,
)


class AutomaticDepositFixtureManifestTests(unittest.TestCase):
    def test_r3_fixture_must_supply_exact_existing_container_identity(self):
        row = next(
            row
            for row in automatic_deposit_matrix_catalog()
            if row.row_id == "R3"
        )
        manifest = AutomaticDepositFixtureManifest(
            schema_version="automatic-deposit-fixture/v1",
            row_id="R3",
            fixture_id="fixture-r3",
            fixture_fingerprint="a" * 64,
            world_snapshot_id="world-1",
            inventory_snapshot_id="inventory-1",
            operator_confirmed=True,
            carry_on_identity=AutomaticDepositCarryOnIdentity(
                AutomaticDepositCarryOnPresence.ABSENT,
                False,
                loader_mod_list_fingerprint="c" * 64,
                mods_directory_fingerprint="d" * 64,
                observation_reason="loader and mods snapshots show absence",
            ),
            container_type="CHEST",
            diagnostics_mode="BOUNDARY",
            attributes=(
                ("existing_container_position", "0,64,0"),
                ("eligible_existing_container_count", "1"),
            ),
        )
        manifest = replace(
            manifest,
            fixture_fingerprint=automatic_deposit_fixture_fingerprint(manifest),
        )

        verified = verify_automatic_deposit_fixture_manifest(manifest, row)

        self.assertTrue(verified.ok)

    def test_unconfirmed_fixture_is_inconclusive(self):
        row = next(
            row
            for row in automatic_deposit_matrix_catalog()
            if row.row_id == "R10"
        )
        manifest = AutomaticDepositFixtureManifest(
            schema_version="automatic-deposit-fixture/v1",
            row_id="R10",
            fixture_id="fixture-r10",
            fixture_fingerprint="a" * 64,
            world_snapshot_id="world-1",
            inventory_snapshot_id="inventory-1",
            operator_confirmed=False,
            carry_on_identity=AutomaticDepositCarryOnIdentity(
                AutomaticDepositCarryOnPresence.ABSENT,
                False,
                loader_mod_list_fingerprint="c" * 64,
                mods_directory_fingerprint="d" * 64,
                observation_reason="loader and mods snapshots show absence",
            ),
            attributes=(("protected_inventory_fingerprint", "b" * 64),),
        )
        manifest = replace(
            manifest,
            fixture_fingerprint=automatic_deposit_fixture_fingerprint(manifest),
        )

        verified = verify_automatic_deposit_fixture_manifest(manifest, row)

        self.assertFalse(verified.ok)
        self.assertIn("FIXTURE_NOT_OPERATOR_CONFIRMED", verified.errors)

    #20260901_kpopmodder: Reject a malformed nested Carry On identity without raising.
    def test_nested_carry_on_identity_type_is_inconclusive(self):
        row = next(
            row for row in automatic_deposit_matrix_catalog() if row.row_id == "R3"
        )
        manifest = AutomaticDepositFixtureManifest(
            schema_version="automatic-deposit-fixture/v1",
            row_id="R3",
            fixture_id="fixture-r3-invalid-carry-on",
            fixture_fingerprint="a" * 64,
            world_snapshot_id="world-1",
            inventory_snapshot_id="inventory-1",
            operator_confirmed=True,
            carry_on_identity=None,
            container_type="CHEST",
            diagnostics_mode="BOUNDARY",
            attributes=(
                ("existing_container_position", "0,64,0"),
                ("eligible_existing_container_count", "1"),
            ),
        )

        verified = verify_automatic_deposit_fixture_manifest(manifest, row)

        self.assertFalse(verified.ok)
        self.assertIn("CARRY_ON_IDENTITY_NOT_TYPED", verified.errors)


if __name__ == "__main__":
    unittest.main()
