# 20260901_kpopmodder: Build sealed runtime observations for bundle contract tests.
from __future__ import annotations

from pathlib import Path

from ....latest_log_byte_cursor import (
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ....runtime_observer.jvm_runtime_artifact_observation import (
    _create_jvm_runtime_artifact_observation,
)
from ....runtime_observer.log_prefix.jvm_runtime_artifact_log_prefix_observation import (
    _create_jvm_runtime_artifact_log_prefix_observation,
)
from ....runtime_observer.production_status_observer import (
    observe_production_fabric_status,
)
from ....runtime_observer.test_production_status_observer import (
    _production_status,
)
from .....scenario.matrix_catalog import automatic_deposit_matrix_catalog
from .....testing.hermetic_evidence import hermetic_fixture, hermetic_run_manifest


def sealed_inputs(
    *,
    command_context_available: bool,
    command_session_id: str = "",
    command_connection_generation: int | None = None,
):
    row = next(row for row in automatic_deposit_matrix_catalog() if row.row_id == "P1")
    run_manifest = hermetic_run_manifest(row, hermetic_fixture(row))
    status, reason = observe_production_fabric_status(_production_status())
    if status is None:
        raise AssertionError(reason)
    artifact = run_manifest.artifact_identity
    code_source_path = str(Path(artifact.deployed_jar_path).resolve(strict=False))
    raw = _create_jvm_runtime_artifact_observation(
        run_manifest_id="jvm-static-manifest-1",
        run_manifest_id_source="RUNTIME_GENERATED",
        manifest_event_sequence=9,
        runtime_code_source_uri=Path(code_source_path).as_uri(),
        runtime_code_source_path=code_source_path,
        runtime_code_source_sha256=artifact.deployed_jar_sha256,
        runtime_code_source_size=1024,
        runtime_code_source_mtime_ns=123,
        reported_runtime_jar_sha256="UNVERIFIED",
        minecraft_version="1.20.1",
        fabric_loader_version="0.19.3",
        chatclef_version="1.20.1-0.18.23",
        command_context_available=command_context_available,
        command_session_id=command_session_id,
        command_connection_generation=command_connection_generation,
    )
    cursor = run_manifest.latest_log_cursor
    log_prefix = _create_jvm_runtime_artifact_log_prefix_observation(
        runtime_artifact_observation=raw,
        source_log_path=str(Path(cursor.path).resolve(strict=False)),
        source_cursor_fingerprint=(
            automatic_deposit_latest_log_cursor_fingerprint(cursor)
        ),
        source_prefix_sha256=cursor.prefix_sha256.casefold(),
        source_prefix_size=cursor.size,
        source_device=cursor.device,
        source_inode=cursor.inode,
    )
    return status, log_prefix, run_manifest
