#20260819_kpopmodder: Project one canonical submit result into the live-run observation contract.
from __future__ import annotations

from typing import Mapping


def apply_submission_result_to_observation(
    observation: dict[str, object],
    result: Mapping[str, object],
) -> dict[str, object]:
    status = result.get("status")
    if not isinstance(status, Mapping):
        return _mark_unknown(observation)

    request_id = status.get("request_id")
    if type(request_id) is str and request_id and request_id == request_id.strip():
        observation["submitted_request_id"] = request_id

    terminal_status = status.get("status")
    details = result.get("details")
    reconciliation_required = (
        isinstance(details, Mapping)
        and details.get("reconciliation_required") is True
    )
    if terminal_status == "unknown" or reconciliation_required:
        return _mark_unknown(observation)
    if result.get("ok") is not True or terminal_status != "accepted":
        observation["submission_outcome"] = "submit_response_not_accepted"
        return observation

    observation["submission_outcome"] = "accepted"
    return observation


def _mark_unknown(observation: dict[str, object]) -> dict[str, object]:
    observation["submission_outcome"] = "submission_outcome_unknown"
    observation["reconciliation_required"] = True
    return observation
