#20260819_kpopmodder: Validate strict UTC process start evidence for listener identity.
from __future__ import annotations

from typing import Mapping


PROCESS_START_TIME_FIELD = "creation_time_utc_ticks"


def process_start_time_utc_ticks(process: Mapping[str, object]) -> int | None:
    value = process.get(PROCESS_START_TIME_FIELD)
    return value if type(value) is int and value > 0 else None


def process_started_no_later_than(
    ancestor: Mapping[str, object],
    child: Mapping[str, object],
) -> bool:
    ancestor_ticks = process_start_time_utc_ticks(ancestor)
    child_ticks = process_start_time_utc_ticks(child)
    return (
        ancestor_ticks is not None
        and child_ticks is not None
        and ancestor_ticks <= child_ticks
    )
