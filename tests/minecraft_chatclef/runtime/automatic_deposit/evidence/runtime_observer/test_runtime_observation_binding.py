#20260901_kpopmodder: Preserve honest status-to-manifest binding and its manual-command limitation.
from __future__ import annotations

import unittest
from pathlib import Path

from ..evidence_test_fixture import HermeticEvidenceFileSystem
from .jvm_runtime_artifact_observer import observe_jvm_runtime_artifact
from .production_status_observer import observe_production_fabric_status
from .runtime_observation_binder import bind_runtime_observations
from .test_jvm_runtime_artifact_observer import _manifest_line, _scan
from .test_production_status_observer import _production_status


class RuntimeObservationBindingTests(unittest.TestCase):
    def setUp(self) -> None:
        self.jar_path = Path("C:/minecraft/instance/mods/chatclef.jar")
        self.file_system = HermeticEvidenceFileSystem(
            Path("C:/minecraft/instance"),
            Path("C:/minecraft/instance/mods"),
        )
        self.file_system.add_file(self.jar_path, b"chatclef")
        self.status, status_reason = observe_production_fabric_status(
            _production_status()
        )
        self.assertEqual("PRODUCTION_FABRIC_STATUS_OBSERVED", status_reason)

    def test_preserves_manual_context_as_not_directly_bound(self):
        artifact = self._artifact(_manifest_line(self.jar_path.as_uri()))

        binding, reason = bind_runtime_observations(self.status, artifact)

        self.assertEqual("RUNTIME_OBSERVATIONS_NOT_DIRECTLY_BOUND", reason)
        self.assertIsNotNone(binding)
        self.assertFalse(binding.directly_bound)
        self.assertEqual(
            "MANIFEST_COMMAND_CONTEXT_UNAVAILABLE",
            binding.limitation_reason,
        )

    def test_directly_binds_only_matching_real_session_and_generation(self):
        line = _manifest_line(self.jar_path.as_uri()).replace(
            " commandContextAvailable=false commandRequestId=~EMPTY~ "
            "commandSessionId=~EMPTY~ commandConnectionGeneration=unavailable",
            " commandContextAvailable=true "
            "commandSessionId=fabric-chatclef-session-1 "
            "commandConnectionGeneration=7",
        )
        artifact = self._artifact(line)

        binding, reason = bind_runtime_observations(self.status, artifact)

        self.assertEqual("RUNTIME_OBSERVATIONS_DIRECTLY_BOUND", reason)
        self.assertIsNotNone(binding)
        self.assertTrue(binding.directly_bound)
        self.assertEqual("", binding.limitation_reason)

    def test_rejects_a_claimed_context_that_does_not_match_status(self):
        line = _manifest_line(self.jar_path.as_uri()).replace(
            " commandContextAvailable=false commandRequestId=~EMPTY~ "
            "commandSessionId=~EMPTY~ commandConnectionGeneration=unavailable",
            " commandContextAvailable=true commandSessionId=another-session "
            "commandConnectionGeneration=7",
        )
        artifact = self._artifact(line)

        binding, reason = bind_runtime_observations(self.status, artifact)

        self.assertIsNone(binding)
        self.assertEqual("RUNTIME_OBSERVATION_SESSION_BINDING_MISMATCH", reason)

    def _artifact(self, line: str):
        artifact, reason = observe_jvm_runtime_artifact(
            _scan(line),
            bytes_reader=self.file_system.read_bytes,
            stat_reader=self.file_system.read_stat,
        )
        self.assertEqual("JVM_RUNTIME_ARTIFACT_OBSERVED", reason)
        return artifact


if __name__ == "__main__":
    unittest.main()
