#20260818_kpopmodder: Observe one submitted request without retry, replay, stop, or cancel.
from __future__ import annotations

import time
from typing import Callable

from ..preflight.runtime_command_snapshot import (
    command_snapshot,
)
from .runtime_connection_state import runtime_connection_error
from .terminal_result_application import apply_terminal_result_snapshot
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
    request_id = observation.get("submitted_request_id")
    if observation.get("submission_outcome") != "accepted":
        return observation
    if (
        type(request_id) is not str
        or not request_id
        or request_id != request_id.strip()
    ):
        observation["reconciliation_required"] = True
        return observation
    deadline = monotonic() + timeout_sec
    while monotonic() < deadline:
        try:
            status = gateway.read_status()
        except Exception:
            observation["connection_state_verified"] = False
            observation["connection_state_error"] = "runtime status read failed"
            observation["reconciliation_required"] = True
            return observation
        connection_error = runtime_connection_error(status)
        if connection_error:
            observation["connection_state_verified"] = False
            observation["connection_state_error"] = connection_error
            observation["reconciliation_required"] = True
            return observation
        observation["connection_state_verified"] = True
        observation["connection_state_error"] = ""
        commands = command_snapshot(status)
        last_result = commands.get("last_result")
        if is_matching_terminal_result(last_result, request_id):
            return apply_terminal_result_snapshot(
                observation,
                commands,
                last_result,
                request_id,
            )
        sleeper(poll_sec)
    observation["observer_timeout"] = True
    observation["reconciliation_required"] = True
    return observation
