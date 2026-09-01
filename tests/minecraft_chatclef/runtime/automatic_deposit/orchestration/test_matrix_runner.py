#20260831_kpopmodder: Lock typed evidence and audited submit ownership.
from __future__ import annotations

import unittest
from dataclasses import replace

from ..oracle.matrix_verdict import AutomaticDepositVerdict
from ..scenario.evidence_requirement import AutomaticDepositEvidenceRequirement
from ..scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ..scenario.matrix_row import AutomaticDepositMatrixRow
from ..scenario.transport_mode import AutomaticDepositTransportMode
from ..testing.hermetic_evidence import (
    hermetic_artifact_collection,
    hermetic_carry_on_collection,
    hermetic_fixture,
    hermetic_gameplay_observation,
    hermetic_log_delta,
    hermetic_run_manifest,
)
from .matrix_runner import run_automatic_deposit_matrix_row


class AutomaticDepositMatrixRunnerTests(unittest.TestCase):
    def test_untyped_row_is_inconclusive_without_attribute_access(self):
        typed_row, fixture, run = _context("R3")

        result = run_automatic_deposit_matrix_row(
            {},
            run,
            fixture,
            hermetic_artifact_collection(typed_row),
            selected_modes=(typed_row.transport_mode,),
        )

        self.assertIs(AutomaticDepositVerdict.INCONCLUSIVE, result.verdict)
        self.assertEqual("MATRIX_ROW_NOT_TYPED", result.reason)
        self.assertEqual("", result.row_id)
        self.assertIsNone(result.transport_mode)
        self.assertEqual(0, result.submit_call_count)
        self.assertEqual("", result.as_mapping()["transport_mode"])

    def test_manual_observer_receives_no_submit_capability_and_submits_zero(self):
        row, fixture, run = _context("R7")
        result = _run(
            row,
            run,
            fixture,
            log_delta=hermetic_log_delta(row, run, fixture),
        )

        self.assertIs(AutomaticDepositVerdict.PASS, result.verdict)
        self.assertEqual(0, result.submit_call_count)

    def test_no_command_observer_receives_no_submit_capability_and_submits_zero(self):
        row, fixture, run = _context("R3")
        result = _run(
            row,
            run,
            fixture,
            log_delta=hermetic_log_delta(row, run, fixture),
        )

        self.assertIs(AutomaticDepositVerdict.PASS, result.verdict)
        self.assertEqual(0, result.submit_call_count)

    def test_multiple_modes_stop_before_log_evaluation(self):
        row, fixture, run = _context("R3")

        result = _run(
            row,
            run,
            fixture,
            selected_modes=(
                AutomaticDepositTransportMode.NO_COMMAND_AUTOMATIC_TRIGGER,
                AutomaticDepositTransportMode.OPERATOR_MANUAL_OBSERVE_ONLY,
            ),
            log_delta=hermetic_log_delta(row, run, fixture),
        )

        self.assertIs(AutomaticDepositVerdict.INCONCLUSIVE, result.verdict)

    def test_untyped_observation_mapping_cannot_produce_pass(self):
        row, fixture, run = _context("R3")

        result = _run(
            row,
            run,
            fixture,
            log_delta={
                "artifact_identity_verified": True,
                "runtime_log_complete": True,
            },
        )

        self.assertIs(AutomaticDepositVerdict.INCONCLUSIVE, result.verdict)
        self.assertIn("LATEST_LOG_DELTA_NOT_TYPED", result.violated_contracts)

    def test_wrong_operation_correlation_is_inconclusive(self):
        row, fixture, run = _context("R3")
        delta = hermetic_log_delta(
            row,
            replace(run, operation_id="operation-X3"),
            fixture,
        )

        result = _run(
            row,
            run,
            fixture,
            log_delta=delta,
        )

        self.assertIs(AutomaticDepositVerdict.INCONCLUSIVE, result.verdict)
        self.assertIn(
            "RUNTIME_EVIDENCE_OPERATION_ID_MISMATCH",
            result.violated_contracts,
        )

    def test_supervised_row_is_delegated_to_common_live_pipeline_without_submit(self):
        row, fixture, run = _supervised_context()

        result = _run(
            row,
            run,
            fixture,
            log_delta=hermetic_log_delta(row, run, fixture),
        )

        self.assertIs(AutomaticDepositVerdict.INCONCLUSIVE, result.verdict)
        self.assertEqual(
            "SUPERVISED_REQUIRES_COMMON_LIVE_PIPELINE",
            result.reason,
        )
        self.assertEqual(0, result.submit_call_count)

    def test_paired_runtime_row_is_hard_inconclusive_before_log_evaluation(self):
        row, fixture, run = _context("P5")

        result = _run(
            row,
            run,
            fixture,
            log_delta=hermetic_log_delta(row, run, fixture),
        )

        self.assertIs(AutomaticDepositVerdict.INCONCLUSIVE, result.verdict)
        self.assertEqual("PAIRED_RUN_EVIDENCE_NOT_IMPLEMENTED", result.reason)

    #20260901_kpopmodder: Route P1 through the sealed production-runtime bundle only.
    def test_p1_does_not_accept_synthetic_evidence_without_live_fixture(self):
        row, fixture, run = _context("P1")

        result = _run(
            row,
            run,
            fixture,
            log_delta=hermetic_log_delta(row, run, fixture),
        )

        self.assertIs(AutomaticDepositVerdict.INCONCLUSIVE, result.verdict)
        self.assertEqual(
            "P1_STORE_HOME_LIVE_FIXTURE_BINDING_FAILED",
            result.reason,
        )
        self.assertIn(
            "P1_LIVE_FIXTURE_OBSERVATION_NOT_TYPED",
            result.violated_contracts,
        )


def _row(row_id: str) -> AutomaticDepositMatrixRow:
    return next(
        row for row in automatic_deposit_matrix_catalog() if row.row_id == row_id
    )


def _context(row_id: str):
    row = _row(row_id)
    fixture = hermetic_fixture(row)
    run = hermetic_run_manifest(row, fixture)
    return row, fixture, run


def _supervised_context():
    row = AutomaticDepositMatrixRow(
        row_id="S1",
        title="synthetic supervised boundary",
        transport_mode=AutomaticDepositTransportMode.SUPERVISED_LAVI_SUBMIT,
        required_fixture_fields=(
            "fixture_fingerprint",
            "world_snapshot_id",
            "inventory_snapshot_id",
            "operator_confirmed",
            "carry_on_state",
        ),
        evidence_requirements=(
            AutomaticDepositEvidenceRequirement(
                "artifact_identity_verified", "ARTIFACT_PREFLIGHT", "true"
            ),
            AutomaticDepositEvidenceRequirement(
                "fixture_identity_verified", "OPERATOR_FIXTURE", "true"
            ),
            AutomaticDepositEvidenceRequirement(
                "runtime_log_complete", "RUNTIME_LOG", "true"
            ),
            AutomaticDepositEvidenceRequirement(
                "supervised_command_observed", "RUNTIME_LOG", "true"
            ),
            AutomaticDepositEvidenceRequirement(
                "helper_submit_call_count", "HARNESS_CONTROL", "1"
            ),
        ),
        expected_operator_action="@synthetic",
    )
    fixture = hermetic_fixture(row)
    run = hermetic_run_manifest(row, fixture, invocation_id="invocation-1")
    return row, fixture, run


def _run(row, run, fixture, **kwargs):
    selected_modes = kwargs.pop("selected_modes", (row.transport_mode,))
    gameplay_observation = kwargs.pop(
        "gameplay_observation",
        hermetic_gameplay_observation(run, fixture),
    )
    return run_automatic_deposit_matrix_row(
        row,
        run,
        fixture,
        hermetic_artifact_collection(row),
        selected_modes=selected_modes,
        gameplay_observation=gameplay_observation,
        carry_on_collection=hermetic_carry_on_collection(row),
        **kwargs,
    )


if __name__ == "__main__":
    unittest.main()
