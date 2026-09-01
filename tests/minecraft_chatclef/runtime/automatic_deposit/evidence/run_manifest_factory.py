#20260831_kpopmodder: Build a run manifest with existing fingerprints.
from __future__ import annotations

from ...preflight.command_fingerprint import command_fingerprint
from ...submission.invocation_fingerprint import invocation_fingerprint
from ..scenario.matrix_row import AutomaticDepositMatrixRow
from ..scenario.transport_mode import AutomaticDepositTransportMode
from .artifact_identity import AutomaticDepositArtifactIdentity
from .latest_log_byte_cursor import AutomaticDepositLatestLogByteCursor
from .run_manifest import AutomaticDepositRunManifest


def build_automatic_deposit_run_manifest(
    *,
    run_id: str,
    created_at_utc: str,
    repository_root: str,
    git_commit: str,
    git_worktree_fingerprint: str,
    harness_fingerprint: str,
    row: AutomaticDepositMatrixRow,
    operation_id: str,
    fixture_fingerprint: str,
    artifact_identity: AutomaticDepositArtifactIdentity,
    runtime_artifact_snapshot_fingerprint: str,
    loader_mod_list_fingerprint: str,
    mods_directory_fingerprint: str,
    instance_root: str,
    mods_directory: str,
    latest_log_path: str,
    latest_log_cursor: AutomaticDepositLatestLogByteCursor,
    crash_reports_directory: str,
    lavi_process_identity_fingerprint: str,
    minecraft_process_identity_fingerprint: str,
    backend: str,
    instance: str,
    world: str,
    diagnostics_mode: str,
    submission_invocation_id: str = "",
) -> AutomaticDepositRunManifest:
    invocation = str(submission_invocation_id or "").strip()
    submission_fingerprint = ""
    if (
        row.transport_mode
        is AutomaticDepositTransportMode.SUPERVISED_LAVI_SUBMIT
        and invocation
    ):
        submission_fingerprint = invocation_fingerprint(invocation)
    return AutomaticDepositRunManifest(
        schema_version="automatic-deposit-runtime/v1",
        run_id=str(run_id).strip(),
        created_at_utc=str(created_at_utc).strip(),
        repository_root=str(repository_root).strip(),
        git_commit=str(git_commit).strip(),
        git_worktree_fingerprint=str(git_worktree_fingerprint).strip(),
        harness_fingerprint=str(harness_fingerprint).strip(),
        row_id=row.row_id,
        operation_id=str(operation_id).strip(),
        transport_mode=row.transport_mode,
        fixture_fingerprint=str(fixture_fingerprint).strip(),
        artifact_identity=artifact_identity,
        runtime_artifact_snapshot_fingerprint=str(
            runtime_artifact_snapshot_fingerprint
        ).strip(),
        loader_mod_list_fingerprint=str(loader_mod_list_fingerprint).strip(),
        mods_directory_fingerprint=str(mods_directory_fingerprint).strip(),
        instance_root=str(instance_root).strip(),
        mods_directory=str(mods_directory).strip(),
        latest_log_path=str(latest_log_path).strip(),
        latest_log_cursor=latest_log_cursor,
        crash_reports_directory=str(crash_reports_directory).strip(),
        lavi_process_identity_fingerprint=str(
            lavi_process_identity_fingerprint
        ).strip(),
        minecraft_process_identity_fingerprint=str(
            minecraft_process_identity_fingerprint
        ).strip(),
        backend=str(backend).strip(),
        instance=str(instance).strip(),
        world=str(world).strip(),
        diagnostics_mode=str(diagnostics_mode).strip(),
        operator_action_fingerprint=(
            command_fingerprint(row.expected_operator_action)
            if row.expected_operator_action
            else ""
        ),
        submission_invocation_fingerprint=submission_fingerprint,
        evidence_directory=(
            str(repository_root).rstrip("/\\")
            + "/test/test_Isolation/automatic_deposit_runtime/"
            + str(run_id).strip()
        ),
    )
