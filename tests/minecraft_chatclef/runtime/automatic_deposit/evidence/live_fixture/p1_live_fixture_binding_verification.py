# 20260901_kpopmodder: Preserve bound fixture identity separately from live readiness.
from __future__ import annotations

from dataclasses import dataclass

from .p1_live_fixture_observation import P1LiveFixtureObservation


@dataclass(frozen=True, slots=True)
class P1LiveFixtureBindingVerification:
    ok: bool
    identity_bound: bool
    ready: bool
    reason: str
    observation: P1LiveFixtureObservation | None = None
    observation_fingerprint: str = ""
    identity_errors: tuple[str, ...] = ()
    readiness_errors: tuple[str, ...] = ()
    limitations: tuple[str, ...] = ()

    @property
    def errors(self) -> tuple[str, ...]:
        return self.identity_errors + self.readiness_errors
