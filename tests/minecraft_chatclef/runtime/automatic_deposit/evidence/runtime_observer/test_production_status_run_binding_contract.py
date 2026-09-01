# 20260901_kpopmodder: Lock exact canonical backend binding between production status and one run manifest.
from __future__ import annotations

import unittest
from dataclasses import replace

from ...testing.hermetic_evidence import (
    hermetic_fixture,
    hermetic_run_manifest,
)
from ...scenario.matrix_catalog import automatic_deposit_matrix_catalog
from .production_status_observer import observe_production_fabric_status
from .production_status_run_binding_contract import (
    verify_production_status_run_binding,
)
from .test_production_status_observer import _production_status


class ProductionStatusRunBindingContractTests(unittest.TestCase):
    def setUp(self) -> None:
        self.row = next(
            row
            for row in automatic_deposit_matrix_catalog()
            if row.row_id == "P1"
        )
        self.manifest = hermetic_run_manifest(
            self.row,
            hermetic_fixture(self.row),
        )
        self.status, reason = observe_production_fabric_status(
            _production_status()
        )
        self.assertEqual("PRODUCTION_FABRIC_STATUS_OBSERVED", reason)

    def test_binds_sealed_status_to_typed_manifest_only_by_exact_backend(self):
        errors = verify_production_status_run_binding(
            self.status,
            self.manifest,
        )

        self.assertEqual((), errors)
        self.assertEqual("fabric_chatclef", self.status.backend_id)
        self.assertEqual(self.status.backend_id, self.manifest.backend)

    def test_rejects_legacy_manifest_backend_alias(self):
        legacy_manifest = replace(
            self.manifest,
            backend="fabric_chatclef_1.20.1",
        )

        errors = verify_production_status_run_binding(
            self.status,
            legacy_manifest,
        )

        self.assertIn("RUN_MANIFEST_BACKEND_NOT_CANONICAL", errors)
        self.assertIn("PRODUCTION_STATUS_RUN_BACKEND_MISMATCH", errors)

    def test_rejects_unsealed_status_and_untyped_manifest(self):
        status_errors = verify_production_status_run_binding(
            _production_status(),
            self.manifest,
        )
        manifest_errors = verify_production_status_run_binding(
            self.status,
            {"backend": "fabric_chatclef"},
        )

        self.assertEqual(
            ("PRODUCTION_STATUS_OBSERVATION_NOT_TYPED",),
            status_errors,
        )
        self.assertEqual(
            ("PRODUCTION_STATUS_RUN_MANIFEST_NOT_TYPED",),
            manifest_errors,
        )


if __name__ == "__main__":
    unittest.main()
