#20260901_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from dataclasses import dataclass, replace


@dataclass(frozen=True, slots=True)
class P1SupervisedVerifiedEvidenceResult:
    ok: bool
    verdict: str
    reason: str
    submit_call_count: int
    automatic_resubmit_count: int
    submitted_request_id: str
    operation_id: str
    evidence: tuple[tuple[str, object], ...] = ()
    reconciled: bool = False


def inconclusive_p1_supervised_evidence(
    reason: str,
    *,
    submit_call_count: int = 0,
    automatic_resubmit_count: int = 0,
    submitted_request_id: str = "",
    operation_id: str = "",
) -> P1SupervisedVerifiedEvidenceResult:
    return P1SupervisedVerifiedEvidenceResult(
        ok=False,
        verdict="INCONCLUSIVE",
        reason=reason,
        submit_call_count=submit_call_count,
        automatic_resubmit_count=automatic_resubmit_count,
        submitted_request_id=submitted_request_id,
        operation_id=operation_id,
    )


def with_p1_supervised_reconciliation(
    result: P1SupervisedVerifiedEvidenceResult,
) -> P1SupervisedVerifiedEvidenceResult:
    return replace(
        result,
        verdict="PASS",
        reason="P1_SUPERVISED_VERIFIED_EVIDENCE_RECONCILED",
        reconciled=True,
    )
