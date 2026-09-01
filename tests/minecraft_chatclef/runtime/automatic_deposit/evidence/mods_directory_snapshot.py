#20260831_kpopmodder: Seal one stable scan of the active instance mods directory.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field


_MODS_DIRECTORY_SNAPSHOT_SEAL = object()


@dataclass(frozen=True, slots=True)
class AutomaticDepositModArtifact:
    path: str
    sha256: str
    size: int
    mtime_ns: int
    mod_id: str
    version: str
    metadata_status: str


@dataclass(frozen=True, slots=True)
class AutomaticDepositModsDirectorySnapshot:
    instance_root: str
    mods_directory: str
    artifacts: tuple[AutomaticDepositModArtifact, ...]
    mods_directory_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _MODS_DIRECTORY_SNAPSHOT_SEAL:
            raise ValueError("mods directory snapshot must be created by its collector")
        expected = _mods_directory_fingerprint(
            self.instance_root,
            self.mods_directory,
            self.artifacts,
        )
        if self.mods_directory_fingerprint != expected or self._integrity != expected:
            raise ValueError("mods directory snapshot integrity mismatch")


def _create_mods_directory_snapshot(
    instance_root: str,
    mods_directory: str,
    artifacts: tuple[AutomaticDepositModArtifact, ...],
) -> AutomaticDepositModsDirectorySnapshot:
    fingerprint = _mods_directory_fingerprint(
        instance_root,
        mods_directory,
        artifacts,
    )
    return AutomaticDepositModsDirectorySnapshot(
        instance_root=instance_root,
        mods_directory=mods_directory,
        artifacts=artifacts,
        mods_directory_fingerprint=fingerprint,
        _seal=_MODS_DIRECTORY_SNAPSHOT_SEAL,
        _integrity=fingerprint,
    )


def _mods_directory_fingerprint(
    instance_root: str,
    mods_directory: str,
    artifacts: tuple[AutomaticDepositModArtifact, ...],
) -> str:
    payload = {
        "instance_root": instance_root,
        "mods_directory": mods_directory,
        "artifacts": [
            {
                "metadata_status": artifact.metadata_status,
                "mod_id": artifact.mod_id,
                "mtime_ns": artifact.mtime_ns,
                "path": artifact.path,
                "sha256": artifact.sha256,
                "size": artifact.size,
                "version": artifact.version,
            }
            for artifact in artifacts
        ],
    }
    encoded = json.dumps(
        payload,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()
