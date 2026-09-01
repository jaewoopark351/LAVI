#20260901_kpopmodder: Carry one exact read-only P1 one-shot guard observation result.
from __future__ import annotations

from dataclasses import dataclass

from minecraft_chatclef.runtime.submission.guard_state import (
    OneShotGuardStateObservation,
)


@dataclass(frozen=True, slots=True)
class P1GuardObservationResult:
    observation: OneShotGuardStateObservation | None
    error_type: str = ""
