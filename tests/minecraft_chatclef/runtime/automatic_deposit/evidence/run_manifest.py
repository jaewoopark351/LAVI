#20260831_kpopmodder: Bind one runtime attempt to immutable evidence identity.
from __future__ import annotations

from dataclasses import dataclass

from ..scenario.transport_mode import AutomaticDepositTransportMode
from .artifact_identity import AutomaticDepositArtifactIdentity
from .latest_log_byte_cursor import AutomaticDepositLatestLogByteCursor


@dataclass(frozen=True, slots=True)
class AutomaticDepositRunManifest:
    schema_version: str
    run_id: str
    created_at_utc: str
    repository_root: str
    git_commit: str
    git_worktree_fingerprint: str
    harness_fingerprint: str
    row_id: str
    operation_id: str
    transport_mode: AutomaticDepositTransportMode
    fixture_fingerprint: str
    artifact_identity: AutomaticDepositArtifactIdentity
    runtime_artifact_snapshot_fingerprint: str
    loader_mod_list_fingerprint: str
    mods_directory_fingerprint: str
    instance_root: str
    mods_directory: str
    latest_log_path: str
    latest_log_cursor: AutomaticDepositLatestLogByteCursor
    crash_reports_directory: str
    lavi_process_identity_fingerprint: str
    minecraft_process_identity_fingerprint: str
    backend: str
    instance: str
    world: str
    diagnostics_mode: str
    operator_action_fingerprint: str
    submission_invocation_fingerprint: str
    evidence_directory: str
