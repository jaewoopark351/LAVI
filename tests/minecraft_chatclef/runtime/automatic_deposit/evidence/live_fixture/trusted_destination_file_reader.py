#20260901_kpopmodder: Parse one stable exact-world trusted destination fail closed.
from __future__ import annotations

import hashlib
import json
from collections.abc import Callable, Mapping
from pathlib import Path

from ..stable_file_digest import read_automatic_deposit_stable_file
from .trusted_destination_snapshot import (
    TrustedDestinationSnapshot,
    _create_trusted_destination_snapshot,
)


_ALLOWED_DIMENSIONS = frozenset(("OVERWORLD", "NETHER", "END"))
_ROOT_KEYS = frozenset(("schemaVersion", "destinations"))
_ENTRY_KEYS = frozenset(("worldKey", "dimension", "x", "y", "z", "enabled"))
_MAX_DESTINATIONS = 256
_MAX_WORLD_KEY_LENGTH = 512
_MIN_HORIZONTAL = -30_000_000
_MAX_HORIZONTAL = 30_000_000
_MIN_VERTICAL = -(2**31)
_MAX_VERTICAL = 2**31 - 1


def read_exact_trusted_destination(
    path: Path,
    *,
    expected_world_key: str,
    expected_dimension: str,
    minimum_mtime_ns: int | None = None,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
) -> tuple[TrustedDestinationSnapshot | None, str]:
    expected_problem = _expected_identity_problem(
        expected_world_key, expected_dimension, minimum_mtime_ns
    )
    if expected_problem:
        return None, expected_problem
    digest, payload, stable_reason = read_automatic_deposit_stable_file(
        path,
        bytes_reader=bytes_reader,
        stat_reader=stat_reader,
    )
    if digest is None or payload is None:
        return None, f"TRUSTED_DESTINATION_{stable_reason}"
    if minimum_mtime_ns is not None and digest.mtime_ns < minimum_mtime_ns:
        return None, "TRUSTED_DESTINATION_REGISTRY_STALE"
    try:
        root = json.loads(
            payload.decode("utf-8", errors="strict"),
            object_pairs_hook=_unique_object,
        )
    except (UnicodeDecodeError, json.JSONDecodeError, ValueError):
        return None, "TRUSTED_DESTINATION_JSON_INVALID"
    if not isinstance(root, Mapping):
        return None, "TRUSTED_DESTINATION_JSON_INVALID"
    schema_version = root.get("schemaVersion")
    if (
        set(root) != _ROOT_KEYS
        or isinstance(schema_version, bool)
        or not isinstance(schema_version, int)
        or schema_version != 1
    ):
        return None, "TRUSTED_DESTINATION_SCHEMA_INVALID"
    raw_destinations = root.get("destinations")
    if (
        not isinstance(raw_destinations, list)
        or len(raw_destinations) > _MAX_DESTINATIONS
    ):
        return None, "TRUSTED_DESTINATION_COLLECTION_INVALID"

    identities: set[str] = set()
    matching: list[tuple[str, str, tuple[int, int, int]]] = []
    for raw_destination in raw_destinations:
        parsed = _parse_destination(raw_destination)
        if parsed is None:
            return None, "TRUSTED_DESTINATION_ENTRY_INVALID"
        world_key, dimension, position, enabled = parsed
        identity = _canonical_identity(world_key, dimension, position)
        if identity in identities:
            return None, "TRUSTED_DESTINATION_DUPLICATE_IDENTITY"
        identities.add(identity)
        if enabled and world_key == expected_world_key and dimension == expected_dimension:
            matching.append((identity, world_key, position))
    if len(matching) != 1:
        return None, "TRUSTED_DESTINATION_NOT_EXCLUSIVE"

    identity, world_key, position = matching[0]
    fingerprint = hashlib.sha256(identity.encode("utf-8")).hexdigest()
    return (
        _create_trusted_destination_snapshot(
            world_key=world_key,
            dimension=expected_dimension,
            position=position,
            destination_id=f"td_{fingerprint[:24]}",
            destination_fingerprint=fingerprint,
            registry_path=digest.path,
            registry_sha256=digest.sha256,
            registry_size=digest.size,
            registry_mtime_ns=digest.mtime_ns,
        ),
        "TRUSTED_DESTINATION_OBSERVED",
    )


def _expected_identity_problem(
    world_key: object,
    dimension: object,
    minimum_mtime_ns: object,
) -> str:
    if (
        not isinstance(world_key, str)
        or not world_key.strip()
        or world_key != world_key.strip()
        or len(world_key) > _MAX_WORLD_KEY_LENGTH
        or _has_control(world_key)
    ):
        return "TRUSTED_DESTINATION_EXPECTED_WORLD_INVALID"
    if dimension not in _ALLOWED_DIMENSIONS:
        return "TRUSTED_DESTINATION_EXPECTED_DIMENSION_INVALID"
    if minimum_mtime_ns is not None and (
        isinstance(minimum_mtime_ns, bool)
        or not isinstance(minimum_mtime_ns, int)
        or minimum_mtime_ns < 0
    ):
        return "TRUSTED_DESTINATION_MINIMUM_MTIME_INVALID"
    return ""


def _parse_destination(
    value: object,
) -> tuple[str, str, tuple[int, int, int], bool] | None:
    if not isinstance(value, Mapping) or set(value) != _ENTRY_KEYS:
        return None
    world_key = value.get("worldKey")
    dimension = value.get("dimension")
    enabled = value.get("enabled")
    coordinates = (value.get("x"), value.get("y"), value.get("z"))
    if (
        not isinstance(world_key, str)
        or not world_key.strip()
        or world_key != world_key.strip()
        or len(world_key) > _MAX_WORLD_KEY_LENGTH
        or _has_control(world_key)
        or dimension not in _ALLOWED_DIMENSIONS
        or not isinstance(enabled, bool)
        or any(isinstance(coordinate, bool) for coordinate in coordinates)
        or any(not isinstance(coordinate, int) for coordinate in coordinates)
    ):
        return None
    x, y, z = coordinates
    assert isinstance(x, int) and isinstance(y, int) and isinstance(z, int)
    if not (
        _MIN_HORIZONTAL <= x <= _MAX_HORIZONTAL
        and _MIN_VERTICAL <= y <= _MAX_VERTICAL
        and _MIN_HORIZONTAL <= z <= _MAX_HORIZONTAL
    ):
        return None
    return world_key, str(dimension), (x, y, z), enabled


def _canonical_identity(
    world_key: str,
    dimension: str,
    position: tuple[int, int, int],
) -> str:
    return "|".join((world_key, dimension, *(str(value) for value in position)))


def _unique_object(pairs: list[tuple[str, object]]) -> dict[str, object]:
    result: dict[str, object] = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("duplicate JSON object key")
        result[key] = value
    return result


def _has_control(value: str) -> bool:
    return any(ord(character) < 0x20 or ord(character) == 0x7F for character in value)
