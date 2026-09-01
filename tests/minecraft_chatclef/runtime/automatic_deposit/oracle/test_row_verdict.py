#20260831_kpopmodder: Lock verdicts to sealed correlated evidence.
from __future__ import annotations

import unittest
from dataclasses import replace

from ..evidence.verified_evidence_builder import (
    build_automatic_deposit_verified_evidence,
)
from ..scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ..testing.hermetic_evidence import (
    hermetic_artifact_collection,
    hermetic_carry_on_collection,
    hermetic_fixture,
    hermetic_gameplay_observation,
    hermetic_java_contract,
    hermetic_log_delta,
    hermetic_run_manifest,
)
from .matrix_verdict import AutomaticDepositVerdict
from .row_verdict import evaluate_automatic_deposit_row


class AutomaticDepositRowVerdictTests(unittest.TestCase):
    def test_loose_boolean_mapping_is_inconclusive(self):
        row, fixture, _run, _verified = _verified_context("R3")

        result = evaluate_automatic_deposit_row(
            row,
            fixture,
            {"artifact_identity_verified": True},
            helper_submit_call_count=0,
        )

        self.assertIs(AutomaticDepositVerdict.INCONCLUSIVE, result.verdict)
        self.assertEqual("VERIFIED_EVIDENCE_NOT_TYPED", result.reason)

    def test_sealed_evidence_cannot_be_replaced_with_missing_values(self):
        row, fixture, _run, verified = _verified_context("R3")
        with self.assertRaisesRegex(ValueError, "integrity mismatch"):
            replace(
                verified,
                values=tuple(
                    pair
                    for pair in verified.values
                    if pair[0] != "transfer_observed"
                ),
            )

    def test_verified_handoff_false_has_dedicated_verdict(self):
        row = _row("R1a")
        fixture = hermetic_fixture(row)
        run = hermetic_run_manifest(row, fixture)
        java = hermetic_java_contract(
            row,
            run,
            overrides={"one_tick_barrier_verified": False},
        )
        built = build_automatic_deposit_verified_evidence(
            row,
            run,
            fixture,
            hermetic_artifact_collection(row),
            hermetic_log_delta(row, run, fixture),
            hermetic_gameplay_observation(run, fixture),
            carry_on_collection=hermetic_carry_on_collection(row),
            java_contract_manifest=java,
        )
        self.assertTrue(built.ok, built.errors)

        result = evaluate_automatic_deposit_row(
            row,
            fixture,
            built.evidence,
            helper_submit_call_count=0,
        )

        self.assertIs(
            AutomaticDepositVerdict.HANDOFF_DEFECT_FOUND,
            result.verdict,
        )

    def test_complete_correlated_evidence_passes(self):
        row, fixture, _run, verified = _verified_context("R3")

        result = evaluate_automatic_deposit_row(
            row,
            fixture,
            verified,
            helper_submit_call_count=0,
        )

        self.assertIs(AutomaticDepositVerdict.PASS, result.verdict)

    def test_incomplete_gameplay_observation_is_inconclusive(self):
        row = _row("R3")
        fixture = hermetic_fixture(row)
        run = hermetic_run_manifest(row, fixture)
        built = build_automatic_deposit_verified_evidence(
            row,
            run,
            fixture,
            hermetic_artifact_collection(row),
            hermetic_log_delta(row, run, fixture),
            hermetic_gameplay_observation(
                run,
                fixture,
                overrides={"gameplay_observation_complete": False},
            ),
            carry_on_collection=hermetic_carry_on_collection(row),
        )

        result = evaluate_automatic_deposit_row(
            row,
            fixture,
            built.evidence,
            helper_submit_call_count=0,
        )

        self.assertIs(AutomaticDepositVerdict.INCONCLUSIVE, result.verdict)
        self.assertEqual("GAMEPLAY_EVIDENCE_INCOMPLETE", result.reason)


def _row(row_id: str):
    return next(
        row for row in automatic_deposit_matrix_catalog() if row.row_id == row_id
    )


def _verified_context(row_id: str):
    row = _row(row_id)
    fixture = hermetic_fixture(row)
    run = hermetic_run_manifest(row, fixture)
    built = build_automatic_deposit_verified_evidence(
        row,
        run,
        fixture,
        hermetic_artifact_collection(row),
        hermetic_log_delta(row, run, fixture),
        hermetic_gameplay_observation(run, fixture),
        carry_on_collection=hermetic_carry_on_collection(row),
        java_contract_manifest=hermetic_java_contract(row, run),
    )
    if not built.ok or built.evidence is None:
        raise AssertionError(built.errors)
    return row, fixture, run, built.evidence


if __name__ == "__main__":
    unittest.main()
