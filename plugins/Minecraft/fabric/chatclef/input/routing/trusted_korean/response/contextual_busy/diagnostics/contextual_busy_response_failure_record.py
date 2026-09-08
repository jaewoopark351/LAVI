#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Carry only bounded contextual-busy pre-permit failure facts.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class ContextualBusyResponseFailureRecord:
    stage: str
    busy_reason: str
    active_command_name: str
    active_lifecycle_state: str
    terminal_state: str
    availability_reason: str
    exception_class: str
    selected_fallback: str


__all__ = ("ContextualBusyResponseFailureRecord",)
