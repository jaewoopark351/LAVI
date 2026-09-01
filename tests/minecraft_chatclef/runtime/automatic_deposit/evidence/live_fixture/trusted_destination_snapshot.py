#20260901_kpopmodder: Seal one exact trusted-destination registry observation.
from __future__ import annotations

import hashlib
from dataclasses import dataclass, field


_TRUSTED_DESTINATION_SNAPSHOT_SEAL = object()


@dataclass(frozen=True, slots=True)
class TrustedDestinationSnapshot:
    world_key: str
    dimension: str
    position: tuple[int, int, int]
    destination_id: str
    destination_fingerprint: str
    registry_path: str
    registry_sha256: str
    registry_size: int
    registry_mtime_ns: int
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = _integrity_value(
            world_key=self.world_key,
            dimension=self.dimension,
            position=self.position,
            destination_id=self.destination_id,
            destination_fingerprint=self.destination_fingerprint,
            registry_sha256=self.registry_sha256,
        )
        if self._seal is not _TRUSTED_DESTINATION_SNAPSHOT_SEAL:
            raise ValueError("trusted destination snapshot must be observer-created")
        if self._integrity != expected:
            raise ValueError("trusted destination snapshot integrity mismatch")


def _create_trusted_destination_snapshot(
    *,
    world_key: str,
    dimension: str,
    position: tuple[int, int, int],
    destination_id: str,
    destination_fingerprint: str,
    registry_path: str,
    registry_sha256: str,
    registry_size: int,
    registry_mtime_ns: int,
) -> TrustedDestinationSnapshot:
    integrity = _integrity_value(
        world_key=world_key,
        dimension=dimension,
        position=position,
        destination_id=destination_id,
        destination_fingerprint=destination_fingerprint,
        registry_sha256=registry_sha256,
    )
    return TrustedDestinationSnapshot(
        world_key=world_key,
        dimension=dimension,
        position=position,
        destination_id=destination_id,
        destination_fingerprint=destination_fingerprint,
        registry_path=registry_path,
        registry_sha256=registry_sha256,
        registry_size=registry_size,
        registry_mtime_ns=registry_mtime_ns,
        _seal=_TRUSTED_DESTINATION_SNAPSHOT_SEAL,
        _integrity=integrity,
    )


def _integrity_value(
    *,
    world_key: str,
    dimension: str,
    position: tuple[int, int, int],
    destination_id: str,
    destination_fingerprint: str,
    registry_sha256: str,
) -> str:
    payload = "\x1f".join(
        (
            world_key,
            dimension,
            ",".join(str(value) for value in position),
            destination_id,
            destination_fingerprint,
            registry_sha256,
        )
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
