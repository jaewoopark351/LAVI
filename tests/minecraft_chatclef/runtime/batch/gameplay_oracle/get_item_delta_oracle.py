#20260818_kpopmodder: Classify only the saved target-item inventory delta.
from __future__ import annotations


def get_item_delta_checkpoint(
    *,
    before_count: int,
    after_count: int,
    requested_count: int,
) -> dict[str, object]:
    if min(before_count, after_count) < 0 or requested_count <= 0:
        raise ValueError("GET item delta inputs are invalid")
    delta = after_count - before_count
    expected_verified = delta >= requested_count
    partial_observed = 0 < delta < requested_count
    return {
        "reason": (
            "target inventory delta verified; broader gameplay observation incomplete"
            if expected_verified
            else "target inventory delta did not meet the requested amount; broader gameplay observation incomplete"
        ),
        "gameplay_oracle_scope": "saved_player_target_inventory",
        "target_count_before": before_count,
        "target_count_after": after_count,
        "expected_item_delta": requested_count,
        "observed_item_delta": delta,
        "gameplay_observation_complete": False,
        "gameplay_effect_observed": delta != 0,
        "expected_gameplay_effect_verified": expected_verified,
        "partial_gameplay_effect_observed": partial_observed,
        "unexpected_effect_observed": None,
        "prohibited_effect_absence_verified": None,
    }
