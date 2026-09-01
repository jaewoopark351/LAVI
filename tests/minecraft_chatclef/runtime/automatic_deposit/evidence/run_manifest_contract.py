#20260831_kpopmodder: Validate one run manifest before any live transport can execute.
from __future__ import annotations

import re
from datetime import datetime, timezone
from pathlib import Path

from ...preflight.command_fingerprint import command_fingerprint
from ..scenario.matrix_row import AutomaticDepositMatrixRow
from ..scenario.transport_mode import AutomaticDepositTransportMode
from .artifact_identity import AutomaticDepositArtifactIdentity
from .artifact_identity_contract import (
    verify_automatic_deposit_artifact_identity,
)
from .run_manifest import AutomaticDepositRunManifest
from .run_manifest_verification import AutomaticDepositRunManifestVerification
from .runtime_observer.production_backend_identity import (
    PRODUCTION_FABRIC_BACKEND_ID,
)
from .latest_log_byte_cursor import AutomaticDepositLatestLogByteCursor


def verify_automatic_deposit_run_manifest(
    manifest: AutomaticDepositRunManifest,
    row: AutomaticDepositMatrixRow,
) -> AutomaticDepositRunManifestVerification:
    errors: list[str] = []
    active_repository_root = Path(__file__).resolve().parents[5]
    if manifest.schema_version != "automatic-deposit-runtime/v1":
        errors.append("RUN_MANIFEST_SCHEMA_VERSION_INVALID")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}", manifest.run_id):
        errors.append("RUN_ID_INVALID")
    if manifest.run_id in (".", ".."):
        errors.append("RUN_ID_INVALID")
    if not _is_utc_timestamp(manifest.created_at_utc):
        errors.append("RUN_CREATED_AT_NOT_UTC")
    if not re.fullmatch(r"[0-9a-fA-F]{40}", manifest.git_commit):
        errors.append("GIT_COMMIT_INVALID")
    for name, value in (
        ("GIT_WORKTREE_FINGERPRINT", manifest.git_worktree_fingerprint),
        ("HARNESS_FINGERPRINT", manifest.harness_fingerprint),
        ("FIXTURE_FINGERPRINT", manifest.fixture_fingerprint),
        (
            "RUNTIME_ARTIFACT_SNAPSHOT_FINGERPRINT",
            manifest.runtime_artifact_snapshot_fingerprint,
        ),
        ("LOADER_MOD_LIST_FINGERPRINT", manifest.loader_mod_list_fingerprint),
        ("MODS_DIRECTORY_FINGERPRINT", manifest.mods_directory_fingerprint),
        ("LAVI_PROCESS_IDENTITY", manifest.lavi_process_identity_fingerprint),
        (
            "MINECRAFT_PROCESS_IDENTITY",
            manifest.minecraft_process_identity_fingerprint,
        ),
    ):
        if not _is_sha256(value):
            errors.append(f"{name}_INVALID")
    if manifest.row_id != row.row_id:
        errors.append("RUN_ROW_ID_MISMATCH")
    if not re.fullmatch(
        r"[A-Za-z0-9][A-Za-z0-9._:-]{0,127}", manifest.operation_id
    ):
        errors.append("RUN_OPERATION_ID_INVALID")
    if manifest.transport_mode is not row.transport_mode:
        errors.append("RUN_TRANSPORT_MODE_MISMATCH")
    if manifest.diagnostics_mode not in ("OFF", "BOUNDARY", "VERBOSE"):
        errors.append("RUN_DIAGNOSTICS_MODE_INVALID")
    for name, value in (
        ("REPOSITORY_ROOT", manifest.repository_root),
        ("INSTANCE_ROOT", manifest.instance_root),
        ("MODS_DIRECTORY", manifest.mods_directory),
        ("LATEST_LOG_PATH", manifest.latest_log_path),
        ("CRASH_REPORTS_DIRECTORY", manifest.crash_reports_directory),
        ("EVIDENCE_DIRECTORY", manifest.evidence_directory),
    ):
        if not Path(value).is_absolute():
            errors.append(f"{name}_NOT_ABSOLUTE")
    if _canonical(manifest.repository_root) != _canonical(
        str(active_repository_root)
    ):
        errors.append("REPOSITORY_ROOT_NOT_ACTIVE")
    if not _is_within(manifest.mods_directory, manifest.instance_root):
        errors.append("MODS_DIRECTORY_OUTSIDE_INSTANCE")
    if not _is_within(manifest.latest_log_path, manifest.instance_root):
        errors.append("LATEST_LOG_OUTSIDE_INSTANCE")
    #20260901_kpopmodder: Validate nested evidence types before dereferencing them.
    if not isinstance(
        manifest.latest_log_cursor,
        AutomaticDepositLatestLogByteCursor,
    ):
        errors.append("LATEST_LOG_CURSOR_NOT_TYPED")
    else:
        if _canonical(manifest.latest_log_cursor.path) != _canonical(
            manifest.latest_log_path
        ):
            errors.append("LATEST_LOG_CURSOR_PATH_MISMATCH")
        if (
            manifest.latest_log_cursor.device < 0
            or manifest.latest_log_cursor.inode < 0
            or manifest.latest_log_cursor.size < 0
            or manifest.latest_log_cursor.mtime_ns < 0
            or not _is_sha256(manifest.latest_log_cursor.prefix_sha256)
        ):
            errors.append("LATEST_LOG_CURSOR_IDENTITY_INVALID")
    if not _is_within(manifest.crash_reports_directory, manifest.instance_root):
        errors.append("CRASH_REPORTS_OUTSIDE_INSTANCE")
    expected_evidence_directory = Path(manifest.repository_root).joinpath(
        "test",
        "test_Isolation",
        "automatic_deposit_runtime",
        manifest.run_id,
    )
    if _canonical(manifest.evidence_directory) != _canonical(
        str(expected_evidence_directory)
    ):
        errors.append("EVIDENCE_DIRECTORY_NOT_CANONICAL")
    if not _is_within(manifest.evidence_directory, manifest.repository_root):
        errors.append("EVIDENCE_DIRECTORY_OUTSIDE_REPOSITORY")
    if not isinstance(
        manifest.artifact_identity,
        AutomaticDepositArtifactIdentity,
    ):
        errors.append("RUN_ARTIFACT_IDENTITY_NOT_TYPED")
    else:
        if not _is_within(
            manifest.artifact_identity.deployed_jar_path,
            manifest.mods_directory,
        ):
            errors.append("DEPLOYED_JAR_OUTSIDE_MODS_DIRECTORY")
        if not _is_within(
            manifest.artifact_identity.source_jar_path,
            manifest.repository_root,
        ):
            errors.append("SOURCE_JAR_OUTSIDE_REPOSITORY")
        artifact = verify_automatic_deposit_artifact_identity(
            manifest.artifact_identity
        )
        errors.extend(artifact.errors)
    expected_action_fingerprint = (
        command_fingerprint(row.expected_operator_action)
        if row.expected_operator_action
        else ""
    )
    if manifest.operator_action_fingerprint != expected_action_fingerprint:
        errors.append("OPERATOR_ACTION_FINGERPRINT_MISMATCH")
    if row.transport_mode in (
        AutomaticDepositTransportMode.OPERATOR_MANUAL_OBSERVE_ONLY,
        AutomaticDepositTransportMode.NO_COMMAND_AUTOMATIC_TRIGGER,
    ):
        if manifest.submission_invocation_fingerprint:
            errors.append("NON_SUBMIT_ROW_HAS_INVOCATION_FINGERPRINT")
    elif not _is_sha256(manifest.submission_invocation_fingerprint):
        errors.append("SUPERVISED_INVOCATION_FINGERPRINT_INVALID")
    for name, value in (
        ("INSTANCE", manifest.instance),
        ("WORLD", manifest.world),
    ):
        if not value.strip():
            errors.append(f"{name}_MISSING")
    if not manifest.backend.strip():
        errors.append("BACKEND_MISSING")
    elif manifest.backend != PRODUCTION_FABRIC_BACKEND_ID:
        errors.append("RUN_BACKEND_NOT_CANONICAL")
    return AutomaticDepositRunManifestVerification(
        ok=not errors,
        reason=(
            "RUN_MANIFEST_VERIFIED"
            if not errors
            else "RUN_MANIFEST_INCONCLUSIVE"
        ),
        errors=tuple(errors),
    )


def _is_sha256(value: object) -> bool:
    return bool(re.fullmatch(r"[0-9a-fA-F]{64}", str(value or "")))


def _is_utc_timestamp(value: object) -> bool:
    text = str(value or "").strip()
    if not text.endswith("Z"):
        return False
    try:
        parsed = datetime.fromisoformat(text[:-1] + "+00:00")
    except ValueError:
        return False
    return parsed.tzinfo is not None and parsed.utcoffset() == timezone.utc.utcoffset(
        parsed
    )


def _canonical(value: str) -> str:
    return str(Path(value).resolve(strict=False)).casefold()


def _is_within(candidate: str, parent: str) -> bool:
    candidate_path = Path(candidate).resolve(strict=False)
    parent_path = Path(parent).resolve(strict=False)
    try:
        candidate_path.relative_to(parent_path)
    except ValueError:
        return False
    return candidate_path != parent_path
