#20260831_kpopmodder: Report Carry On identity contract verification immutably.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AutomaticDepositCarryOnIdentityVerification:
    ok: bool
    reason: str
    errors: tuple[str, ...] = ()
