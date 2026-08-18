#20260819_kpopmodder: Apply one validated terminal snapshot without owning polling.
from __future__ import annotations

from collections.abc import Mapping


def apply_terminal_result_snapshot(
    observation: dict[str, object],
    commands: Mapping[str, object],
    result: Mapping[str, object],
    request_id: str,
) -> dict[str, object]:
    terminal_status = result["status"]
    active_clear = _active_request_clear(commands)
    observation.update(
        {
            "terminal_request_id": request_id,
            "terminal_status": terminal_status,
            "active_request_clear": active_clear,
            "active_clear_observation": (
                "same_snapshot" if active_clear is True else "not_observed"
            ),
            "terminal_lifecycle_observed": active_clear is True,
            "runtime_reported_completion": (
                terminal_status == "completed" if active_clear is True else False
            ),
        }
    )
    if not _result_reason_is_valid(result):
        observation["terminal_lifecycle_observed"] = False
        observation["runtime_reported_completion"] = False
        observation["reconciliation_required"] = True
    if active_clear is not True or terminal_status == "unknown":
        observation["reconciliation_required"] = True
    return observation


def _result_reason_is_valid(result: Mapping[str, object]) -> bool:
    data = result.get("data")
    if not isinstance(data, Mapping) or "result_reason" not in data:
        return True
    reason = data.get("result_reason")
    return type(reason) is str and bool(reason.strip())


def _active_request_clear(commands: Mapping[str, object]) -> bool | None:
    if "active_request_id" not in commands:
        return None
    active_request_id = commands["active_request_id"]
    if active_request_id is None:
        return True
    if type(active_request_id) is str and active_request_id.strip():
        return False
    return None
