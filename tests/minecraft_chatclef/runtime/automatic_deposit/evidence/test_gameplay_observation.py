#20260831_kpopmodder: Lock gameplay evidence correlation and explicit confirmation.
from __future__ import annotations

import unittest
from dataclasses import replace

from ..scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ..testing.hermetic_evidence import hermetic_fixture, hermetic_run_manifest
from .gameplay_observation_collector import (
    collect_automatic_deposit_gameplay_observation,
)
from .gameplay_observation_contract import (
    verify_automatic_deposit_gameplay_observation,
)


class AutomaticDepositGameplayObservationTests(unittest.TestCase):
    def test_complete_checkpoint_is_bound_to_run_and_fixture(self):
        row, fixture, run = _context()
        manifest, reason = _collect(run, fixture)

        self.assertEqual("GAMEPLAY_OBSERVATION_COLLECTED", reason)
        self.assertEqual(
            (),
            verify_automatic_deposit_gameplay_observation(
                manifest,
                run,
                fixture,
            ),
        )

    def test_operator_confirmation_is_not_auto_set(self):
        _row, fixture, run = _context()
        manifest, reason = _collect(
            run,
            fixture,
            operator_confirmed=False,
        )

        self.assertIsNone(manifest)
        self.assertEqual(
            "GAMEPLAY_OBSERVATION_NOT_OPERATOR_CONFIRMED",
            reason,
        )

    def test_collected_checkpoint_cannot_be_replaced(self):
        _row, fixture, run = _context()
        manifest, _reason = _collect(run, fixture)

        with self.assertRaisesRegex(ValueError, "integrity mismatch"):
            replace(manifest, operation_id="fabricated-operation")


def _context():
    row = next(
        row for row in automatic_deposit_matrix_catalog() if row.row_id == "R3"
    )
    fixture = hermetic_fixture(row)
    run = hermetic_run_manifest(row, fixture)
    return row, fixture, run


def _collect(run, fixture, *, operator_confirmed=True):
    return collect_automatic_deposit_gameplay_observation(
        run_id=run.run_id,
        row_id=run.row_id,
        operation_id=run.operation_id,
        artifact_sha256=run.artifact_identity.deployed_jar_sha256,
        fixture_fingerprint=fixture.fixture_fingerprint,
        before_world_snapshot_id=fixture.world_snapshot_id,
        after_world_snapshot_id="world-after-1",
        before_inventory_snapshot_id=fixture.inventory_snapshot_id,
        after_inventory_snapshot_id="inventory-after-1",
        observation_source="OPERATOR_OBSERVATION",
        observer_identity_fingerprint="d" * 64,
        operator_confirmed=operator_confirmed,
        checkpoint={
            "gameplay_observation_complete": True,
            "gameplay_effect_observed": True,
            "expected_gameplay_effect_verified": True,
            "partial_gameplay_effect_observed": False,
            "unexpected_effect_observed": False,
            "prohibited_effect_absence_verified": True,
        },
    )


if __name__ == "__main__":
    unittest.main()
