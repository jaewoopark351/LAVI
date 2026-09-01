#20260831_kpopmodder: Keep evidence verification failure explicit and non-product.
from __future__ import annotations

from dataclasses import dataclass

from .verified_evidence import AutomaticDepositVerifiedEvidence


@dataclass(frozen=True, slots=True)
class AutomaticDepositVerifiedEvidenceResult:
    ok: bool
    reason: str
    errors: tuple[str, ...] = ()
    evidence: AutomaticDepositVerifiedEvidence | None = None
