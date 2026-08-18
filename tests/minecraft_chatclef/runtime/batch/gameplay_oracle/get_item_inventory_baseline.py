#20260818_kpopmodder: Capture a fresh target-item baseline before one GET submission.
from __future__ import annotations

import time
from collections.abc import Callable, Mapping
from pathlib import Path

from .player_data_path_resolver import resolve_player_data_path
from .player_inventory_snapshot import read_stable_player_inventory_snapshot


def capture_get_item_inventory_baseline(
    environment: Mapping[str, object],
    *,
    path_resolver: Callable[
        [Mapping[str, object]], tuple[Path | None, str]
    ] = resolve_player_data_path,
    snapshot_reader: Callable[[str | Path], Mapping[str, object]] = (
        read_stable_player_inventory_snapshot
    ),
    monotonic: Callable[[], float] = time.monotonic,
    wall_clock_ns: Callable[[], int] = time.time_ns,
    sleeper: Callable[[float], None] = time.sleep,
) -> dict[str, object]:
    item_id = str(environment.get("expected_item_id") or "").strip()
    requested_count = environment.get("expected_item_delta")
    if not item_id.startswith("minecraft:"):
        return _failure("expected GET item id is missing or invalid")
    if not isinstance(requested_count, int) or requested_count <= 0:
        return _failure("expected GET item delta is missing or invalid")
    player_data_path, path_error = path_resolver(environment)
    if path_error or player_data_path is None:
        return _failure(path_error or "playerdata path is unavailable")

    timeout_sec = _positive_number(
        environment.get("gameplay_observation_timeout_sec")
    )
    poll_sec = _positive_number(environment.get("gameplay_observation_poll_sec"))
    max_age_sec = _positive_number(environment.get("gameplay_snapshot_max_age_sec"))
    if timeout_sec is None or poll_sec is None or max_age_sec is None:
        return _failure("gameplay observation timing is invalid")
    collection_started_ns = wall_clock_ns()
    freshness_floor_ns = max(
        collection_started_ns,
        wall_clock_ns() - int(max_age_sec * 1_000_000_000),
    )
    deadline = monotonic() + timeout_sec
    last_reason = "fresh player inventory snapshot was not observed"
    while monotonic() < deadline:
        snapshot = snapshot_reader(player_data_path)
        if snapshot.get("ok") is True:
            mtime_ns = snapshot.get("mtime_ns")
            counts = snapshot.get("inventory_counts")
            if isinstance(mtime_ns, int) and mtime_ns >= freshness_floor_ns:
                if not isinstance(counts, Mapping):
                    return _failure("player inventory counts are unavailable")
                try:
                    target_count_before = _item_count(counts, item_id)
                except ValueError as error:
                    return _failure(str(error))
                return {
                    "ok": True,
                    "reason": "fresh target-item baseline captured",
                    "player_data_path": str(player_data_path),
                    "snapshot_mtime_ns": mtime_ns,
                    "collection_started_ns": collection_started_ns,
                    "target_item_id": item_id,
                    "requested_count": requested_count,
                    "target_count_before": target_count_before,
                }
            last_reason = "playerdata snapshot is stale"
        else:
            last_reason = str(snapshot.get("reason") or "playerdata snapshot failed")
        sleeper(poll_sec)
    return _failure(f"fresh baseline timeout: {last_reason}")


def _item_count(counts: Mapping[object, object], item_id: str) -> int:
    value = counts.get(item_id, 0)
    if not isinstance(value, int) or value < 0:
        raise ValueError("target item count is invalid")
    return value


def _positive_number(value: object) -> float | None:
    if isinstance(value, bool):
        return None
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        return None
    return parsed if parsed > 0 else None


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason}
