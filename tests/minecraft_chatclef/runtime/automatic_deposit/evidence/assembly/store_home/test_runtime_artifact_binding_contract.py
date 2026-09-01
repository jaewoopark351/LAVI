# 20260901_kpopmodder: Keep JVM CodeSource binding total for malformed run artifacts.
from __future__ import annotations

import unittest
from dataclasses import replace

from .runtime_artifact_binding_contract import (
    verify_p1_jvm_runtime_artifact_binding,
)
from .runtime_observation._test_fixture import sealed_inputs


class RuntimeArtifactBindingContractTests(unittest.TestCase):
    def test_rejects_malformed_run_artifact_fields_without_raising(self):
        _status, log_prefix, run_manifest = sealed_inputs(
            command_context_available=False,
        )
        raw = log_prefix.runtime_artifact_observation
        artifact = run_manifest.artifact_identity
        cases = (
            (
                replace(artifact, deployed_jar_path={}),
                "P1_RUN_ARTIFACT_DEPLOYED_PATH_INVALID",
            ),
            (
                replace(artifact, loaded_code_source={}),
                "P1_RUN_ARTIFACT_LOADED_CODESOURCE_INVALID",
            ),
            (
                replace(artifact, deployed_jar_sha256={}),
                "P1_RUN_ARTIFACT_DEPLOYED_SHA256_INVALID",
            ),
            (
                replace(artifact, source_jar_sha256={}),
                "P1_RUN_ARTIFACT_SOURCE_SHA256_INVALID",
            ),
        )

        for candidate, expected in cases:
            with self.subTest(expected=expected):
                errors = verify_p1_jvm_runtime_artifact_binding(
                    raw,
                    replace(run_manifest, artifact_identity=candidate),
                )

                self.assertIn(expected, errors)


if __name__ == "__main__":
    unittest.main()
