# 20260901_kpopmodder: Observe an emit-once JVM manifest only from the cursor-sealed pre-action prefix.
from __future__ import annotations

import hashlib
import re
from collections.abc import Callable
from pathlib import Path

from .....preflight.minecraft_log_decoder import decode_log_bytes
from ....oracle.runtime_log.production_log_delta_scanner import (
    MAX_CANDIDATE_RECORDS,
    MAX_DELTA_LINES,
)
from ...latest_log_byte_cursor import (
    AutomaticDepositLatestLogByteCursor,
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ...latest_log_delta_result import _create_latest_log_delta_result
from ..jvm_runtime_artifact_observer import observe_jvm_runtime_artifact
from .jvm_runtime_artifact_log_prefix_observation import (
    JvmRuntimeArtifactLogPrefixObservation,
    _create_jvm_runtime_artifact_log_prefix_observation,
)
from .jvm_run_manifest_log_prefix_scanner import (
    scan_jvm_run_manifest_log_prefix,
)
from .shared_log_prefix_reader import read_shared_log_prefix


MAX_LOG_PREFIX_BYTES = 64 * 1024 * 1024
_SHA256 = re.compile(r"[0-9a-fA-F]{64}\Z", re.ASCII)


def observe_jvm_runtime_artifact_from_log_prefix(
    cursor: object,
    *,
    prefix_reader: Callable[[Path, int], bytes] | None = None,
    log_stat_reader: Callable[[Path], object] | None = None,
    artifact_bytes_reader: Callable[[Path], bytes] | None = None,
    artifact_stat_reader: Callable[[Path], object] | None = None,
    max_prefix_bytes: int = MAX_LOG_PREFIX_BYTES,
    max_lines: int = MAX_DELTA_LINES,
    max_candidate_records: int = MAX_CANDIDATE_RECORDS,
) -> tuple[JvmRuntimeArtifactLogPrefixObservation | None, str]:
    if not isinstance(cursor, AutomaticDepositLatestLogByteCursor):
        return None, "JVM_LOG_PREFIX_CURSOR_NOT_TYPED"
    if not _valid_bound(max_prefix_bytes, MAX_LOG_PREFIX_BYTES):
        return None, "JVM_LOG_PREFIX_BYTE_BOUND_INVALID"
    if not _valid_bound(max_lines, MAX_DELTA_LINES) or not _valid_bound(
        max_candidate_records,
        MAX_CANDIDATE_RECORDS,
    ):
        return None, "JVM_LOG_PREFIX_SCAN_BOUND_INVALID"
    path = _canonical_latest_log_path(cursor.path)
    if path is None:
        return None, "JVM_LOG_PREFIX_PATH_INVALID"
    cursor_problem = _cursor_problem(cursor, max_prefix_bytes)
    if cursor_problem:
        return None, cursor_problem

    read_prefix = prefix_reader or read_shared_log_prefix
    read_stat = log_stat_reader or (lambda candidate: candidate.stat())
    try:
        before = _stat_values(read_stat(path))
        if before is None:
            return None, "JVM_LOG_PREFIX_STAT_INVALID"
        identity_problem = _identity_problem(cursor, before)
        if identity_problem:
            return None, identity_problem
        raw = read_prefix(path, cursor.size)
        after = _stat_values(read_stat(path))
    except Exception as error:
        return None, f"JVM_LOG_PREFIX_READ_FAILED:{type(error).__name__}"
    if after is None:
        return None, "JVM_LOG_PREFIX_STAT_INVALID"
    identity_problem = _identity_problem(cursor, after)
    if identity_problem:
        return None, identity_problem
    if not isinstance(raw, bytes):
        return None, "JVM_LOG_PREFIX_READER_DID_NOT_RETURN_BYTES"
    if len(raw) != cursor.size:
        return None, "JVM_LOG_PREFIX_READER_SIZE_MISMATCH"
    if hashlib.sha256(raw).hexdigest() != cursor.prefix_sha256.casefold():
        return None, "JVM_LOG_PREFIX_SHA256_MISMATCH"
    if raw and not raw.endswith(b"\n"):
        return None, "JVM_LOG_PREFIX_HAS_PARTIAL_RECORD"

    decoded = decode_log_bytes(raw)
    if decoded.get("ok") is not True:
        return None, "JVM_LOG_PREFIX_DECODE_FAILED"
    cursor_fingerprint = automatic_deposit_latest_log_cursor_fingerprint(cursor)
    prefix_delta = _create_latest_log_delta_result(
        True,
        "LATEST_LOG_PRE_ACTION_PREFIX_READ",
        None,
        0,
        cursor.size,
        str(decoded.get("text") or ""),
        str(decoded.get("encoding") or ""),
        cursor_fingerprint,
    )
    scan = scan_jvm_run_manifest_log_prefix(
        prefix_delta,
        max_lines=max_lines,
        max_candidate_records=max_candidate_records,
    )
    if not scan.ok:
        return None, (
            "JVM_LOG_PREFIX_MANIFEST_SCAN_NOT_OK:"
            f"{_bounded_reason(scan.reason)}"
        )
    raw_observation, artifact_reason = observe_jvm_runtime_artifact(
        scan,
        bytes_reader=artifact_bytes_reader,
        stat_reader=artifact_stat_reader,
    )
    if raw_observation is None:
        return None, (
            "JVM_LOG_PREFIX_ARTIFACT_NOT_OBSERVED:"
            f"{_bounded_reason(artifact_reason)}"
        )
    return (
        _create_jvm_runtime_artifact_log_prefix_observation(
            runtime_artifact_observation=raw_observation,
            source_log_path=str(path),
            source_cursor_fingerprint=cursor_fingerprint,
            source_prefix_sha256=cursor.prefix_sha256.casefold(),
            source_prefix_size=cursor.size,
            source_device=cursor.device,
            source_inode=cursor.inode,
        ),
        "JVM_RUNTIME_ARTIFACT_LOG_PREFIX_OBSERVED",
    )


def _cursor_problem(
    cursor: AutomaticDepositLatestLogByteCursor,
    max_prefix_bytes: int,
) -> str:
    for value in (cursor.device, cursor.inode, cursor.size, cursor.mtime_ns):
        if type(value) is not int or value < 0:
            return "JVM_LOG_PREFIX_CURSOR_IDENTITY_INVALID"
    if cursor.size > max_prefix_bytes:
        return "JVM_LOG_PREFIX_EXCEEDS_BYTE_BOUND"
    if not isinstance(cursor.prefix_sha256, str) or _SHA256.fullmatch(
        cursor.prefix_sha256
    ) is None:
        return "JVM_LOG_PREFIX_CURSOR_SHA256_INVALID"
    return ""


def _identity_problem(
    cursor: AutomaticDepositLatestLogByteCursor,
    current: tuple[int, int, int, int],
) -> str:
    device, inode, size, mtime_ns = current
    if device != cursor.device:
        return "JVM_LOG_PREFIX_DEVICE_MISMATCH"
    if inode != cursor.inode:
        return "JVM_LOG_PREFIX_INODE_MISMATCH"
    if size < cursor.size:
        return "JVM_LOG_PREFIX_TRUNCATED"
    if mtime_ns < cursor.mtime_ns:
        return "JVM_LOG_PREFIX_MTIME_REGRESSION"
    return ""


def _stat_values(value: object) -> tuple[int, int, int, int] | None:
    raw = tuple(
        getattr(value, name, None)
        for name in ("st_dev", "st_ino", "st_size", "st_mtime_ns")
    )
    if any(type(item) is not int or item < 0 for item in raw):
        return None
    return raw


def _canonical_latest_log_path(value: object) -> Path | None:
    if not isinstance(value, str) or not value or value != value.strip():
        return None
    try:
        path = Path(value)
        if not path.is_absolute() or path.name.casefold() != "latest.log":
            return None
        return path.resolve(strict=False)
    except (OSError, ValueError):
        return None


def _valid_bound(value: object, maximum: int) -> bool:
    return type(value) is int and 1 <= value <= maximum


def _bounded_reason(value: object) -> str:
    text = str(value or "")
    if (
        not text
        or len(text) > 160
        or any(ord(character) < 0x20 or ord(character) == 0x7F for character in text)
    ):
        return "UNAVAILABLE"
    return text
