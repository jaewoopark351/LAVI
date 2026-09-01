#20260831_kpopmodder: Return one immutable automatic-deposit verdict result.
from __future__ import annotations

from dataclasses import dataclass

from .matrix_verdict import AutomaticDepositVerdict


@dataclass(frozen=True, slots=True)
class AutomaticDepositVerdictResult:
    verdict: AutomaticDepositVerdict
    reason: str
    missing_evidence: tuple[str, ...] = ()
    violated_contracts: tuple[str, ...] = ()
