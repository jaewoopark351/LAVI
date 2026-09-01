#20260831_kpopmodder: Report fixture identity and completeness immutably.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AutomaticDepositFixtureManifestVerification:
    ok: bool
    reason: str
    errors: tuple[str, ...] = ()
