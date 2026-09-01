# 20260901_kpopmodder: Lock JVM artifact provenance to the sealed pre-action latest.log prefix.
from __future__ import annotations

import hashlib
import unittest
from dataclasses import replace
from pathlib import Path
from types import SimpleNamespace

from ...evidence_test_fixture import HermeticEvidenceFileSystem
from ...latest_log_byte_cursor import (
    AutomaticDepositLatestLogByteCursor,
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ..test_jvm_runtime_artifact_observer import _manifest_line
from .jvm_runtime_artifact_log_prefix_observer import (
    observe_jvm_runtime_artifact_from_log_prefix,
)


class JvmRuntimeArtifactLogPrefixObserverTests(unittest.TestCase):
    def setUp(self) -> None:
        self.instance_root = Path("C:/minecraft/instance")
        self.log_path = self.instance_root / "logs" / "latest.log"
        self.jar_path = self.instance_root / "mods" / "chatclef.jar"
        self.jar_payload = b"fresh-chatclef-runtime-jar"
        self.artifact_files = HermeticEvidenceFileSystem(
            self.instance_root,
            self.instance_root / "mods",
        )
        self.artifact_files.add_file(self.jar_path, self.jar_payload)
        self.prefix = (_manifest_line(self.jar_path.as_uri()) + "\n").encode()
        self.post_action = b"post-action line that must not be scanned\n"
        self.cursor = _cursor(self.log_path, self.prefix)
        self.prefix_read_requests: list[tuple[Path, int]] = []

    def test_observes_only_exact_sealed_prefix_and_preserves_provenance(self):
        observation, reason = self._observe(self.prefix + self.post_action)

        self.assertEqual("JVM_RUNTIME_ARTIFACT_LOG_PREFIX_OBSERVED", reason)
        self.assertIsNotNone(observation)
        self.assertEqual(
            str(self.log_path.resolve(strict=False)),
            observation.source_log_path,
        )
        self.assertEqual(
            automatic_deposit_latest_log_cursor_fingerprint(self.cursor),
            observation.source_cursor_fingerprint,
        )
        self.assertEqual(self.cursor.prefix_sha256, observation.source_prefix_sha256)
        self.assertEqual(self.cursor.size, observation.source_prefix_size)
        self.assertEqual(self.cursor.device, observation.source_device)
        self.assertEqual(self.cursor.inode, observation.source_inode)
        self.assertEqual(10, observation.manifest_record_sequence)
        self.assertEqual(
            observation.manifest_record_sequence,
            observation.runtime_artifact_observation.manifest_event_sequence,
        )
        self.assertEqual(
            [(self.log_path.resolve(strict=False), self.cursor.size)],
            self.prefix_read_requests,
        )
        with self.assertRaisesRegex(ValueError, "integrity mismatch"):
            replace(observation, source_prefix_size=self.cursor.size + 1)

    def test_does_not_scan_a_manifest_appended_after_the_cursor(self):
        pre_action = b"ordinary startup line\n"
        self.cursor = _cursor(self.log_path, pre_action)
        full_log = pre_action + self.prefix

        observation, reason = self._observe(full_log)

        self.assertIsNone(observation)
        self.assertEqual(
            "JVM_LOG_PREFIX_ARTIFACT_NOT_OBSERVED:"
            "JVM_RUN_MANIFEST_RECORD_MISSING",
            reason,
        )
        self.assertEqual(
            [(self.log_path.resolve(strict=False), len(pre_action))],
            self.prefix_read_requests,
        )

    def test_unrelated_partial_store_home_record_does_not_contaminate_manifest(self):
        unrelated_partial = (
            _store_home_candidate_line(capture_status="partial") + "\n"
        ).encode()
        prefix = self.prefix + unrelated_partial
        self.cursor = _cursor(self.log_path, prefix)

        observation, reason = self._observe(prefix)

        self.assertEqual("JVM_RUNTIME_ARTIFACT_LOG_PREFIX_OBSERVED", reason)
        self.assertIsNotNone(observation)
        self.assertEqual(10, observation.manifest_record_sequence)

    def test_rejects_non_typed_cursor_without_reading(self):
        observation, reason = observe_jvm_runtime_artifact_from_log_prefix(
            {"path": str(self.log_path)},
            prefix_reader=lambda _path, _size: self.prefix,
            log_stat_reader=lambda _path: _stat(len(self.prefix)),
            artifact_bytes_reader=self.artifact_files.read_bytes,
            artifact_stat_reader=self.artifact_files.read_stat,
        )

        self.assertIsNone(observation)
        self.assertEqual("JVM_LOG_PREFIX_CURSOR_NOT_TYPED", reason)

    def test_rejects_prefix_identity_or_bounds_mismatch(self):
        cases = (
            (
                replace(self.cursor, device=self.cursor.device + 1),
                self.prefix,
                _stat(len(self.prefix)),
                "JVM_LOG_PREFIX_DEVICE_MISMATCH",
            ),
            (
                replace(self.cursor, inode=self.cursor.inode + 1),
                self.prefix,
                _stat(len(self.prefix)),
                "JVM_LOG_PREFIX_INODE_MISMATCH",
            ),
            (
                self.cursor,
                self.prefix,
                _stat(self.cursor.size - 1),
                "JVM_LOG_PREFIX_TRUNCATED",
            ),
            (
                replace(self.cursor, prefix_sha256="f" * 64),
                self.prefix,
                _stat(len(self.prefix)),
                "JVM_LOG_PREFIX_SHA256_MISMATCH",
            ),
            (
                self.cursor,
                self.prefix + self.post_action,
                _stat(len(self.prefix + self.post_action)),
                "JVM_LOG_PREFIX_READER_SIZE_MISMATCH",
            ),
        )
        for cursor, read_result, stat_result, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                observation, reason = observe_jvm_runtime_artifact_from_log_prefix(
                    cursor,
                    prefix_reader=lambda _path, _size, raw=read_result: raw,
                    log_stat_reader=lambda _path, value=stat_result: value,
                    artifact_bytes_reader=self.artifact_files.read_bytes,
                    artifact_stat_reader=self.artifact_files.read_stat,
                )

                self.assertIsNone(observation)
                self.assertEqual(expected_reason, reason)

    def test_manifest_scan_enforces_line_and_candidate_bounds(self):
        two_lines = self.prefix + b"ordinary startup line\n"
        self.cursor = _cursor(self.log_path, two_lines)
        line_observation, line_reason = observe_jvm_runtime_artifact_from_log_prefix(
            self.cursor,
            prefix_reader=lambda _path, size: two_lines[:size],
            log_stat_reader=lambda _path: _stat(len(two_lines)),
            artifact_bytes_reader=self.artifact_files.read_bytes,
            artifact_stat_reader=self.artifact_files.read_stat,
            max_lines=1,
        )

        duplicate = self.prefix + (
            _manifest_line(self.jar_path.as_uri(), sequence=11) + "\n"
        ).encode()
        self.cursor = _cursor(self.log_path, duplicate)
        candidate_observation, candidate_reason = (
            observe_jvm_runtime_artifact_from_log_prefix(
                self.cursor,
                prefix_reader=lambda _path, size: duplicate[:size],
                log_stat_reader=lambda _path: _stat(len(duplicate)),
                artifact_bytes_reader=self.artifact_files.read_bytes,
                artifact_stat_reader=self.artifact_files.read_stat,
                max_candidate_records=1,
            )
        )

        self.assertIsNone(line_observation)
        self.assertEqual(
            "JVM_LOG_PREFIX_MANIFEST_SCAN_NOT_OK:"
            "JVM_RUN_MANIFEST_LINE_COUNT_EXCEEDS_BOUND",
            line_reason,
        )
        self.assertIsNone(candidate_observation)
        self.assertEqual(
            "JVM_LOG_PREFIX_MANIFEST_SCAN_NOT_OK:"
            "JVM_RUN_MANIFEST_CANDIDATE_COUNT_EXCEEDS_BOUND",
            candidate_reason,
        )

    def test_malformed_incomplete_or_ambiguous_prefix_fails_closed(self):
        malformed = self.prefix.replace(
            b"diagnosticCaptureStatus=complete",
            b"diagnosticCaptureStatus",
        )
        incomplete_capture = self.prefix.replace(
            b"diagnosticCaptureStatus=complete",
            b"diagnosticCaptureStatus=partial",
        )
        incomplete = self.prefix.rstrip(b"\n")
        ambiguous = self.prefix + (
            _manifest_line(self.jar_path.as_uri(), sequence=11) + "\n"
        ).encode()
        cases = (
            (
                malformed,
                "JVM_LOG_PREFIX_MANIFEST_SCAN_NOT_OK:"
                "JVM_RUN_MANIFEST_CANDIDATE_MALFORMED",
            ),
            (
                incomplete_capture,
                "JVM_LOG_PREFIX_MANIFEST_SCAN_NOT_OK:"
                "JVM_RUN_MANIFEST_CANDIDATE_MALFORMED",
            ),
            (incomplete, "JVM_LOG_PREFIX_HAS_PARTIAL_RECORD"),
            (
                ambiguous,
                "JVM_LOG_PREFIX_ARTIFACT_NOT_OBSERVED:"
                "JVM_RUN_MANIFEST_RECORD_NOT_EXCLUSIVE",
            ),
        )
        for raw, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                self.cursor = _cursor(self.log_path, raw)

                observation, reason = self._observe(raw)

                self.assertIsNone(observation)
                self.assertEqual(expected_reason, reason)

    def _observe(self, full_log: bytes):
        def read_prefix(path: Path, size: int) -> bytes:
            self.prefix_read_requests.append((path, size))
            return full_log[:size]

        return observe_jvm_runtime_artifact_from_log_prefix(
            self.cursor,
            prefix_reader=read_prefix,
            log_stat_reader=lambda _path: _stat(len(full_log)),
            artifact_bytes_reader=self.artifact_files.read_bytes,
            artifact_stat_reader=self.artifact_files.read_stat,
        )


def _cursor(path: Path, raw: bytes) -> AutomaticDepositLatestLogByteCursor:
    return AutomaticDepositLatestLogByteCursor(
        path=str(path.resolve(strict=False)),
        device=17,
        inode=29,
        size=len(raw),
        mtime_ns=100,
        prefix_sha256=hashlib.sha256(raw).hexdigest(),
    )


def _stat(size: int) -> SimpleNamespace:
    return SimpleNamespace(
        st_dev=17,
        st_ino=29,
        st_size=size,
        st_mtime_ns=101,
    )


def _store_home_candidate_line(*, capture_status: str) -> str:
    return (
        "[01:04:06] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefBoundary] traceId=trace-candidate clientTickId=813 "
        "eventSequence=11 taskInstanceId=task-1 taskRunId=run-1 "
        "parentTaskRunId=unavailable threadName=Render%20thread level=BOUNDARY "
        "event=STORE_HOME_CANDIDATE_ATTEMPT_STARTED reason=runtime_test "
        "taskClass=lavi.StoreHomeTask "
        f"diagnosticCaptureStatus={capture_status}"
    )


if __name__ == "__main__":
    unittest.main()
