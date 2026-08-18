#20260818_kpopmodder: Match only the submitted request and documented terminal statuses.
from __future__ import annotations

from typing import Mapping


TERMINAL_STATUSES = {
    "completed",
    "rejected",
    "failed",
    "cancelled",
    "deadline_exceeded",
    "unknown",
}


def is_matching_terminal_result(
    last_result: object,
    submitted_request_id: str,
) -> bool:
    if not isinstance(last_result, Mapping):
        return False
    if (
        type(submitted_request_id) is not str
        or not submitted_request_id
        or submitted_request_id != submitted_request_id.strip()
    ):
        return False
    if last_result.get("request_id") != submitted_request_id:
        return False
    status = last_result.get("status")
    return type(status) is str and status in TERMINAL_STATUSES
