#20260818_kpopmodder: Evaluate gameplay evidence without conflating runtime completion.
from __future__ import annotations

from typing import Mapping


def evaluate_end_to_end_success(
    observation: Mapping[str, object],
) -> bool | None:
    runtime_completed = _tri_state(observation.get("runtime_reported_completion"))
    complete = _tri_state(observation.get("gameplay_observation_complete"))
    expected = _tri_state(observation.get("expected_gameplay_effect_verified"))
    prohibited_absent = _tri_state(
        observation.get("prohibited_effect_absence_verified")
    )
    partial = _tri_state(observation.get("partial_gameplay_effect_observed"))
    unexpected = _tri_state(observation.get("unexpected_effect_observed"))
    if runtime_completed is False or expected is False or prohibited_absent is False:
        return False
    if partial is True or unexpected is True:
        return False
    if (
        runtime_completed is True
        and complete is True
        and expected is True
        and prohibited_absent is True
    ):
        return True
    return None


def apply_gameplay_evidence(
    observation: dict[str, object],
    *,
    observation_complete: bool | None,
    effect_observed: bool | None,
    expected_effect_verified: bool | None,
    partial_effect_observed: bool | None,
    unexpected_effect_observed: bool | None,
    prohibited_effect_absence_verified: bool | None,
) -> dict[str, object]:
    observation.update(
        {
            "gameplay_observation_complete": observation_complete,
            "gameplay_effect_observed": effect_observed,
            "expected_gameplay_effect_verified": expected_effect_verified,
            "partial_gameplay_effect_observed": partial_effect_observed,
            "unexpected_effect_observed": unexpected_effect_observed,
            "prohibited_effect_absence_verified": prohibited_effect_absence_verified,
        }
    )
    observation["end_to_end_success"] = evaluate_end_to_end_success(observation)
    if (
        observation_complete is not True
        or partial_effect_observed is True
        or unexpected_effect_observed is True
    ):
        observation["reconciliation_required"] = True
    return observation


def _tri_state(value: object) -> bool | None:
    return value if isinstance(value, bool) else None
