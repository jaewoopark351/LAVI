#20260831_kpopmodder: Report exclusive evidence-directory creation without fallback.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AutomaticDepositEvidenceDirectoryResult:
    ok: bool
    reason: str
    path: str = ""
