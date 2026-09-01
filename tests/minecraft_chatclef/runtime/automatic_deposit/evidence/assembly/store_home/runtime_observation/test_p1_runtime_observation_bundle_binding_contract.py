# 20260901_kpopmodder: Rebind a sealed runtime bundle to the exact consuming run.
from __future__ import annotations

import unittest
from dataclasses import replace

from ._test_fixture import sealed_inputs
from .p1_runtime_observation_bundle_binding_contract import (
    verify_p1_runtime_observation_bundle_binding,
)
from .p1_runtime_observation_bundle_collector import (
    collect_p1_runtime_observation_bundle,
)


class P1RuntimeObservationBundleBindingContractTests(unittest.TestCase):
    def test_accepts_bundle_collected_for_the_exact_run(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )
        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )
        self.assertEqual("P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED", reason)

        errors = verify_p1_runtime_observation_bundle_binding(
            bundle,
            run_manifest,
        )

        self.assertEqual((), errors)

    def test_rejects_untyped_bundle_before_dereferencing_it(self):
        _status, _log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )

        errors = verify_p1_runtime_observation_bundle_binding({}, run_manifest)

        self.assertEqual(("P1_RUNTIME_OBSERVATION_BUNDLE_NOT_TYPED",), errors)

    def test_rejects_bundle_collected_against_a_different_log_cursor(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )
        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )
        self.assertEqual("P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED", reason)
        changed_cursor = replace(
            run_manifest.latest_log_cursor,
            inode=run_manifest.latest_log_cursor.inode + 1,
        )

        errors = verify_p1_runtime_observation_bundle_binding(
            bundle,
            replace(run_manifest, latest_log_cursor=changed_cursor),
        )

        self.assertIn("JVM_LOG_PREFIX_CURSOR_INODE_MISMATCH", errors)

    def test_rejects_different_harness_run_or_operation_identity(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )
        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )
        self.assertEqual("P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED", reason)
        cases = (
            (
                replace(run_manifest, run_id="another-run"),
                "P1_RUNTIME_OBSERVATION_HARNESS_RUN_ID_MISMATCH",
            ),
            (
                replace(run_manifest, operation_id="another-operation"),
                "P1_RUNTIME_OBSERVATION_HARNESS_OPERATION_ID_MISMATCH",
            ),
        )

        for candidate, expected in cases:
            with self.subTest(expected=expected):
                errors = verify_p1_runtime_observation_bundle_binding(
                    bundle,
                    candidate,
                )

                self.assertIn(expected, errors)

    def test_rejects_untyped_transport_without_dereferencing_it(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )
        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )
        self.assertEqual("P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED", reason)

        errors = verify_p1_runtime_observation_bundle_binding(
            bundle,
            replace(
                run_manifest,
                transport_mode="OPERATOR_MANUAL_OBSERVE_ONLY",
            ),
        )

        self.assertIn(
            "P1_RUNTIME_OBSERVATION_RUN_TRANSPORT_NOT_MANUAL",
            errors,
        )

    def test_rejects_unfingerprintable_nested_run_value_without_raising(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )
        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )
        self.assertEqual("P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED", reason)

        errors = verify_p1_runtime_observation_bundle_binding(
            bundle,
            replace(run_manifest, artifact_identity={}),
        )

        self.assertIn("P1_RUN_ARTIFACT_IDENTITY_NOT_TYPED", errors)
        self.assertIn(
            "P1_RUNTIME_OBSERVATION_HARNESS_RUN_FINGERPRINT_UNAVAILABLE",
            errors,
        )

    def test_rejects_malformed_cursor_fingerprint_fields_without_raising(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )
        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )
        self.assertEqual("P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED", reason)
        cases = (
            replace(run_manifest.latest_log_cursor, path={}),
            replace(run_manifest.latest_log_cursor, prefix_sha256={}),
        )

        for cursor in cases:
            with self.subTest(cursor=cursor):
                errors = verify_p1_runtime_observation_bundle_binding(
                    bundle,
                    replace(run_manifest, latest_log_cursor=cursor),
                )

                self.assertIn(
                    "P1_RUNTIME_OBSERVATION_HARNESS_CURSOR_FINGERPRINT_UNAVAILABLE",
                    errors,
                )


if __name__ == "__main__":
    unittest.main()
