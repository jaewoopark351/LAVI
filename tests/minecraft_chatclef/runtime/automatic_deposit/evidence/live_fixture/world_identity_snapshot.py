#20260901_kpopmodder: Seal one saved single-player world identity snapshot.
from __future__ import annotations

from dataclasses import dataclass, field


_WORLD_IDENTITY_SNAPSHOT_SEAL = object()


@dataclass(frozen=True, slots=True)
class SavedWorldIdentitySnapshot:
    world_key: str
    level_name: str
    level_dat_path: str
    level_dat_sha256: str
    level_dat_size: int
    level_dat_mtime_ns: int
    _seal: object = field(default=None, repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _WORLD_IDENTITY_SNAPSHOT_SEAL:
            raise ValueError("saved world identity must be reader-created")


def _create_saved_world_identity_snapshot(
    *,
    world_key: str,
    level_name: str,
    level_dat_path: str,
    level_dat_sha256: str,
    level_dat_size: int,
    level_dat_mtime_ns: int,
) -> SavedWorldIdentitySnapshot:
    return SavedWorldIdentitySnapshot(
        world_key=world_key,
        level_name=level_name,
        level_dat_path=level_dat_path,
        level_dat_sha256=level_dat_sha256,
        level_dat_size=level_dat_size,
        level_dat_mtime_ns=level_dat_mtime_ns,
        _seal=_WORLD_IDENTITY_SNAPSHOT_SEAL,
    )
