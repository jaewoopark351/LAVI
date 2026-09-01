#20260901_kpopmodder: Build bounded hermetic Minecraft save fixtures for observer tests.
from __future__ import annotations

import gzip
import json
import math
import struct
import zlib
from pathlib import Path


SECTOR_BYTES = 4096


def write_trusted_destinations(
    path: Path,
    destinations: list[dict[str, object]],
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(
            {"schemaVersion": 1, "destinations": destinations},
            ensure_ascii=False,
            separators=(",", ":"),
        ),
        encoding="utf-8",
    )


def trusted_destination(
    *,
    world_key: str = "singleplayer:P1 fixture",
    dimension: str = "OVERWORLD",
    x: int = 12,
    y: int = 64,
    z: int = -9,
    enabled: bool = True,
) -> dict[str, object]:
    return {
        "worldKey": world_key,
        "dimension": dimension,
        "x": x,
        "y": y,
        "z": z,
        "enabled": enabled,
    }


def write_level_dat(path: Path, level_name: str) -> None:
    root = _compound_payload(
        (
            _named_compound(
                "Data",
                (_named_string("LevelName", level_name),),
            ),
        )
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(b"\x0a\x00\x00" + root, mtime=0))


def write_player_dat(
    path: Path,
    items: tuple[tuple[int, str, int], ...],
) -> None:
    entries = tuple(
        _compound_payload(
            (
                _named_byte("Slot", slot),
                _named_string("id", item_id),
                _named_byte("Count", count),
            )
        )
        for slot, item_id, count in items
    )
    root = _compound_payload((_named_compound_list("Inventory", entries),))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(b"\x0a\x00\x00" + root, mtime=0))


def chunk_nbt(
    *,
    chunk_x: int,
    chunk_z: int,
    block_entities: tuple[bytes, ...],
) -> bytes:
    root = _compound_payload(
        (
            _named_int("xPos", chunk_x),
            _named_int("zPos", chunk_z),
            _named_compound_list("block_entities", block_entities),
        )
    )
    return b"\x0a\x00\x00" + root


def container_block_entity(
    *,
    block_entity_id: str = "minecraft:barrel",
    x: int = 12,
    y: int = 64,
    z: int = -9,
    items: tuple[tuple[int, str, int], ...] = ((0, "minecraft:cobblestone", 32),),
    extra_named_tags: tuple[bytes, ...] = (),
) -> bytes:
    item_entries = tuple(
        _compound_payload(
            (
                _named_byte("Slot", slot),
                _named_string("id", item_id),
                _named_byte("Count", count),
            )
        )
        for slot, item_id, count in items
    )
    return _compound_payload(
        (
            _named_string("id", block_entity_id),
            _named_int("x", x),
            _named_int("y", y),
            _named_int("z", z),
            _named_compound_list("Items", item_entries),
            *extra_named_tags,
        )
    )


def named_string(name: str, value: str) -> bytes:
    return _named_string(name, value)


def write_region_file(
    path: Path,
    *,
    chunk_x: int,
    chunk_z: int,
    nbt_payload: bytes,
    compression_type: int = 2,
    timestamp: int = 1_777_777_777,
) -> None:
    if compression_type == 1:
        stored_payload = gzip.compress(nbt_payload, mtime=0)
    elif compression_type == 2:
        stored_payload = zlib.compress(nbt_payload)
    elif compression_type == 3:
        stored_payload = nbt_payload
    else:
        stored_payload = nbt_payload
    chunk_record = (
        struct.pack(">I", len(stored_payload) + 1)
        + bytes((compression_type,))
        + stored_payload
    )
    sector_count = math.ceil(len(chunk_record) / SECTOR_BYTES)
    header = bytearray(SECTOR_BYTES * 2)
    index = (chunk_x & 31) + ((chunk_z & 31) * 32)
    header[index * 4 : index * 4 + 4] = (2).to_bytes(3, "big") + bytes(
        (sector_count,)
    )
    timestamp_offset = SECTOR_BYTES + index * 4
    header[timestamp_offset : timestamp_offset + 4] = struct.pack(
        ">I", timestamp
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(
        bytes(header) + chunk_record.ljust(sector_count * SECTOR_BYTES, b"\x00")
    )


def _named_byte(name: str, value: int) -> bytes:
    return b"\x01" + _nbt_string(name) + struct.pack(">b", value)


def _named_int(name: str, value: int) -> bytes:
    return b"\x03" + _nbt_string(name) + struct.pack(">i", value)


def _named_string(name: str, value: str) -> bytes:
    return b"\x08" + _nbt_string(name) + _nbt_string(value)


def _named_compound(name: str, named_tags: tuple[bytes, ...]) -> bytes:
    return b"\x0a" + _nbt_string(name) + _compound_payload(named_tags)


def _named_compound_list(name: str, compounds: tuple[bytes, ...]) -> bytes:
    return (
        b"\x09"
        + _nbt_string(name)
        + b"\x0a"
        + struct.pack(">i", len(compounds))
        + b"".join(compounds)
    )


def _compound_payload(named_tags: tuple[bytes, ...]) -> bytes:
    return b"".join(named_tags) + b"\x00"


def _nbt_string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded
