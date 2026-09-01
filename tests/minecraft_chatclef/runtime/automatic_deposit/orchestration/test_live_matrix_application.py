#20260831_kpopmodder: Lock the live entry boundary to INCONCLUSIVE and submit zero.
from __future__ import annotations

import unittest
from dataclasses import replace

from ..scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ..testing.hermetic_evidence import hermetic_fixture, hermetic_run_manifest
from .live_matrix_application import (
    run_automatic_deposit_live_matrix_application,
)


class AutomaticDepositLiveMatrixApplicationTests(unittest.TestCase):
    def test_missing_opt_in_is_inconclusive_without_submit(self):
        result = run_automatic_deposit_live_matrix_application(
            {"live_opt_in": False, "mutating_opt_in": False},
            row_id="R3",
        )

        self.assertEqual("INCONCLUSIVE", result["verdict"])
        self.assertEqual(0, result["submit_call_count"])

    def test_missing_manifests_are_inconclusive_without_submit(self):
        result = run_automatic_deposit_live_matrix_application(
            {"live_opt_in": True, "mutating_opt_in": True},
            row_id="R3",
        )

        self.assertEqual("RUN_OR_FIXTURE_MANIFEST_MISSING", result["reason"])
        self.assertEqual(0, result["submit_call_count"])

    def test_untyped_manifests_are_inconclusive_without_submit(self):
        result = run_automatic_deposit_live_matrix_application(
            {"live_opt_in": True, "mutating_opt_in": True},
            row_id="R3",
            run_manifest={},
            fixture_manifest={},
        )

        self.assertEqual("RUN_MANIFEST_NOT_TYPED", result["reason"])
        self.assertEqual(0, result["submit_call_count"])

    def test_even_verified_manifests_have_no_live_transport_or_mutation(self):
        row = next(
            row
            for row in automatic_deposit_matrix_catalog()
            if row.row_id == "R3"
        )
        fixture = hermetic_fixture(row)
        run = hermetic_run_manifest(row, fixture)

        result = run_automatic_deposit_live_matrix_application(
            {"live_opt_in": True, "mutating_opt_in": True},
            row_id="R3",
            run_manifest=run,
            fixture_manifest=fixture,
        )

        self.assertEqual(
            "LIVE_TRANSPORT_AND_RUNTIME_OBSERVER_NOT_CONFIGURED",
            result["reason"],
        )
        self.assertEqual("INCONCLUSIVE", result["verdict"])
        self.assertEqual(0, result["submit_call_count"])

    def test_diagnostics_mode_mismatch_stops_before_live_wiring(self):
        row = next(
            row
            for row in automatic_deposit_matrix_catalog()
            if row.row_id == "R3"
        )
        fixture = hermetic_fixture(row)
        run = replace(hermetic_run_manifest(row, fixture), diagnostics_mode="OFF")

        result = run_automatic_deposit_live_matrix_application(
            {"live_opt_in": True, "mutating_opt_in": True},
            row_id="R3",
            run_manifest=run,
            fixture_manifest=fixture,
        )

        self.assertEqual(
            "RUN_AND_FIXTURE_DIAGNOSTICS_MODE_MISMATCH",
            result["reason"],
        )
        self.assertEqual(0, result["submit_call_count"])


if __name__ == "__main__":
    unittest.main()
