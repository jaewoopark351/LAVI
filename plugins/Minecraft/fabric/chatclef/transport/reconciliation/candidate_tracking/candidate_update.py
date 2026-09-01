#20260901_kpopmodder: Isolate the reconciliation candidate transition result.
from __future__ import annotations

from dataclasses import dataclass

from .reconciliation_candidate import ReconciliationCandidate


@dataclass(frozen=True)
class CandidateUpdate:
    candidate: ReconciliationCandidate | None
    would_reconcile: bool
    reason: str
