#20260831_kpopmodder: Derive ChatClef deployment identity from stable disk and runtime snapshots.
from __future__ import annotations

from collections.abc import Callable
from pathlib import Path

from ...preflight.shared_file_reader import read_shared_bytes
from .artifact_identity import AutomaticDepositArtifactIdentity
from .artifact_identity_collection import (
    AutomaticDepositArtifactIdentityCollection,
    _create_artifact_identity_collection,
)
from .mods_directory_snapshot import AutomaticDepositModsDirectorySnapshot
from .runtime_artifact_snapshot import AutomaticDepositRuntimeArtifactSnapshot
from .stable_file_digest import read_automatic_deposit_stable_file_digest


def collect_automatic_deposit_artifact_identity(
    source_jar_path: object,
    mods_snapshot: AutomaticDepositModsDirectorySnapshot,
    runtime_snapshot: AutomaticDepositRuntimeArtifactSnapshot,
    *,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
) -> AutomaticDepositArtifactIdentityCollection:
    source_text = str(source_jar_path or "").strip()
    if not source_text or not Path(source_text).is_absolute():
        return _create_artifact_identity_collection(
            False,
            "SOURCE_ARTIFACT_PATH_INVALID",
        )
    if not isinstance(mods_snapshot, AutomaticDepositModsDirectorySnapshot):
        return _create_artifact_identity_collection(
            False,
            "MODS_DIRECTORY_SNAPSHOT_NOT_TYPED",
        )
    if not isinstance(runtime_snapshot, AutomaticDepositRuntimeArtifactSnapshot):
        return _create_artifact_identity_collection(
            False,
            "RUNTIME_ARTIFACT_SNAPSHOT_NOT_TYPED",
        )
    source_digest, source_reason = read_automatic_deposit_stable_file_digest(
        Path(source_text),
        bytes_reader=bytes_reader or read_shared_bytes,
        stat_reader=stat_reader,
    )
    if source_digest is None:
        return _create_artifact_identity_collection(
            False,
            f"SOURCE_ARTIFACT_{source_reason}",
        )
    discovered = tuple(
        artifact
        for artifact in mods_snapshot.artifacts
        if artifact.metadata_status == "COMPLETE" and artifact.mod_id == "altoclef"
    )
    if len(discovered) != 1:
        return _create_artifact_identity_collection(
            False,
            "DISCOVERED_CHATCLEF_JAR_NOT_EXCLUSIVE",
        )
    deployed = discovered[0]
    if _canonical(deployed.path) != _canonical(runtime_snapshot.loaded_code_source):
        return _create_artifact_identity_collection(
            False,
            "RUNTIME_CODE_SOURCE_NOT_DISCOVERED_CHATCLEF_JAR",
        )
    runtime_chatclef = tuple(
        mod for mod in runtime_snapshot.loaded_mods if mod.mod_id == "altoclef"
    )
    if len(runtime_chatclef) != 1:
        return _create_artifact_identity_collection(
            False,
            "RUNTIME_CHATCLEF_MOD_NOT_EXCLUSIVE",
        )
    if runtime_chatclef[0].version != deployed.version:
        return _create_artifact_identity_collection(
            False,
            "RUNTIME_AND_DISK_CHATCLEF_VERSION_MISMATCH",
        )
    identity = AutomaticDepositArtifactIdentity(
        source_jar_path=source_digest.path,
        source_jar_sha256=source_digest.sha256,
        deployed_jar_path=deployed.path,
        deployed_jar_sha256=deployed.sha256,
        loaded_code_source=runtime_snapshot.loaded_code_source,
        discovered_chatclef_jars=tuple(artifact.path for artifact in discovered),
    )
    return _create_artifact_identity_collection(
        True,
        "ARTIFACT_IDENTITY_COLLECTED_FROM_DISCOVERY",
        identity,
        runtime_snapshot_fingerprint=runtime_snapshot.snapshot_fingerprint,
        mods_directory_fingerprint=mods_snapshot.mods_directory_fingerprint,
    )


def _canonical(value: str) -> str:
    return str(Path(value).resolve(strict=False)).casefold()
