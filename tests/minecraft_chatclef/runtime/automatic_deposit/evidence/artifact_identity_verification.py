#20260831_kpopmodder: Report immutable ChatClef artifact identity verification.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AutomaticDepositArtifactIdentityVerification:
    ok: bool
    reason: str
    errors: tuple[str, ...] = ()
