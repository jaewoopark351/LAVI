#20260901_kpopmodder: Lock independent SHA evidence for one exact JVM CodeSource manifest.
from __future__ import annotations

import hashlib
import unittest
from dataclasses import replace
from pathlib import Path

from ..evidence_test_fixture import HermeticEvidenceFileSystem
from ...oracle.runtime_log.production_log_delta_scanner import (
    scan_production_diagnostic_delta,
)
from ..latest_log_delta_result import _create_latest_log_delta_result
from .jvm_runtime_artifact_observer import observe_jvm_runtime_artifact


class JvmRuntimeArtifactObserverTests(unittest.TestCase):
    def setUp(self) -> None:
        self.instance_root = Path("C:/minecraft/instance")
        self.mods_directory = self.instance_root / "mods"
        self.jar_path = self.mods_directory / "chatclef.jar"
        self.jar_payload = b"fresh-chatclef-runtime-jar"
        self.file_system = HermeticEvidenceFileSystem(
            self.instance_root,
            self.mods_directory,
        )
        self.file_system.add_file(self.jar_path, self.jar_payload)

    def test_observes_exact_codesource_sha_and_windows_100ns_mtime(self):
        scan = _scan(_manifest_line(self.jar_path.as_uri()))

        observation, reason = observe_jvm_runtime_artifact(
            scan,
            bytes_reader=self.file_system.read_bytes,
            stat_reader=self.file_system.read_stat,
        )

        self.assertEqual("JVM_RUNTIME_ARTIFACT_OBSERVED", reason)
        self.assertIsNotNone(observation)
        self.assertEqual("manifest-runtime-1", observation.run_manifest_id)
        self.assertEqual(
            str(self.jar_path.resolve(strict=False)).casefold(),
            observation.runtime_code_source_path.casefold(),
        )
        self.assertEqual(
            hashlib.sha256(self.jar_payload).hexdigest(),
            observation.runtime_code_source_sha256,
        )
        self.assertEqual(
            "INDEPENDENT_STABLE_CODESOURCE_FILE_READ",
            observation.sha256_evidence_source,
        )
        self.assertEqual(200, observation.runtime_code_source_mtime_ns)
        self.assertFalse(observation.command_context_available)
        self.assertEqual("", observation.command_session_id)
        self.assertIsNone(observation.command_connection_generation)
        self.assertFalse(hasattr(observation, "command_request_id"))
        with self.assertRaises(ValueError):
            replace(observation, runtime_code_source_sha256="f" * 64)

    def test_rejects_zero_or_multiple_manifest_records(self):
        no_manifest = _scan(_store_home_start_line())
        duplicate = _scan(
            _manifest_line(self.jar_path.as_uri())
            + "\n"
            + _manifest_line(self.jar_path.as_uri(), sequence=11)
        )

        for scan, expected_reason in (
            (no_manifest, "JVM_RUN_MANIFEST_RECORD_MISSING"),
            (duplicate, "JVM_RUN_MANIFEST_RECORD_NOT_EXCLUSIVE"),
        ):
            with self.subTest(expected_reason=expected_reason):
                observation, reason = observe_jvm_runtime_artifact(
                    scan,
                    bytes_reader=self.file_system.read_bytes,
                    stat_reader=self.file_system.read_stat,
                )

                self.assertIsNone(observation)
                self.assertEqual(expected_reason, reason)

    def test_rejects_manifest_codesource_alias_or_reported_sha_mismatch(self):
        alias_mismatch = _manifest_line(self.jar_path.as_uri()).replace(
            f"runtimeCodeSourcePath={_encoded(self.jar_path.as_uri())}",
            "runtimeCodeSourcePath=file%3A%2F%2FC%3A%2Fanother.jar",
        )
        reported_sha_mismatch = _manifest_line(
            self.jar_path.as_uri(),
            reported_runtime_sha="f" * 64,
        )

        cases = (
            (alias_mismatch, "JVM_RUNTIME_CODE_SOURCE_FIELDS_MISMATCH"),
            (reported_sha_mismatch, "JVM_REPORTED_RUNTIME_SHA256_MISMATCH"),
        )
        for line, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                observation, reason = observe_jvm_runtime_artifact(
                    _scan(line),
                    bytes_reader=self.file_system.read_bytes,
                    stat_reader=self.file_system.read_stat,
                )

                self.assertIsNone(observation)
                self.assertEqual(expected_reason, reason)

    def test_fails_closed_when_exact_codesource_changes_during_hashing(self):
        self.file_system.make_unstable(self.jar_path)

        observation, reason = observe_jvm_runtime_artifact(
            _scan(_manifest_line(self.jar_path.as_uri())),
            bytes_reader=self.file_system.read_bytes,
            stat_reader=self.file_system.read_stat,
        )

        self.assertIsNone(observation)
        self.assertEqual(
            "JVM_RUNTIME_CODE_SOURCE_STABLE_FILE_CHANGED_DURING_READ",
            reason,
        )

    def test_rejects_manifest_last_modified_that_does_not_match_stable_stat(self):
        line = _manifest_line(
            self.jar_path.as_uri(),
            runtime_last_modified="1970-01-01T00:00:00.000000201Z",
        )

        observation, reason = observe_jvm_runtime_artifact(
            _scan(line),
            bytes_reader=self.file_system.read_bytes,
            stat_reader=self.file_system.read_stat,
        )

        self.assertIsNone(observation)
        self.assertEqual(
            "JVM_RUNTIME_CODE_SOURCE_LAST_MODIFIED_MISMATCH",
            reason,
        )

    def test_rejects_non_canonical_manifest_last_modified(self):
        line = _manifest_line(
            self.jar_path.as_uri(),
            runtime_last_modified="1970-01-01T00:00:00+00:00",
        )

        observation, reason = observe_jvm_runtime_artifact(
            _scan(line),
            bytes_reader=self.file_system.read_bytes,
            stat_reader=self.file_system.read_stat,
        )

        self.assertIsNone(observation)
        self.assertEqual(
            "JVM_RUNTIME_CODE_SOURCE_LAST_MODIFIED_INVALID",
            reason,
        )

    def test_preserves_optional_real_command_context_without_requiring_it(self):
        line = _manifest_line(self.jar_path.as_uri()).replace(
            " commandContextAvailable=false commandRequestId=~EMPTY~ "
            "commandSessionId=~EMPTY~ commandConnectionGeneration=unavailable",
            " commandContextAvailable=true commandSessionId=session-live "
            "commandConnectionGeneration=7",
        )

        observation, reason = observe_jvm_runtime_artifact(
            _scan(line),
            bytes_reader=self.file_system.read_bytes,
            stat_reader=self.file_system.read_stat,
        )

        self.assertEqual("JVM_RUNTIME_ARTIFACT_OBSERVED", reason)
        self.assertIsNotNone(observation)
        self.assertTrue(observation.command_context_available)
        self.assertEqual("session-live", observation.command_session_id)
        self.assertEqual(7, observation.command_connection_generation)


def _scan(*lines: str):
    text = "\n".join(lines) + "\n"
    delta = _create_latest_log_delta_result(
        True,
        "LATEST_LOG_DELTA_READ",
        None,
        100,
        100 + len(text.encode("utf-8")),
        text,
        "utf-8",
        "a" * 64,
    )
    scan = scan_production_diagnostic_delta(delta)
    if not scan.ok:
        raise AssertionError(f"test fixture did not scan: {scan.reason}:{scan.failure_detail}")
    return scan


def _manifest_line(
    code_source_uri: str,
    *,
    sequence: int = 10,
    reported_runtime_sha: str = "UNVERIFIED",
    runtime_last_modified: str = "1970-01-01T00:00:00.0000002Z",
) -> str:
    encoded_uri = _encoded(code_source_uri)
    identity = f"{code_source_uri}|lastModified={runtime_last_modified}"
    encoded_identity = _encoded(identity)
    return (
        "[01:04:05] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefBoundary] traceId=trace-runtime clientTickId=812 "
        f"eventSequence={sequence} taskInstanceId=task-1 taskRunId=run-1 "
        "parentTaskRunId=unavailable threadName=Render%20thread level=BOUNDARY "
        "event=STORE_HOME_RUN_MANIFEST reason=store_home_controlled_run_manifest "
        "taskClass=lavi.StoreHomeTask runId=manifest-runtime-1 "
        "runManifestIdSource=RUNTIME_GENERATED "
        f"runtimeCodeSource={encoded_uri} runtimeCodeSourcePath={encoded_uri} "
        f"runtimeCodeSourceLastModified={_encoded(runtime_last_modified)} "
        f"runtimeCodeSourceIdentity={encoded_identity} "
        f"runtimeJarSha256={reported_runtime_sha} "
        "runtimeJarSha256EvidenceSource="
        "EXTERNAL_SYSTEM_PROPERTY_NOT_INDEPENDENTLY_VERIFIED "
        "minecraftVersion=1.20.1 fabricLoaderVersion=0.19.3 "
        "chatClefVersion=1.20.1-0.18.23 commandContextAvailable=false "
        "commandRequestId=~EMPTY~ commandSessionId=~EMPTY~ "
        "commandConnectionGeneration=unavailable "
        "diagnosticCaptureStatus=complete"
    )


def _encoded(value: str) -> str:
    return (
        value.replace("%", "%25")
        .replace(":", "%3A")
        .replace("=", "%3D")
        .replace("|", "%7C")
        .replace(" ", "%20")
    )


def _store_home_start_line() -> str:
    return (
        "[01:04:05] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefBoundary] traceId=trace-runtime clientTickId=812 "
        "eventSequence=9 taskInstanceId=task-1 taskRunId=run-1 "
        "parentTaskRunId=unavailable threadName=Render%20thread level=BOUNDARY "
        "event=STORE_HOME_OPERATION_STARTED reason=runtime_test "
        "taskClass=lavi.StoreHomeTask diagnosticCaptureStatus=complete"
    )


if __name__ == "__main__":
    unittest.main()
