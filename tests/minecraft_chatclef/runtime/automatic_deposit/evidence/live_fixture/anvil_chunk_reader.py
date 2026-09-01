#20260901_kpopmodder: Read one exact Anvil region chunk with bounded decompression.
from __future__ import annotations

import struct
import zlib
from collections.abc import Callable, Mapping
from pathlib import Path
from types import MappingProxyType

from ....batch.gameplay_oracle.nbt_document_reader import decode_nbt_document
from ..stable_file_digest import read_automatic_deposit_stable_file
from .anvil_chunk_snapshot import AnvilChunkSnapshot, _create_anvil_chunk_snapshot


_SECTOR_BYTES = 4096
_HEADER_BYTES = 8192
_MAX_DECOMPRESSED_CHUNK_BYTES = 16 * 1024 * 1024
_SUPPORTED_COMPRESSION_TYPES = frozenset((1, 2, 3))


def read_stable_anvil_chunk(
    region_path: Path,
    *,
    block_position: tuple[int, int, int],
    minimum_mtime_ns: int | None = None,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
) -> tuple[AnvilChunkSnapshot | None, str]:
    position_problem = _position_problem(block_position, minimum_mtime_ns)
    if position_problem:
        return None, position_problem
    block_x, _block_y, block_z = block_position
    chunk_x = block_x >> 4
    chunk_z = block_z >> 4
    region_x = chunk_x >> 5
    region_z = chunk_z >> 5
    if region_path.name != f"r.{region_x}.{region_z}.mca":
        return None, "ANVIL_REGION_PATH_COORDINATE_MISMATCH"

    digest, payload, stable_reason = read_automatic_deposit_stable_file(
        region_path,
        bytes_reader=bytes_reader,
        stat_reader=stat_reader,
    )
    if digest is None or payload is None:
        return None, f"ANVIL_{stable_reason}"
    if minimum_mtime_ns is not None and digest.mtime_ns < minimum_mtime_ns:
        return None, "ANVIL_REGION_STALE"
    if len(payload) < _HEADER_BYTES or len(payload) % _SECTOR_BYTES:
        return None, "ANVIL_REGION_SIZE_INVALID"

    index = (chunk_x & 31) + ((chunk_z & 31) * 32)
    location_offset = index * 4
    location = payload[location_offset : location_offset + 4]
    sector_offset = int.from_bytes(location[:3], "big")
    sector_count = location[3]
    if sector_offset == 0 and sector_count == 0:
        return None, "ANVIL_CHUNK_LOCATION_MISSING"
    if sector_offset < 2 or sector_count < 1:
        return None, "ANVIL_CHUNK_LOCATION_INVALID"
    sector_end = sector_offset + sector_count
    if sector_end > len(payload) // _SECTOR_BYTES:
        return None, "ANVIL_CHUNK_LOCATION_OUT_OF_BOUNDS"

    record_start = sector_offset * _SECTOR_BYTES
    record_capacity = sector_count * _SECTOR_BYTES
    length = struct.unpack(">I", payload[record_start : record_start + 4])[0]
    if length < 2 or length > record_capacity - 4:
        return None, "ANVIL_CHUNK_LENGTH_INVALID"
    compression_byte = payload[record_start + 4]
    compression_type = compression_byte & 0x7F
    if compression_byte & 0x80:
        return None, "ANVIL_EXTERNAL_CHUNK_UNSUPPORTED"
    if compression_type not in _SUPPORTED_COMPRESSION_TYPES:
        return None, "ANVIL_CHUNK_COMPRESSION_UNSUPPORTED"
    compressed = payload[record_start + 5 : record_start + 4 + length]
    decompressed, decompression_reason = _bounded_decompress(
        compressed, compression_type
    )
    if decompressed is None:
        return None, decompression_reason
    try:
        document = decode_nbt_document(decompressed)
    except (OSError, TypeError, ValueError, zlib.error):
        return None, "ANVIL_CHUNK_NBT_INVALID"
    if (
        _exact_int(document.get("xPos")) != chunk_x
        or _exact_int(document.get("zPos")) != chunk_z
    ):
        return None, "ANVIL_CHUNK_COORDINATE_MISMATCH"
    block_entities = document.get("block_entities")
    if not isinstance(block_entities, list):
        return None, "ANVIL_CHUNK_BLOCK_ENTITIES_MISSING"

    timestamp_offset = _SECTOR_BYTES + index * 4
    timestamp = struct.unpack(">I", payload[timestamp_offset : timestamp_offset + 4])[0]
    frozen = _freeze_nbt(document)
    if not isinstance(frozen, Mapping):
        return None, "ANVIL_CHUNK_NBT_INVALID"
    return (
        _create_anvil_chunk_snapshot(
            region_path=digest.path,
            region_sha256=digest.sha256,
            region_size=digest.size,
            region_mtime_ns=digest.mtime_ns,
            chunk_x=chunk_x,
            chunk_z=chunk_z,
            chunk_timestamp=timestamp,
            location_sector_offset=sector_offset,
            location_sector_count=sector_count,
            compression_type=compression_type,
            decompressed_size=len(decompressed),
            nbt=frozen,
        ),
        "ANVIL_CHUNK_OBSERVED",
    )


def _position_problem(
    position: object,
    minimum_mtime_ns: object,
) -> str:
    if (
        not isinstance(position, tuple)
        or len(position) != 3
        or any(isinstance(value, bool) for value in position)
        or any(not isinstance(value, int) for value in position)
    ):
        return "ANVIL_BLOCK_POSITION_INVALID"
    if minimum_mtime_ns is not None and (
        isinstance(minimum_mtime_ns, bool)
        or not isinstance(minimum_mtime_ns, int)
        or minimum_mtime_ns < 0
    ):
        return "ANVIL_MINIMUM_MTIME_INVALID"
    return ""


def _bounded_decompress(
    payload: bytes,
    compression_type: int,
) -> tuple[bytes | None, str]:
    if compression_type == 3:
        if len(payload) > _MAX_DECOMPRESSED_CHUNK_BYTES:
            return None, "ANVIL_CHUNK_DECOMPRESSED_SIZE_EXCEEDED"
        return payload, ""
    window_bits = 31 if compression_type == 1 else zlib.MAX_WBITS
    try:
        decompressor = zlib.decompressobj(window_bits)
        result = decompressor.decompress(
            payload, _MAX_DECOMPRESSED_CHUNK_BYTES + 1
        )
        if (
            len(result) > _MAX_DECOMPRESSED_CHUNK_BYTES
            or decompressor.unconsumed_tail
        ):
            return None, "ANVIL_CHUNK_DECOMPRESSED_SIZE_EXCEEDED"
        remaining = _MAX_DECOMPRESSED_CHUNK_BYTES + 1 - len(result)
        result += decompressor.flush(remaining)
    except zlib.error:
        return None, "ANVIL_CHUNK_COMPRESSION_INVALID"
    if len(result) > _MAX_DECOMPRESSED_CHUNK_BYTES:
        return None, "ANVIL_CHUNK_DECOMPRESSED_SIZE_EXCEEDED"
    if not decompressor.eof or decompressor.unused_data:
        return None, "ANVIL_CHUNK_COMPRESSION_INVALID"
    return result, ""


def _freeze_nbt(value: object) -> object:
    if isinstance(value, Mapping):
        return MappingProxyType(
            {str(key): _freeze_nbt(child) for key, child in value.items()}
        )
    if isinstance(value, list):
        return tuple(_freeze_nbt(child) for child in value)
    if isinstance(value, (bytes, str, int, float)) or value is None:
        return value
    raise ValueError("unsupported decoded NBT value")


def _exact_int(value: object) -> int | None:
    return value if isinstance(value, int) and not isinstance(value, bool) else None
