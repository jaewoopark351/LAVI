# 20260901_kpopmodder: Preserve static-manifest binding limits without claiming P1 operation correlation.
from __future__ import annotations

import unittest
from dataclasses import replace

from ._test_fixture import sealed_inputs
from .p1_runtime_observation_bundle_collector import (
    collect_p1_runtime_observation_bundle,
)


class P1RuntimeObservationBundleCollectorTests(unittest.TestCase):
    def test_collects_exact_runtime_binding_as_direct_but_not_p1_correlated(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=True,
            command_session_id="fabric-chatclef-session-1",
            command_connection_generation=7,
        )

        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )

        self.assertEqual("P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED", reason)
        self.assertIsNotNone(bundle)
        self.assertEqual("DIRECT", bundle.static_binding_classification)
        self.assertEqual(
            "RUNTIME_OBSERVATIONS_DIRECTLY_BOUND",
            bundle.static_binding_reason,
        )
        self.assertFalse(bundle.p1_operation_correlation_claimed)
        self.assertEqual(status.observation_fingerprint, bundle.status_fingerprint)
        self.assertEqual(
            log_prefix.observation_fingerprint,
            bundle.log_prefix_fingerprint,
        )
        self.assertEqual(
            log_prefix.runtime_artifact_observation.observation_fingerprint,
            bundle.raw_artifact_fingerprint,
        )
        self.assertEqual(
            "jvm-static-manifest-1",
            bundle.raw_run_manifest_id,
        )
        self.assertRegex(bundle.binding_fingerprint, r"\A[0-9a-f]{64}\Z")
        self.assertEqual(
            run_manifest.run_id,
            bundle.harness_run_binding.run_id,
        )
        self.assertEqual(
            run_manifest.operation_id,
            bundle.harness_run_binding.operation_id,
        )
        self.assertRegex(
            bundle.harness_run_binding_fingerprint,
            r"\A[0-9a-f]{64}\Z",
        )
        self.assertRegex(bundle.bundle_fingerprint, r"\A[0-9a-f]{64}\Z")

    def test_preserves_manual_command_context_unavailable_limitation(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )

        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )

        self.assertEqual("P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED", reason)
        self.assertIsNotNone(bundle)
        self.assertEqual(
            "COMMAND_CONTEXT_UNAVAILABLE",
            bundle.static_binding_classification,
        )
        self.assertEqual(
            "MANIFEST_COMMAND_CONTEXT_UNAVAILABLE",
            bundle.static_binding_reason,
        )
        self.assertFalse(bundle.p1_operation_correlation_claimed)

    def test_preserves_emit_once_manifest_as_historical_session_limitation(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=True,
            command_session_id="past-fabric-session",
            command_connection_generation=7,
        )

        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )

        self.assertEqual("P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED", reason)
        self.assertIsNotNone(bundle)
        self.assertEqual("HISTORICAL_SESSION", bundle.static_binding_classification)
        self.assertEqual(
            "RUNTIME_OBSERVATION_SESSION_BINDING_MISMATCH",
            bundle.static_binding_reason,
        )
        self.assertFalse(bundle.p1_operation_correlation_claimed)

    def test_preserves_emit_once_manifest_as_historical_generation_limitation(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=True,
            command_session_id="fabric-chatclef-session-1",
            command_connection_generation=6,
        )

        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )

        self.assertEqual("P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED", reason)
        self.assertIsNotNone(bundle)
        self.assertEqual(
            "HISTORICAL_GENERATION",
            bundle.static_binding_classification,
        )
        self.assertEqual(
            "RUNTIME_OBSERVATION_GENERATION_BINDING_MISMATCH",
            bundle.static_binding_reason,
        )
        self.assertFalse(bundle.p1_operation_correlation_claimed)

    def test_rejects_untyped_inputs_and_seals_bundle_integrity(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )

        missing, failure_reason = collect_p1_runtime_observation_bundle(
            status,
            {},
            run_manifest,
        )
        bundle, _ = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )

        self.assertIsNone(missing)
        self.assertEqual(
            "P1_RUNTIME_OBSERVATION_INPUTS_INVALID:JVM_LOG_PREFIX_OBSERVATION_NOT_TYPED",
            failure_reason,
        )
        self.assertIsNotNone(bundle)
        with self.assertRaises(ValueError):
            replace(bundle, raw_run_manifest_id="fabricated-manifest-id")

    def test_rejects_a_sealed_but_contradictory_command_context(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
            command_session_id="unexpected-session",
            command_connection_generation=7,
        )

        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            run_manifest,
        )

        self.assertIsNone(bundle)
        self.assertEqual(
            "P1_RUNTIME_OBSERVATION_INPUTS_INVALID:"
            "P1_STATIC_MANIFEST_COMMAND_CONTEXT_CONTRADICTORY",
            reason,
        )

    def test_rejects_unfingerprintable_harness_run_without_raising(self):
        status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )

        bundle, reason = collect_p1_runtime_observation_bundle(
            status,
            log_prefix,
            replace(run_manifest, created_at_utc={}),
        )

        self.assertIsNone(bundle)
        self.assertEqual(
            "P1_RUNTIME_OBSERVATION_INPUTS_INVALID:"
            "P1_RUNTIME_OBSERVATION_HARNESS_RUN_FINGERPRINT_UNAVAILABLE",
            reason,
        )


if __name__ == "__main__":
    unittest.main()
