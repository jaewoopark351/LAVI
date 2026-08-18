#20260818_kpopmodder: Observe one submitted request without retry, replay, stop, or cancel.
from __future__ import annotations

import time
from typing import Callable, Mapping

from ..preflight.runtime_command_snapshot import (
    command_snapshot,
)
from .terminal_result_matcher import is_matching_terminal_result


def observe_terminal_result(
    gateway: object,
    observation: dict[str, object],
    *,
    timeout_sec: float,
    poll_sec: float,
    monotonic: Callable[[], float] = time.monotonic,
    sleeper: Callable[[float], None] = time.sleep,
) -> dict[str, object]:
    request_id = str(observation.get("submitted_request_id") or "").strip()
    if observation.get("submission_outcome") != "accepted" or not request_id:
        return observation
    deadline = monotonic() + timeout_sec
    while monotonic() < deadline:
        try:
            status = gateway.read_status()
        except Exception:
            observation["reconciliation_required"] = True
            return observation
        commands = command_snapshot(status)
        last_result = commands.get("last_result")
        if is_matching_terminal_result(last_result, request_id):
            result = dict(last_result)
            terminal_status = str(result.get("status") or "").strip().lower()
            active_clear = _active_request_clear(commands)
            observation.update(
                {
                    "terminal_request_id": request_id,
                    "terminal_status": terminal_status,
                    "active_request_clear": active_clear,
                    "active_clear_observation": (
                        "same_snapshot"
                        if active_clear is True
                        else "not_observed"
                    ),
                    "terminal_lifecycle_observed": active_clear is True,
                    "runtime_reported_completion": (
                        terminal_status == "completed"
                        if active_clear is True
                        else False
                    ),
                }
            )
            data = result.get("data")
            if isinstance(data, Mapping) and "result_reason" in data:
                if not str(data.get("result_reason") or "").strip():
                    observation["terminal_lifecycle_observed"] = False
                    observation["runtime_reported_completion"] = False
                    observation["reconciliation_required"] = True
            if active_clear is not True or terminal_status == "unknown":
                observation["reconciliation_required"] = True
            return observation
        sleeper(poll_sec)
    observation["observer_timeout"] = True
    observation["reconciliation_required"] = True
    return observation


def _active_request_clear(commands: Mapping[str, object]) -> bool | None:
    if "active_request_id" not in commands:
        return None
    active_request_id = commands["active_request_id"]
    if active_request_id is None:
        return True
    if isinstance(active_request_id, str) and active_request_id.strip():
        return False
    return None
