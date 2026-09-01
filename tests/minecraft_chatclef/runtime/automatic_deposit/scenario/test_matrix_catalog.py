#20260831_kpopmodder: Lock row coverage and evidence ownership for the runtime matrix.
from __future__ import annotations

import unittest

from .matrix_catalog import automatic_deposit_matrix_catalog
from .transport_mode import AutomaticDepositTransportMode


class AutomaticDepositMatrixCatalogTests(unittest.TestCase):
    def test_catalog_contains_r1a_through_r10_and_p1_through_p5(self):
        rows = automatic_deposit_matrix_catalog()

        self.assertEqual(
            (
                "R1a",
                "R1b",
                "R2a",
                "R2b",
                "R3",
                "R4",
                "R5",
                "R6",
                "R7",
                "R8a",
                "R8b",
                "R9a",
                "R9b",
                "R10",
                "P1",
                "P2",
                "P3",
                "P4",
                "P5",
            ),
            tuple(row.row_id for row in rows),
        )

    def test_manual_rows_cannot_select_supervised_submit(self):
        manual_ids = {"R4", "R7", "P1", "P2", "P3"}
        rows = automatic_deposit_matrix_catalog()

        self.assertTrue(
            all(
                row.transport_mode
                is AutomaticDepositTransportMode.OPERATOR_MANUAL_OBSERVE_ONLY
                for row in rows
                if row.row_id in manual_ids
            )
        )

    def test_p1_remains_exact_manual_store_home_with_zero_harness_submit(self):
        row = next(
            row for row in automatic_deposit_matrix_catalog() if row.row_id == "P1"
        )
        requirements = {
            requirement.key: requirement
            for requirement in row.evidence_requirements
        }

        self.assertIs(
            AutomaticDepositTransportMode.OPERATOR_MANUAL_OBSERVE_ONLY,
            row.transport_mode,
        )
        self.assertEqual("@store_home", row.expected_operator_action)
        self.assertEqual(
            "0",
            requirements["helper_submit_call_count"].expected_value,
        )
        self.assertIn("operator_action_observed", requirements)
        self.assertNotIn("automatic_trigger_observed", requirements)

    def test_every_row_requires_artifact_fixture_log_and_gameplay_evidence(self):
        for row in automatic_deposit_matrix_catalog():
            keys = {requirement.key for requirement in row.evidence_requirements}
            self.assertIn("artifact_identity_verified", keys, row.row_id)
            self.assertIn("fixture_identity_verified", keys, row.row_id)
            self.assertIn("runtime_log_complete", keys, row.row_id)
            self.assertIn("runtime_reported_completion", keys, row.row_id)
            self.assertIn("gameplay_observation_complete", keys, row.row_id)
            self.assertIn("expected_gameplay_effect_verified", keys, row.row_id)
            self.assertIn("prohibited_effect_absence_verified", keys, row.row_id)
            self.assertIn("helper_submit_call_count", keys, row.row_id)

    def test_chest_barrel_and_carry_on_boundaries_have_exact_fixtures(self):
        expected = {
            "R1a": {
                "container_type": "CHEST",
                "carry_on_state": "INSTALLED",
                "eligible_existing_container_count": "0",
                "placeable_container_available": "true",
            },
            "R1b": {
                "container_type": "CHEST",
                "carry_on_state": "ABSENT",
                "eligible_existing_container_count": "0",
                "placeable_container_available": "true",
            },
            "R2a": {
                "container_type": "BARREL",
                "carry_on_state": "INSTALLED",
                "eligible_existing_container_count": "0",
                "placeable_container_available": "true",
            },
            "R2b": {
                "container_type": "BARREL",
                "carry_on_state": "ABSENT",
                "eligible_existing_container_count": "0",
                "placeable_container_available": "true",
            },
        }

        for row in automatic_deposit_matrix_catalog():
            if row.row_id in expected:
                self.assertEqual(
                    expected[row.row_id],
                    dict(row.expected_fixture_values),
                )

    def test_transport_has_matching_trigger_evidence(self):
        for row in automatic_deposit_matrix_catalog():
            keys = {requirement.key for requirement in row.evidence_requirements}
            if row.transport_mode is (
                AutomaticDepositTransportMode.NO_COMMAND_AUTOMATIC_TRIGGER
            ):
                self.assertIn("automatic_trigger_observed", keys, row.row_id)
                self.assertNotIn("operator_action_observed", keys, row.row_id)
            elif row.transport_mode is (
                AutomaticDepositTransportMode.OPERATOR_MANUAL_OBSERVE_ONLY
            ):
                self.assertIn("operator_action_observed", keys, row.row_id)
                self.assertNotIn("automatic_trigger_observed", keys, row.row_id)


if __name__ == "__main__":
    unittest.main()
