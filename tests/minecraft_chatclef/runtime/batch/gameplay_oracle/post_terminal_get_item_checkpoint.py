#20260818_kpopmodder: Verify the target-item delta from a post-terminal player save.
from __future__ import annotations

import time
from collections.abc import Callable, Mapping
from pathlib import Path

from .get_item_delta_oracle import get_item_delta_checkpoint
from .player_inventory_snapshot import read_stable_player_inventory_snapshot


def collect_post_terminal_get_item_checkpoint(
    environment: Mapping[str, object],
    _command_result: Mapping[str, object],
    baseline: object,
    *,
    snapshot_reader: Callable[[str | Path], Mapping[str, object]] = (
        read_stable_player_inventory_snapshot
    ),
    monotonic: Callable[[], float] = time.monotonic,
    wall_clock_ns: Callable[[], int] = time.time_ns,
    sleeper: Callable[[float], None] = time.sleep,
) -> dict[str, object]:
    if not isinstance(baseline, Mapping) or baseline.get("ok") is not True:
        return _unknown_checkpoint("valid gameplay baseline is missing")
    path = str(baseline.get("player_data_path") or "").strip()
    item_id = str(baseline.get("target_item_id") or "").strip()
    before_count = baseline.get("target_count_before")
    requested_count = baseline.get("requested_count")
    baseline_mtime_ns = baseline.get("snapshot_mtime_ns")
    if (
        not path
        or not item_id.startswith("minecraft:")
        or not isinstance(before_count, int)
        or before_count < 0
        or not isinstance(requested_count, int)
        or requested_count <= 0
        or not isinstance(baseline_mtime_ns, int)
    ):
        return _unknown_checkpoint("gameplay baseline fields are invalid")

    timeout_sec = _positive_number(
        environment.get("gameplay_observation_timeout_sec")
    )
    poll_sec = _positive_number(environment.get("gameplay_observation_poll_sec"))
    if timeout_sec is None or poll_sec is None:
        return _unknown_checkpoint("gameplay observation timing is invalid")
    post_terminal_floor_ns = max(baseline_mtime_ns, wall_clock_ns())
    deadline = monotonic() + timeout_sec
    last_reason = "post-terminal player save was not observed"
    while monotonic() < deadline:
        snapshot = snapshot_reader(path)
        if snapshot.get("ok") is True:
            mtime_ns = snapshot.get("mtime_ns")
            counts = snapshot.get("inventory_counts")
            if isinstance(mtime_ns, int) and mtime_ns > post_terminal_floor_ns:
                if not isinstance(counts, Mapping):
                    return _unknown_checkpoint("post-terminal inventory is unavailable")
                try:
                    after_count = _item_count(counts, item_id)
                except ValueError as error:
                    return _unknown_checkpoint(str(error))
                return get_item_delta_checkpoint(
                    before_count=before_count,
                    after_count=after_count,
                    requested_count=requested_count,
                )
            last_reason = "playerdata has no save newer than terminal observation"
        else:
            last_reason = str(snapshot.get("reason") or "playerdata snapshot failed")
        sleeper(poll_sec)
    return _unknown_checkpoint(f"post-terminal inventory timeout: {last_reason}")


def _unknown_checkpoint(reason: str) -> dict[str, object]:
    return {
        "reason": reason,
        "gameplay_oracle_scope": "saved_player_target_inventory",
        "gameplay_observation_complete": False,
        "gameplay_effect_observed": None,
        "expected_gameplay_effect_verified": None,
        "partial_gameplay_effect_observed": None,
        "unexpected_effect_observed": None,
        "prohibited_effect_absence_verified": None,
    }


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
