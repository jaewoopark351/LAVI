# 20260901_kpopmodder: Lock log-prefix evidence to the exact cursor sealed by one run manifest.
from __future__ import annotations

import unittest
from dataclasses import replace

from ...latest_log_byte_cursor import (
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ....scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ....testing.hermetic_evidence import hermetic_fixture, hermetic_run_manifest
from .jvm_runtime_artifact_log_prefix_binding_contract import (
    verify_jvm_runtime_artifact_log_prefix_binding,
)
from .test_jvm_runtime_artifact_log_prefix_observer import (
    JvmRuntimeArtifactLogPrefixObserverTests,
)


class JvmRuntimeArtifactLogPrefixBindingContractTests(unittest.TestCase):
    def setUp(self) -> None:
        observer_case = JvmRuntimeArtifactLogPrefixObserverTests(methodName="runTest")
        observer_case.setUp()
        self.observation, reason = observer_case._observe(observer_case.prefix)
        self.assertEqual("JVM_RUNTIME_ARTIFACT_LOG_PREFIX_OBSERVED", reason)
        row = next(
            row
            for row in automatic_deposit_matrix_catalog()
            if row.row_id == "P1"
        )
        base_manifest = hermetic_run_manifest(row, hermetic_fixture(row))
        self.manifest = replace(
            base_manifest,
            latest_log_path=observer_case.cursor.path,
            latest_log_cursor=observer_case.cursor,
        )

    def test_accepts_exact_wrapper_to_manifest_cursor_binding(self):
        errors = verify_jvm_runtime_artifact_log_prefix_binding(
            self.observation,
            self.manifest,
        )

        self.assertEqual((), errors)
        self.assertEqual(
            automatic_deposit_latest_log_cursor_fingerprint(
                self.manifest.latest_log_cursor
            ),
            self.observation.source_cursor_fingerprint,
        )

    def test_rejects_raw_artifact_or_untyped_manifest(self):
        raw_errors = verify_jvm_runtime_artifact_log_prefix_binding(
            self.observation.runtime_artifact_observation,
            self.manifest,
        )
        manifest_errors = verify_jvm_runtime_artifact_log_prefix_binding(
            self.observation,
            {"latest_log_cursor": self.manifest.latest_log_cursor},
        )

        self.assertEqual(
            ("JVM_LOG_PREFIX_OBSERVATION_NOT_TYPED",),
            raw_errors,
        )
        self.assertEqual(
            ("JVM_LOG_PREFIX_RUN_MANIFEST_NOT_TYPED",),
            manifest_errors,
        )

    def test_rejects_any_different_manifest_cursor_identity(self):
        changed_cursor = replace(
            self.manifest.latest_log_cursor,
            inode=self.manifest.latest_log_cursor.inode + 1,
        )
        changed_manifest = replace(
            self.manifest,
            latest_log_cursor=changed_cursor,
        )

        errors = verify_jvm_runtime_artifact_log_prefix_binding(
            self.observation,
            changed_manifest,
        )

        self.assertIn("JVM_LOG_PREFIX_CURSOR_FINGERPRINT_MISMATCH", errors)
        self.assertIn("JVM_LOG_PREFIX_CURSOR_INODE_MISMATCH", errors)

    def test_rejects_malformed_run_paths_without_raising(self):
        cases = (
            (
                replace(self.manifest, latest_log_path={}),
                "JVM_LOG_PREFIX_RUN_LATEST_LOG_PATH_INVALID",
            ),
            (
                replace(
                    self.manifest,
                    latest_log_cursor=replace(
                        self.manifest.latest_log_cursor,
                        path={},
                    ),
                ),
                "JVM_LOG_PREFIX_RUN_CURSOR_PATH_INVALID",
            ),
            (
                replace(
                    self.manifest,
                    latest_log_cursor=replace(
                        self.manifest.latest_log_cursor,
                        prefix_sha256={},
                    ),
                ),
                "JVM_LOG_PREFIX_RUN_CURSOR_SHA256_INVALID",
            ),
        )

        for manifest, expected in cases:
            with self.subTest(expected=expected):
                errors = verify_jvm_runtime_artifact_log_prefix_binding(
                    self.observation,
                    manifest,
                )

                self.assertIn(expected, errors)


if __name__ == "__main__":
    unittest.main()
