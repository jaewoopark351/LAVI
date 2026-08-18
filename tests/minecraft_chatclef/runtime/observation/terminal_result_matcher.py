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
    if str(last_result.get("request_id") or "") != submitted_request_id:
        return False
    return str(last_result.get("status") or "").strip().lower() in TERMINAL_STATUSES
