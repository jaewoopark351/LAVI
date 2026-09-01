#20260831_kpopmodder: Return immutable run-manifest verification evidence.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AutomaticDepositRunManifestVerification:
    ok: bool
    reason: str
    errors: tuple[str, ...] = ()
