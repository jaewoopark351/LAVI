#20260901_kpopmodder: Carry one path-free P1 guard gate decision.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class P1GuardGateResult:
    clear: bool
    reason: str
    guard_state: str = ""
    guard_reason: str = ""
    error_type: str = ""
