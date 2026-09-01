#20260901_kpopmodder: Seal one exact Anvil chunk payload and region identity.
from __future__ import annotations

from collections.abc import Mapping
from dataclasses import dataclass, field


_ANVIL_CHUNK_SNAPSHOT_SEAL = object()


@dataclass(frozen=True, slots=True)
class AnvilChunkSnapshot:
    region_path: str
    region_sha256: str
    region_size: int
    region_mtime_ns: int
    chunk_x: int
    chunk_z: int
    chunk_timestamp: int
    location_sector_offset: int
    location_sector_count: int
    compression_type: int
    decompressed_size: int
    nbt: Mapping[str, object]
    _seal: object = field(default=None, repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _ANVIL_CHUNK_SNAPSHOT_SEAL:
            raise ValueError("Anvil chunk snapshot must be reader-created")


def _create_anvil_chunk_snapshot(
    *,
    region_path: str,
    region_sha256: str,
    region_size: int,
    region_mtime_ns: int,
    chunk_x: int,
    chunk_z: int,
    chunk_timestamp: int,
    location_sector_offset: int,
    location_sector_count: int,
    compression_type: int,
    decompressed_size: int,
    nbt: Mapping[str, object],
) -> AnvilChunkSnapshot:
    return AnvilChunkSnapshot(
        region_path=region_path,
        region_sha256=region_sha256,
        region_size=region_size,
        region_mtime_ns=region_mtime_ns,
        chunk_x=chunk_x,
        chunk_z=chunk_z,
        chunk_timestamp=chunk_timestamp,
        location_sector_offset=location_sector_offset,
        location_sector_count=location_sector_count,
        compression_type=compression_type,
        decompressed_size=decompressed_size,
        nbt=nbt,
        _seal=_ANVIL_CHUNK_SNAPSHOT_SEAL,
    )
