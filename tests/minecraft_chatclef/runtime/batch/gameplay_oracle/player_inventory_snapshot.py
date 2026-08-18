#20260818_kpopmodder: Read one stable saved-player inventory snapshot without mutation.
from __future__ import annotations

import time
from collections.abc import Callable, Mapping
from pathlib import Path

from .nbt_document_reader import decode_nbt_document
from .player_inventory_counts import extract_player_inventory_counts


def read_stable_player_inventory_snapshot(
    player_data_path: str | Path,
    *,
    attempts: int = 3,
    byte_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
    document_reader: Callable[[bytes], Mapping[str, object]] = decode_nbt_document,
    sleeper: Callable[[float], None] = time.sleep,
) -> dict[str, object]:
    path = Path(player_data_path)
    read_bytes = byte_reader or _read_bytes
    read_stat = stat_reader or _read_stat
    last_reason = "playerdata snapshot was not attempted"
    for attempt in range(max(1, attempts)):
        try:
            before = read_stat(path)
            raw_payload = read_bytes(path)
            after = read_stat(path)
        except OSError as error:
            last_reason = f"playerdata read failed: {type(error).__name__}"
        else:
            try:
                before_identity = _stat_identity(before)
                after_identity = _stat_identity(after)
            except (TypeError, ValueError) as error:
                return _failure(
                    f"playerdata stat validation failed: {type(error).__name__}"
                )
            if before_identity != after_identity:
                last_reason = "playerdata changed during snapshot"
            elif len(raw_payload) != after_identity[0]:
                last_reason = "playerdata snapshot size did not match stat"
            else:
                try:
                    document = document_reader(raw_payload)
                    counts = extract_player_inventory_counts(document)
                except (OSError, TypeError, ValueError) as error:
                    return _failure(
                        f"playerdata NBT decode failed: {type(error).__name__}"
                    )
                return {
                    "ok": True,
                    "reason": "stable player inventory snapshot",
                    "player_data_path": str(path),
                    "size": after_identity[0],
                    "mtime_ns": after_identity[1],
                    "inventory_counts": counts,
                }
        if attempt + 1 < max(1, attempts):
            sleeper(0.05)
    return _failure(last_reason)


def _stat_identity(stat_result: object) -> tuple[int, int]:
    size = getattr(stat_result, "st_size", None)
    mtime_ns = getattr(stat_result, "st_mtime_ns", None)
    if not isinstance(size, int) or not isinstance(mtime_ns, int):
        raise ValueError("playerdata stat is incomplete")
    return size, mtime_ns


def _read_bytes(path: Path) -> bytes:
    return path.read_bytes()


def _read_stat(path: Path) -> object:
    return path.stat()


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason}
