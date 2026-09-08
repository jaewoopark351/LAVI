#20260908_kpopmodder: Carry only bounded status-route failure facts.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandStatusRouteFailureRecord:
    stage: str
    query_kind: str
    addressed: bool | str
    requested_family: str
    active_command_name: str
    lifecycle_state: str
    terminal_state: str
    availability_reason: str
    exception_class: str


__all__ = ("CommandStatusRouteFailureRecord",)
