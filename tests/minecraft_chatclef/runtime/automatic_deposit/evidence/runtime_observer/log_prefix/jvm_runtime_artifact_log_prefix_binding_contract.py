# 20260901_kpopmodder: Bind log-prefix provenance to the exact cursor sealed by one typed run manifest.
from __future__ import annotations

import re
from pathlib import Path

from ...latest_log_byte_cursor import (
    AutomaticDepositLatestLogByteCursor,
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ...run_manifest import AutomaticDepositRunManifest
from .jvm_runtime_artifact_log_prefix_observation import (
    JvmRuntimeArtifactLogPrefixObservation,
)


_SHA256 = re.compile(r"[0-9a-fA-F]{64}\Z", re.ASCII)


def verify_jvm_runtime_artifact_log_prefix_binding(
    observation: object,
    run_manifest: object,
) -> tuple[str, ...]:
    errors: list[str] = []
    if not isinstance(observation, JvmRuntimeArtifactLogPrefixObservation):
        errors.append("JVM_LOG_PREFIX_OBSERVATION_NOT_TYPED")
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        errors.append("JVM_LOG_PREFIX_RUN_MANIFEST_NOT_TYPED")
    if errors:
        return tuple(errors)
    cursor = run_manifest.latest_log_cursor
    if not isinstance(cursor, AutomaticDepositLatestLogByteCursor):
        return ("JVM_LOG_PREFIX_RUN_CURSOR_NOT_TYPED",)

    cursor_errors = _cursor_shape_errors(cursor)
    if cursor_errors:
        return cursor_errors
    expected_path = _resolved_path(cursor.path)
    if expected_path is None:
        return ("JVM_LOG_PREFIX_RUN_CURSOR_PATH_INVALID",)
    run_log_path = _canonical(run_manifest.latest_log_path)
    if run_log_path is None:
        return ("JVM_LOG_PREFIX_RUN_LATEST_LOG_PATH_INVALID",)
    observation_log_path = _canonical(observation.source_log_path)
    if observation_log_path is None:
        return ("JVM_LOG_PREFIX_OBSERVATION_PATH_INVALID",)

    if observation.source_log_path != expected_path:
        errors.append("JVM_LOG_PREFIX_CURSOR_PATH_MISMATCH")
    if run_log_path != observation_log_path:
        errors.append("JVM_LOG_PREFIX_RUN_LATEST_LOG_PATH_MISMATCH")
    if observation.source_cursor_fingerprint != (
        automatic_deposit_latest_log_cursor_fingerprint(cursor)
    ):
        errors.append("JVM_LOG_PREFIX_CURSOR_FINGERPRINT_MISMATCH")
    if observation.source_prefix_sha256 != cursor.prefix_sha256.casefold():
        errors.append("JVM_LOG_PREFIX_CURSOR_SHA256_MISMATCH")
    if observation.source_prefix_size != cursor.size:
        errors.append("JVM_LOG_PREFIX_CURSOR_SIZE_MISMATCH")
    if observation.source_device != cursor.device:
        errors.append("JVM_LOG_PREFIX_CURSOR_DEVICE_MISMATCH")
    if observation.source_inode != cursor.inode:
        errors.append("JVM_LOG_PREFIX_CURSOR_INODE_MISMATCH")
    if observation.manifest_record_sequence != (
        observation.runtime_artifact_observation.manifest_event_sequence
    ):
        errors.append("JVM_LOG_PREFIX_MANIFEST_SEQUENCE_MISMATCH")
    return tuple(errors)


def _cursor_shape_errors(
    cursor: AutomaticDepositLatestLogByteCursor,
) -> tuple[str, ...]:
    errors: list[str] = []
    if _resolved_path(cursor.path) is None:
        errors.append("JVM_LOG_PREFIX_RUN_CURSOR_PATH_INVALID")
    for field_name in ("device", "inode", "size", "mtime_ns"):
        value = getattr(cursor, field_name)
        if type(value) is not int or value < 0:
            errors.append(
                f"JVM_LOG_PREFIX_RUN_CURSOR_{field_name.upper()}_INVALID"
            )
    if not isinstance(cursor.prefix_sha256, str) or _SHA256.fullmatch(
        cursor.prefix_sha256
    ) is None:
        errors.append("JVM_LOG_PREFIX_RUN_CURSOR_SHA256_INVALID")
    return tuple(errors)


def _resolved_path(value: object) -> str | None:
    if not isinstance(value, str) or not value or value != value.strip():
        return None
    try:
        path = Path(value)
        if not path.is_absolute():
            return None
        return str(path.resolve(strict=False))
    except (OSError, ValueError):
        return None


def _canonical(value: object) -> str | None:
    resolved = _resolved_path(value)
    return resolved.casefold() if resolved is not None else None
