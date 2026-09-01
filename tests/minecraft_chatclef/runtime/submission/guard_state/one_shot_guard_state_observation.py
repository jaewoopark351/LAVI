#20260901_kpopmodder: Carry one immutable read-only guard observation result.
from __future__ import annotations

from dataclasses import dataclass

from .one_shot_guard_state import OneShotGuardState


@dataclass(frozen=True, slots=True)
class OneShotGuardStateObservation:
    state: OneShotGuardState
    state_directory: str
    invocation_fingerprint: str | None
    command_fingerprint: str | None
    reason: str
