#20260831_kpopmodder: Lock artifact, fixture, log, and operation evidence binding.
from __future__ import annotations

import unittest
from dataclasses import replace

from ..scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ..testing.hermetic_evidence import (
    hermetic_artifact_collection,
    hermetic_carry_on_collection,
    hermetic_fixture,
    hermetic_gameplay_observation,
    hermetic_log_delta,
    hermetic_run_manifest,
)
from .carry_on_identity_collection import _create_carry_on_identity_collection
from .verified_evidence_builder import build_automatic_deposit_verified_evidence


class AutomaticDepositVerifiedEvidenceBuilderTests(unittest.TestCase):
    def test_correlated_r3_evidence_is_sealed_and_verified(self):
        row, fixture, run = _context("R3")

        result = build_automatic_deposit_verified_evidence(
            row,
            run,
            fixture,
            hermetic_artifact_collection(row),
            hermetic_log_delta(row, run, fixture),
            hermetic_gameplay_observation(run, fixture),
            carry_on_collection=hermetic_carry_on_collection(row),
        )

        self.assertTrue(result.ok, result.errors)
        self.assertEqual(run.operation_id, result.evidence.operation_id)
        self.assertTrue(result.evidence.as_mapping()["runtime_log_complete"])

    def test_artifact_recheck_must_equal_run_manifest_identity(self):
        row, fixture, run = _context("R3")
        collection = hermetic_artifact_collection(row)
        run = replace(
            run,
            artifact_identity=replace(
                run.artifact_identity,
                source_jar_sha256="f" * 64,
                deployed_jar_sha256="f" * 64,
            ),
        )

        result = build_automatic_deposit_verified_evidence(
            row,
            run,
            fixture,
            collection,
            hermetic_log_delta(row, run, fixture),
            hermetic_gameplay_observation(run, fixture),
            carry_on_collection=hermetic_carry_on_collection(row),
        )

        self.assertFalse(result.ok)
        self.assertIn("ARTIFACT_RECHECK_IDENTITY_MISMATCH", result.errors)

    def test_artifact_collection_cannot_be_replaced_after_collection(self):
        row, _fixture, _run = _context("R3")
        collection = hermetic_artifact_collection(row)

        with self.assertRaisesRegex(ValueError, "integrity mismatch"):
            replace(
                collection,
                identity=replace(
                    collection.identity,
                    deployed_jar_sha256="f" * 64,
                ),
            )

    def test_missing_required_runtime_event_is_inconclusive(self):
        row, fixture, run = _context("R3")
        delta = hermetic_log_delta(
            row,
            run,
            fixture,
            omit_keys=frozenset({"transfer_observed"}),
        )

        result = build_automatic_deposit_verified_evidence(
            row,
            run,
            fixture,
            hermetic_artifact_collection(row),
            delta,
            hermetic_gameplay_observation(run, fixture),
            carry_on_collection=hermetic_carry_on_collection(row),
        )

        self.assertFalse(result.ok)
        self.assertIn("RUNTIME_EVIDENCE_KEY_MISSING:transfer_observed", result.errors)

    def test_run_and_fixture_diagnostics_modes_must_match(self):
        row, fixture, run = _context("R3")
        run = replace(run, diagnostics_mode="OFF")

        result = build_automatic_deposit_verified_evidence(
            row,
            run,
            fixture,
            hermetic_artifact_collection(row),
            hermetic_log_delta(row, run, fixture),
            hermetic_gameplay_observation(run, fixture),
            carry_on_collection=hermetic_carry_on_collection(row),
        )

        self.assertFalse(result.ok)
        self.assertIn(
            "RUN_AND_FIXTURE_DIAGNOSTICS_MODE_MISMATCH",
            result.errors,
        )

    #20260901_kpopmodder: Do not combine Carry On evidence from another runtime snapshot.
    def test_carry_on_and_artifact_must_share_runtime_snapshot(self):
        row, fixture, run = _context("R3")
        original = hermetic_carry_on_collection(row)
        mismatched = _create_carry_on_identity_collection(
            True,
            original.reason,
            original.identity,
            runtime_snapshot_fingerprint="f" * 64,
        )

        result = build_automatic_deposit_verified_evidence(
            row,
            run,
            fixture,
            hermetic_artifact_collection(row),
            hermetic_log_delta(row, run, fixture),
            hermetic_gameplay_observation(run, fixture),
            carry_on_collection=mismatched,
        )

        self.assertFalse(result.ok)
        self.assertIn(
            "CARRY_ON_RUNTIME_SNAPSHOT_FINGERPRINT_MISMATCH",
            result.errors,
        )

    def test_paired_p4_and_p5_never_accept_single_run_evidence(self):
        for row_id in ("P4", "P5"):
            row, fixture, run = _context(row_id)
            result = build_automatic_deposit_verified_evidence(
                row,
                run,
                fixture,
                hermetic_artifact_collection(row),
                hermetic_log_delta(row, run, fixture),
                hermetic_gameplay_observation(run, fixture),
                carry_on_collection=hermetic_carry_on_collection(row),
            )

            self.assertFalse(result.ok)
            self.assertEqual("PAIRED_RUN_EVIDENCE_NOT_IMPLEMENTED", result.reason)

    #20260901_kpopmodder: Keep synthetic evidence out of the production-only P1 path.
    def test_generic_builder_rejects_synthetic_p1_evidence(self):
        row, fixture, run = _context("P1")

        result = build_automatic_deposit_verified_evidence(
            row,
            run,
            fixture,
            hermetic_artifact_collection(row),
            hermetic_log_delta(row, run, fixture),
            hermetic_gameplay_observation(run, fixture),
            carry_on_collection=hermetic_carry_on_collection(row),
        )

        self.assertFalse(result.ok)
        self.assertEqual(
            "P1_STORE_HOME_REQUIRES_PRODUCTION_ASSEMBLY",
            result.reason,
        )


def _context(row_id: str):
    row = next(
        row for row in automatic_deposit_matrix_catalog() if row.row_id == row_id
    )
    fixture = hermetic_fixture(row)
    run = hermetic_run_manifest(row, fixture)
    return row, fixture, run


if __name__ == "__main__":
    unittest.main()
