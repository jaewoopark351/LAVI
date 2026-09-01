#20260901_kpopmodder: Bind the existing stable player snapshot to a stable file SHA.
from __future__ import annotations

import hashlib
import json
from collections.abc import Callable, Mapping
from pathlib import Path

from ....batch.gameplay_oracle.player_inventory_snapshot import (
    read_stable_player_inventory_snapshot,
)
from ..stable_file_digest import read_automatic_deposit_stable_file_digest
from .player_inventory_observation import (
    SavedPlayerInventoryObservation,
    _create_saved_player_inventory_observation,
)


def observe_saved_player_inventory(
    player_data_path: Path,
    *,
    minimum_mtime_ns: int | None = None,
    attempts: int = 3,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
) -> tuple[SavedPlayerInventoryObservation | None, str]:
    if isinstance(attempts, bool) or not isinstance(attempts, int) or not 1 <= attempts <= 5:
        return None, "PLAYER_INVENTORY_ATTEMPTS_INVALID"
    if minimum_mtime_ns is not None and (
        isinstance(minimum_mtime_ns, bool)
        or not isinstance(minimum_mtime_ns, int)
        or minimum_mtime_ns < 0
    ):
        return None, "PLAYER_INVENTORY_MINIMUM_MTIME_INVALID"
    snapshot = read_stable_player_inventory_snapshot(
        player_data_path,
        attempts=attempts,
        byte_reader=bytes_reader,
        stat_reader=stat_reader,
        sleeper=lambda _seconds: None,
    )
    if snapshot.get("ok") is not True:
        return None, f"PLAYER_INVENTORY_SNAPSHOT_FAILED:{_bounded_reason(snapshot.get('reason'))}"
    digest, digest_reason = read_automatic_deposit_stable_file_digest(
        player_data_path,
        bytes_reader=bytes_reader,
        stat_reader=stat_reader,
    )
    if digest is None:
        return None, f"PLAYER_INVENTORY_{digest_reason}"
    if (
        snapshot.get("size") != digest.size
        or snapshot.get("mtime_ns") != digest.mtime_ns
    ):
        return None, "PLAYER_INVENTORY_CHANGED_BETWEEN_OBSERVATIONS"
    if minimum_mtime_ns is not None and digest.mtime_ns < minimum_mtime_ns:
        return None, "PLAYER_DATA_STALE"
    raw_counts = snapshot.get("inventory_counts")
    if not isinstance(raw_counts, Mapping):
        return None, "PLAYER_INVENTORY_COUNTS_INVALID"
    counts: list[tuple[str, int]] = []
    for item_id, count in raw_counts.items():
        if (
            not isinstance(item_id, str)
            or not item_id
            or isinstance(count, bool)
            or not isinstance(count, int)
            or count < 0
        ):
            return None, "PLAYER_INVENTORY_COUNTS_INVALID"
        counts.append((item_id, count))
    normalized_counts = tuple(sorted(counts))
    fingerprint_payload = json.dumps(
        {
            "file_sha256": digest.sha256,
            "inventory_counts": normalized_counts,
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return (
        _create_saved_player_inventory_observation(
            player_data_path=digest.path,
            player_data_sha256=digest.sha256,
            player_data_size=digest.size,
            player_data_mtime_ns=digest.mtime_ns,
            inventory_counts=normalized_counts,
            inventory_fingerprint=hashlib.sha256(fingerprint_payload).hexdigest(),
        ),
        "PLAYER_INVENTORY_OBSERVED",
    )


def _bounded_reason(value: object) -> str:
    text = str(value or "")
    return text if text and len(text) <= 160 else "UNAVAILABLE"
