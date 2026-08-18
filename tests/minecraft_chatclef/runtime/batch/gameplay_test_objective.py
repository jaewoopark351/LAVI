#20260819_kpopmodder: Admit only the currently observable live GET test objective.
from __future__ import annotations


GET_ACQUISITION_DELTA_OBJECTIVE = "get_acquisition_delta"
MOVEMENT_AND_MINING_OBJECTIVE = "movement_and_mining"


def validate_gameplay_test_objective(value: object) -> str:
    if (
        type(value) is not str
        or not value
        or value != value.strip()
    ):
        return "gameplay_test_objective_invalid"
    if value == GET_ACQUISITION_DELTA_OBJECTIVE:
        return ""
    if value == MOVEMENT_AND_MINING_OBJECTIVE:
        return "movement_and_mining_observer_unavailable"
    return "gameplay_test_objective_unsupported"
