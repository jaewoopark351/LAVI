# 20260901_kpopmodder: Require all three sealed production-runtime bindings before bundle assembly.
from __future__ import annotations

import unittest
from dataclasses import replace

from .....scenario.transport_mode import AutomaticDepositTransportMode

from ._test_fixture import sealed_inputs
from .p1_runtime_observation_bundle_contract import (
    verify_p1_runtime_observation_bundle_inputs,
)


class P1RuntimeObservationBundleContractTests(unittest.TestCase):
    def test_accepts_only_inputs_that_pass_all_three_existing_bindings(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )

        errors = verify_p1_runtime_observation_bundle_inputs(
            status,
            log_prefix,
            run_manifest,
        )

        self.assertEqual((), errors)

    def test_rejects_each_untyped_input_without_dereferencing_it(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )

        cases = (
            (
                {},
                log_prefix,
                run_manifest,
                "PRODUCTION_STATUS_OBSERVATION_NOT_TYPED",
            ),
            (
                status,
                log_prefix.runtime_artifact_observation,
                run_manifest,
                "JVM_LOG_PREFIX_OBSERVATION_NOT_TYPED",
            ),
            (
                status,
                log_prefix,
                {},
                "PRODUCTION_STATUS_RUN_MANIFEST_NOT_TYPED",
            ),
        )
        for candidate_status, candidate_prefix, candidate_run, expected in cases:
            with self.subTest(expected=expected):
                errors = verify_p1_runtime_observation_bundle_inputs(
                    candidate_status,
                    candidate_prefix,
                    candidate_run,
                )

                self.assertIn(expected, errors)

    def test_rejects_cursor_and_jvm_artifact_identity_mismatches(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )
        wrong_cursor = replace(
            run_manifest.latest_log_cursor,
            inode=run_manifest.latest_log_cursor.inode + 1,
        )
        wrong_artifact = replace(
            run_manifest.artifact_identity,
            deployed_jar_sha256="f" * 64,
        )
        changed_run = replace(
            run_manifest,
            latest_log_cursor=wrong_cursor,
            artifact_identity=wrong_artifact,
        )

        errors = verify_p1_runtime_observation_bundle_inputs(
            status,
            log_prefix,
            changed_run,
        )

        self.assertIn("JVM_LOG_PREFIX_CURSOR_INODE_MISMATCH", errors)
        self.assertIn(
            "P1_JVM_RUNTIME_CODESOURCE_SHA256_NOT_DEPLOYED_JAR",
            errors,
        )

    def test_rejects_non_p1_or_non_manual_run_identity(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )
        cases = (
            (
                replace(run_manifest, row_id="P2"),
                "P1_RUNTIME_OBSERVATION_RUN_ROW_NOT_P1",
            ),
            (
                replace(
                    run_manifest,
                    transport_mode=(
                        AutomaticDepositTransportMode.NO_COMMAND_AUTOMATIC_TRIGGER
                    ),
                ),
                "P1_RUNTIME_OBSERVATION_RUN_TRANSPORT_NOT_MANUAL",
            ),
        )

        for candidate, expected in cases:
            with self.subTest(expected=expected):
                errors = verify_p1_runtime_observation_bundle_inputs(
                    status,
                    log_prefix,
                    candidate,
                )

                self.assertIn(expected, errors)


if __name__ == "__main__":
    unittest.main()
