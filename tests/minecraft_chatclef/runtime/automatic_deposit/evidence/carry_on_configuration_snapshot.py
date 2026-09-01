#20260831_kpopmodder: Seal stable Carry On key-binding and config file digests.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field

from .stable_file_digest import AutomaticDepositStableFileDigest


_CARRY_ON_CONFIGURATION_SNAPSHOT_SEAL = object()


@dataclass(frozen=True, slots=True)
class AutomaticDepositCarryOnConfigurationSnapshot:
    instance_root: str
    files: tuple[AutomaticDepositStableFileDigest, ...]
    fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _CARRY_ON_CONFIGURATION_SNAPSHOT_SEAL:
            raise ValueError("Carry On configuration snapshot must be collected")
        expected = _configuration_fingerprint(self.instance_root, self.files)
        if self.fingerprint != expected or self._integrity != expected:
            raise ValueError("Carry On configuration snapshot integrity mismatch")


def _create_carry_on_configuration_snapshot(
    instance_root: str,
    files: tuple[AutomaticDepositStableFileDigest, ...],
) -> AutomaticDepositCarryOnConfigurationSnapshot:
    fingerprint = _configuration_fingerprint(instance_root, files)
    return AutomaticDepositCarryOnConfigurationSnapshot(
        instance_root=instance_root,
        files=files,
        fingerprint=fingerprint,
        _seal=_CARRY_ON_CONFIGURATION_SNAPSHOT_SEAL,
        _integrity=fingerprint,
    )


def _configuration_fingerprint(
    instance_root: str,
    files: tuple[AutomaticDepositStableFileDigest, ...],
) -> str:
    payload = {
        "files": [
            {
                "mtime_ns": item.mtime_ns,
                "path": item.path,
                "sha256": item.sha256,
                "size": item.size,
            }
            for item in files
        ],
        "instance_root": instance_root,
    }
    encoded = json.dumps(
        payload,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()
