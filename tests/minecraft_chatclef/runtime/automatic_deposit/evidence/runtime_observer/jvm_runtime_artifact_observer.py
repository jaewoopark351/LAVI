#20260901_kpopmodder: Hash only the exact JAR path reported by one complete JVM run manifest.
from __future__ import annotations

import re
from collections.abc import Callable
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import url2pathname

from ...oracle.runtime_log.diagnostic_record import ProductionDiagnosticRecord
from ...oracle.runtime_log.diagnostic_record_parser import BOUNDED_EVENT_ENVELOPE
from ...oracle.runtime_log.production_log_scan_result import (
    ProductionDiagnosticLogScanResult,
)
from ..stable_file_digest import read_automatic_deposit_stable_file_digest
from .jvm_runtime_artifact_observation import (
    JvmRuntimeArtifactObservation,
    _create_jvm_runtime_artifact_observation,
)
from .runtime_code_source_last_modified import parse_java_file_time_epoch_ns


_EVENT_NAME = "STORE_HOME_RUN_MANIFEST"
_COMPLETE_CAPTURE_STATUSES = frozenset(("complete", "complete_no_active_candidate"))
_RUN_ID_SOURCES = frozenset(("RUNTIME_GENERATED", "EXTERNAL_BUILD_DEPLOY_MANIFEST"))
_REPORTED_SHA_SOURCE = "EXTERNAL_SYSTEM_PROPERTY_NOT_INDEPENDENTLY_VERIFIED"
_SHA256 = re.compile(r"[0-9a-fA-F]{64}\Z", re.ASCII)


def observe_jvm_runtime_artifact(
    scan: object,
    *,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
) -> tuple[JvmRuntimeArtifactObservation | None, str]:
    if not isinstance(scan, ProductionDiagnosticLogScanResult):
        return None, "JVM_RUN_MANIFEST_SCAN_NOT_TYPED"
    if scan.ok is not True:
        return None, f"JVM_RUN_MANIFEST_SCAN_NOT_OK:{_bounded_reason(scan.reason)}"
    manifests = tuple(record for record in scan.records if record.event_name == _EVENT_NAME)
    if not manifests:
        return None, "JVM_RUN_MANIFEST_RECORD_MISSING"
    if len(manifests) != 1:
        return None, "JVM_RUN_MANIFEST_RECORD_NOT_EXCLUSIVE"
    record = manifests[0]
    record_problem = _record_problem(record)
    if record_problem:
        return None, record_problem

    run_id = _required_field(record, "runId")
    if not _bounded_identity(run_id, 256):
        return None, "JVM_RUN_MANIFEST_ID_INVALID"
    run_id_source = _required_field(record, "runManifestIdSource")
    if run_id_source not in _RUN_ID_SOURCES:
        return None, "JVM_RUN_MANIFEST_ID_SOURCE_INVALID"
    runtime_uri = _required_field(record, "runtimeCodeSource")
    runtime_path_alias = _required_field(record, "runtimeCodeSourcePath")
    if not runtime_uri or not runtime_path_alias:
        return None, "JVM_RUNTIME_CODE_SOURCE_FIELD_MISSING"
    if runtime_uri != runtime_path_alias:
        return None, "JVM_RUNTIME_CODE_SOURCE_FIELDS_MISMATCH"
    runtime_path = _file_uri_path(runtime_uri)
    if runtime_path is None or runtime_path.suffix.casefold() != ".jar":
        return None, "JVM_RUNTIME_CODE_SOURCE_URI_INVALID"

    last_modified = _required_field(record, "runtimeCodeSourceLastModified")
    identity = _required_field(record, "runtimeCodeSourceIdentity")
    if not last_modified or identity != f"{runtime_uri}|lastModified={last_modified}":
        return None, "JVM_RUNTIME_CODE_SOURCE_IDENTITY_MISMATCH"
    last_modified_ns = parse_java_file_time_epoch_ns(last_modified)
    if last_modified_ns is None:
        return None, "JVM_RUNTIME_CODE_SOURCE_LAST_MODIFIED_INVALID"
    reported_sha_source = _required_field(record, "runtimeJarSha256EvidenceSource")
    if reported_sha_source != _REPORTED_SHA_SOURCE:
        return None, "JVM_REPORTED_RUNTIME_SHA256_SOURCE_INVALID"
    reported_sha = _required_field(record, "runtimeJarSha256")
    if reported_sha != "UNVERIFIED" and _SHA256.fullmatch(reported_sha) is None:
        return None, "JVM_REPORTED_RUNTIME_SHA256_INVALID"

    versions: dict[str, str] = {}
    for field_name, reason_name in (
        ("minecraftVersion", "MINECRAFT_VERSION"),
        ("fabricLoaderVersion", "FABRIC_LOADER_VERSION"),
        ("chatClefVersion", "CHATCLEF_VERSION"),
    ):
        value = _required_field(record, field_name)
        if not _bounded_identity(value, 128) or _is_unavailable(value):
            return None, f"JVM_RUN_MANIFEST_{reason_name}_INVALID"
        versions[field_name] = value

    context, context_reason = _command_context(record)
    if context is None:
        return None, context_reason
    context_available, command_session_id, command_generation = context

    digest, digest_reason = read_automatic_deposit_stable_file_digest(
        runtime_path,
        bytes_reader=bytes_reader,
        stat_reader=stat_reader,
    )
    if digest is None:
        return None, f"JVM_RUNTIME_CODE_SOURCE_{digest_reason}"
    if digest.mtime_ns != last_modified_ns:
        return None, "JVM_RUNTIME_CODE_SOURCE_LAST_MODIFIED_MISMATCH"
    if reported_sha != "UNVERIFIED" and reported_sha.casefold() != digest.sha256:
        return None, "JVM_REPORTED_RUNTIME_SHA256_MISMATCH"

    return (
        _create_jvm_runtime_artifact_observation(
            run_manifest_id=run_id,
            run_manifest_id_source=run_id_source,
            manifest_event_sequence=record.event_sequence,
            runtime_code_source_uri=runtime_uri,
            runtime_code_source_path=digest.path,
            runtime_code_source_sha256=digest.sha256,
            runtime_code_source_size=digest.size,
            runtime_code_source_mtime_ns=digest.mtime_ns,
            reported_runtime_jar_sha256=reported_sha,
            minecraft_version=versions["minecraftVersion"],
            fabric_loader_version=versions["fabricLoaderVersion"],
            chatclef_version=versions["chatClefVersion"],
            command_context_available=context_available,
            command_session_id=command_session_id,
            command_connection_generation=command_generation,
        ),
        "JVM_RUNTIME_ARTIFACT_OBSERVED",
    )


def _record_problem(record: ProductionDiagnosticRecord) -> str:
    if record.envelope_kind != BOUNDED_EVENT_ENVELOPE:
        return "JVM_RUN_MANIFEST_ENVELOPE_INVALID"
    event = _required_field(record, "event")
    if event != _EVENT_NAME:
        return "JVM_RUN_MANIFEST_EVENT_INVALID"
    capture_status = _required_field(record, "diagnosticCaptureStatus")
    if capture_status not in _COMPLETE_CAPTURE_STATUSES:
        return "JVM_RUN_MANIFEST_CAPTURE_NOT_COMPLETE"
    sequence = _required_field(record, "eventSequence")
    if sequence != str(record.event_sequence):
        return "JVM_RUN_MANIFEST_EVENT_SEQUENCE_MISMATCH"
    return ""


def _command_context(
    record: ProductionDiagnosticRecord,
) -> tuple[tuple[bool, str, int | None] | None, str]:
    available = _optional_field(record, "commandContextAvailable")
    if available in (None, "false"):
        session_id = _optional_field(record, "commandSessionId") or ""
        generation = _optional_field(record, "commandConnectionGeneration") or ""
        if _concrete_context_value(session_id) or _concrete_context_value(generation):
            return None, "JVM_RUN_MANIFEST_COMMAND_CONTEXT_CONTRADICTORY"
        return (False, "", None), ""
    if available != "true":
        return None, "JVM_RUN_MANIFEST_COMMAND_CONTEXT_FLAG_INVALID"
    session_id = _optional_field(record, "commandSessionId") or ""
    generation_text = _optional_field(record, "commandConnectionGeneration") or ""
    if not _bounded_identity(session_id, 256):
        return None, "JVM_RUN_MANIFEST_COMMAND_SESSION_ID_INVALID"
    if not generation_text.isascii() or not generation_text.isdecimal():
        return None, "JVM_RUN_MANIFEST_COMMAND_GENERATION_INVALID"
    generation = int(generation_text)
    if not 1 <= generation <= 9_223_372_036_854_775_807:
        return None, "JVM_RUN_MANIFEST_COMMAND_GENERATION_INVALID"
    return (True, session_id, generation), ""


def _required_field(record: ProductionDiagnosticRecord, key: str) -> str:
    value, reason = record.resolved_value(key)
    if reason in {
        "PRODUCTION_LOG_FIELD_MISSING",
        "PRODUCTION_LOG_FIELD_REPEATS_AMBIGUOUS",
    }:
        return ""
    return value or ""


def _optional_field(record: ProductionDiagnosticRecord, key: str) -> str | None:
    value, reason = record.resolved_value(key)
    if reason == "PRODUCTION_LOG_FIELD_MISSING":
        return None
    if reason == "PRODUCTION_LOG_FIELD_REPEATS_AMBIGUOUS":
        return "__AMBIGUOUS__"
    return value


def _file_uri_path(value: str) -> Path | None:
    try:
        parsed = urlsplit(value)
        if (
            parsed.scheme.casefold() != "file"
            or parsed.netloc not in ("", "localhost")
            or parsed.query
            or parsed.fragment
        ):
            return None
        path = Path(url2pathname(parsed.path))
        if not path.is_absolute():
            return None
        return path.resolve(strict=False)
    except (OSError, ValueError):
        return None


def _bounded_identity(value: str, maximum: int) -> bool:
    return (
        bool(value)
        and len(value) <= maximum
        and not any(ord(character) < 0x20 or ord(character) == 0x7F for character in value)
        and not _is_unavailable(value)
    )


def _concrete_context_value(value: str) -> bool:
    return bool(value) and value.casefold() not in {"unavailable", "none"}


def _is_unavailable(value: str) -> bool:
    folded = value.casefold()
    return folded in {"unavailable", "unverified"} or folded.startswith("unavailable_")


def _bounded_reason(value: object) -> str:
    text = str(value or "")
    if not text or len(text) > 160:
        return "UNAVAILABLE"
    return text
