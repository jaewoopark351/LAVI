#20260901_kpopmodder: Read LevelName from one stable level.dat without mutation.
from __future__ import annotations

from collections.abc import Callable, Mapping
from pathlib import Path

from ....batch.gameplay_oracle.nbt_document_reader import decode_nbt_document
from ..stable_file_digest import read_automatic_deposit_stable_file
from .world_identity_snapshot import (
    SavedWorldIdentitySnapshot,
    _create_saved_world_identity_snapshot,
)


def read_saved_world_identity(
    level_dat_path: Path,
    *,
    expected_world_key: str,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
) -> tuple[SavedWorldIdentitySnapshot | None, str]:
    if (
        not isinstance(expected_world_key, str)
        or not expected_world_key.startswith("singleplayer:")
        or not expected_world_key.removeprefix("singleplayer:").strip()
        or expected_world_key != expected_world_key.strip()
    ):
        return None, "SAVED_WORLD_EXPECTED_KEY_INVALID"
    digest, payload, stable_reason = read_automatic_deposit_stable_file(
        level_dat_path,
        bytes_reader=bytes_reader,
        stat_reader=stat_reader,
    )
    if digest is None or payload is None:
        return None, f"SAVED_WORLD_{stable_reason}"
    try:
        document = decode_nbt_document(payload)
    except (OSError, TypeError, ValueError):
        return None, "SAVED_WORLD_LEVEL_DAT_NBT_INVALID"
    data = document.get("Data")
    if not isinstance(data, Mapping):
        return None, "SAVED_WORLD_DATA_COMPOUND_MISSING"
    level_name = data.get("LevelName")
    if (
        not isinstance(level_name, str)
        or not level_name.strip()
        or level_name != level_name.strip()
        or len(level_name) > 512
    ):
        return None, "SAVED_WORLD_LEVEL_NAME_INVALID"
    world_key = f"singleplayer:{level_name}"
    if world_key != expected_world_key:
        return None, "SAVED_WORLD_IDENTITY_MISMATCH"
    return (
        _create_saved_world_identity_snapshot(
            world_key=world_key,
            level_name=level_name,
            level_dat_path=digest.path,
            level_dat_sha256=digest.sha256,
            level_dat_size=digest.size,
            level_dat_mtime_ns=digest.mtime_ns,
        ),
        "SAVED_WORLD_IDENTITY_OBSERVED",
    )
