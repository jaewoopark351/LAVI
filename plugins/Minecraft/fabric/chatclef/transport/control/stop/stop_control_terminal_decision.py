#20260905_kpopmodder: Represent strict Python validation of one STOP terminal result.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class StopControlTerminalDecision:
    valid: bool
    release_barrier: bool
    status: str
    control_outcome: str
    reason: str


__all__ = ("StopControlTerminalDecision",)
