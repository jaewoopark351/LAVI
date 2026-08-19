#20260819_kpopmodder: Decide whether terminal and gameplay evidence permit the next approved batch step.
from __future__ import annotations

from collections.abc import Mapping


def batch_advancement_error(
    command_result: Mapping[str, object],
    checkpoint_result: object,
) -> str:
    observation = _mapping(command_result.get("observation"))
    if observation.get("terminal_status") != "completed":
        return "terminal_status_not_completed"
    if observation.get("runtime_reported_completion") is not True:
        return "runtime_completion_not_verified"
    if not isinstance(checkpoint_result, Mapping):
        return "gameplay_checkpoint_result_invalid"
    if checkpoint_result.get("ok") is not True:
        return str(
            checkpoint_result.get("reason")
            or "gameplay checkpoint did not prove end-to-end success"
        )
    if observation.get("gameplay_observation_complete") is not True:
        return "gameplay_observation_not_complete"
    if observation.get("gameplay_effect_observed") is not True:
        return "gameplay_effect_not_verified"
    if observation.get("expected_gameplay_effect_verified") is not True:
        return "expected_gameplay_effect_not_verified"
    if observation.get("partial_gameplay_effect_observed") is not False:
        return "partial_gameplay_effect_not_excluded"
    if observation.get("unexpected_effect_observed") is not False:
        return "unexpected_gameplay_effect_not_excluded"
    if observation.get("prohibited_effect_absence_verified") is not True:
        return "prohibited_effect_absence_not_verified"
    if observation.get("end_to_end_success") is not True:
        return "end_to_end_success_not_verified"
    return ""


def _mapping(value: object) -> dict[str, object]:
    return dict(value) if isinstance(value, Mapping) else {}
