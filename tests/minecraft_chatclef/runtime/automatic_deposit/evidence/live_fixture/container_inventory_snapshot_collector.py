#20260901_kpopmodder: Normalize capacity only for one exact supported block entity.
from __future__ import annotations

import hashlib
import json
import re
from collections.abc import Mapping, Sequence
from types import MappingProxyType

from .anvil_chunk_snapshot import AnvilChunkSnapshot
from .container_inventory_snapshot import (
    TrustedContainerInventorySnapshot,
    _create_container_inventory_snapshot,
)


_CONTAINER_CAPACITIES = MappingProxyType(
    {
        "minecraft:barrel": 27,
        "minecraft:chest": 27,
        "minecraft:trapped_chest": 27,
    }
)
_ITEM_ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+\Z", re.ASCII)


def collect_trusted_container_inventory_snapshot(
    chunk: object,
    *,
    expected_position: tuple[int, int, int],
) -> tuple[TrustedContainerInventorySnapshot | None, str]:
    if not isinstance(chunk, AnvilChunkSnapshot):
        return None, "TRUSTED_CONTAINER_CHUNK_NOT_TYPED"
    if not _valid_position(expected_position):
        return None, "TRUSTED_CONTAINER_POSITION_INVALID"
    raw_entities = chunk.nbt.get("block_entities")
    if not _sequence(raw_entities):
        return None, "TRUSTED_CONTAINER_BLOCK_ENTITIES_INVALID"
    matching: list[Mapping[str, object]] = []
    for raw_entity in raw_entities:
        if not isinstance(raw_entity, Mapping):
            return None, "TRUSTED_CONTAINER_BLOCK_ENTITY_INVALID"
        position = tuple(raw_entity.get(axis) for axis in ("x", "y", "z"))
        if position == expected_position:
            matching.append(raw_entity)
    if len(matching) != 1:
        return None, "TRUSTED_CONTAINER_BLOCK_ENTITY_NOT_EXCLUSIVE"

    entity = matching[0]
    block_entity_id = entity.get("id")
    if block_entity_id not in _CONTAINER_CAPACITIES:
        return None, "TRUSTED_CONTAINER_BLOCK_ENTITY_UNSUPPORTED"
    if "LootTable" in entity:
        return None, "TRUSTED_CONTAINER_LOOT_TABLE_UNRESOLVED"
    raw_items = entity.get("Items", ())
    if not _sequence(raw_items):
        return None, "TRUSTED_CONTAINER_ITEMS_INVALID"

    capacity = _CONTAINER_CAPACITIES[str(block_entity_id)]
    slots: set[int] = set()
    item_counts: dict[str, int] = {}
    normalized_items: list[tuple[int, object]] = []
    for raw_item in raw_items:
        if not isinstance(raw_item, Mapping):
            return None, "TRUSTED_CONTAINER_ITEM_INVALID"
        slot = raw_item.get("Slot")
        count = raw_item.get("Count")
        item_id = raw_item.get("id")
        if (
            isinstance(slot, bool)
            or not isinstance(slot, int)
            or not 0 <= slot < capacity
        ):
            return None, "TRUSTED_CONTAINER_SLOT_INVALID"
        if slot in slots:
            return None, "TRUSTED_CONTAINER_SLOT_DUPLICATE"
        if (
            isinstance(count, bool)
            or not isinstance(count, int)
            or not 1 <= count <= 127
        ):
            return None, "TRUSTED_CONTAINER_ITEM_COUNT_INVALID"
        if (
            not isinstance(item_id, str)
            or len(item_id) > 256
            or _ITEM_ID.fullmatch(item_id) is None
        ):
            return None, "TRUSTED_CONTAINER_ITEM_ID_INVALID"
        slots.add(slot)
        item_counts[item_id] = item_counts.get(item_id, 0) + count
        try:
            normalized_items.append((slot, _json_value(raw_item)))
        except (TypeError, ValueError):
            return None, "TRUSTED_CONTAINER_ITEM_NBT_UNSUPPORTED"

    normalized_items.sort(key=lambda value: value[0])
    fingerprint_payload = json.dumps(
        {
            "block_entity_id": block_entity_id,
            "items": [item for _slot, item in normalized_items],
            "position": list(expected_position),
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    occupied = len(slots)
    return (
        _create_container_inventory_snapshot(
            block_entity_id=str(block_entity_id),
            position=expected_position,
            capacity_slots=capacity,
            occupied_slots=occupied,
            empty_slots=capacity - occupied,
            has_capacity=occupied < capacity,
            item_counts=tuple(sorted(item_counts.items())),
            inventory_fingerprint=hashlib.sha256(fingerprint_payload).hexdigest(),
        ),
        "TRUSTED_CONTAINER_INVENTORY_OBSERVED",
    )


def _valid_position(value: object) -> bool:
    return (
        isinstance(value, tuple)
        and len(value) == 3
        and not any(isinstance(coordinate, bool) for coordinate in value)
        and all(isinstance(coordinate, int) for coordinate in value)
    )


def _sequence(value: object) -> bool:
    return isinstance(value, Sequence) and not isinstance(
        value, (str, bytes, bytearray)
    )


def _json_value(value: object) -> object:
    if isinstance(value, Mapping):
        result: dict[str, object] = {}
        for key, child in value.items():
            if not isinstance(key, str):
                raise TypeError("NBT mapping key is not text")
            result[key] = _json_value(child)
        return result
    if _sequence(value):
        return [_json_value(child) for child in value]
    if isinstance(value, bytes):
        return {"nbt_byte_array_hex": value.hex()}
    if isinstance(value, (str, int, float)) or value is None:
        return value
    raise TypeError("NBT value is not canonicalizable")
