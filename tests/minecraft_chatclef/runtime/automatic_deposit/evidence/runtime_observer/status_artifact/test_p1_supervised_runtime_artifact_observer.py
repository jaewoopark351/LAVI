#20260901_kpopmodder: Lock the fail-closed JVM-owned runtime artifact status contract for supervised P1.
from __future__ import annotations

import unittest
from dataclasses import replace

from ..test_production_status_observer import _production_status
from ._test_fixture import status_with_p1_supervised_runtime_artifact
from .p1_supervised_runtime_artifact_observer import (
    observe_p1_supervised_runtime_artifact,
)


class P1SupervisedRuntimeArtifactObserverTests(unittest.TestCase):
    def test_current_production_status_without_runtime_artifact_fails_closed(self):
        observation, reason = observe_p1_supervised_runtime_artifact(
            _production_status()
        )

        self.assertIsNone(observation)
        self.assertEqual("P1_SUPERVISED_RUNTIME_ARTIFACT_STATUS_MISSING", reason)

    def test_observes_complete_jvm_owned_artifact_bound_to_active_session(self):
        observation, reason = observe_p1_supervised_runtime_artifact(
            status_with_p1_supervised_runtime_artifact()
        )

        self.assertEqual("P1_SUPERVISED_RUNTIME_ARTIFACT_OBSERVED", reason)
        self.assertIsNotNone(observation)
        self.assertEqual("fabric_chatclef", observation.backend_id)
        self.assertEqual("fabric", observation.loader_id)
        self.assertEqual("1.20.1", observation.minecraft_version)
        self.assertEqual(7_512_843, observation.loaded_jar_size)
        self.assertEqual("a" * 64, observation.loaded_jar_sha256)
        self.assertEqual("JVM_COMPUTED", observation.sha256_evidence_source)
        self.assertEqual("fabric-chatclef-session-1", observation.session_id)
        self.assertEqual(7, observation.connection_generation)
        with self.assertRaises(ValueError):
            replace(observation, loaded_jar_sha256="b" * 64)

    def test_rejects_non_jvm_sha_or_wrong_process_session_binding(self):
        cases = (
            (
                lambda artifact: artifact.update(
                    sha256_evidence_source="PYTHON_DISK_READ"
                ),
                "P1_SUPERVISED_RUNTIME_ARTIFACT_SHA256_SOURCE_INVALID",
            ),
            (
                lambda artifact: artifact.update(session_id="other-session"),
                "P1_SUPERVISED_RUNTIME_ARTIFACT_SESSION_MISMATCH",
            ),
            (
                lambda artifact: artifact.update(connection_generation=8),
                "P1_SUPERVISED_RUNTIME_ARTIFACT_GENERATION_MISMATCH",
            ),
            (
                lambda artifact: artifact.update(minecraft_process_id=0),
                "P1_SUPERVISED_RUNTIME_ARTIFACT_PROCESS_ID_INVALID",
            ),
        )
        for mutate, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                status = status_with_p1_supervised_runtime_artifact()
                mutate(status["details"]["runtime_artifact"])

                observation, reason = observe_p1_supervised_runtime_artifact(
                    status
                )

                self.assertIsNone(observation)
                self.assertEqual(expected_reason, reason)
if __name__ == "__main__":
    unittest.main()
