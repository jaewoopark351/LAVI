#20260831_kpopmodder: Preserve one immutable matrix orchestration result.
from __future__ import annotations

from dataclasses import dataclass

from ..oracle.matrix_verdict import AutomaticDepositVerdict
from ..scenario.transport_mode import AutomaticDepositTransportMode


@dataclass(frozen=True, slots=True)
class AutomaticDepositMatrixRunResult:
    row_id: str
    transport_mode: AutomaticDepositTransportMode | None
    verdict: AutomaticDepositVerdict
    reason: str
    submit_call_count: int
    operator_action_fingerprint: str = ""
    submission_invocation_fingerprint: str = ""
    missing_evidence: tuple[str, ...] = ()
    violated_contracts: tuple[str, ...] = ()

    def as_mapping(self) -> dict[str, object]:
        return {
            "row_id": self.row_id,
            "transport_mode": (
                self.transport_mode.value if self.transport_mode is not None else ""
            ),
            "verdict": self.verdict.value,
            "reason": self.reason,
            "submit_call_count": self.submit_call_count,
            "operator_action_fingerprint": self.operator_action_fingerprint,
            "submission_invocation_fingerprint": (
                self.submission_invocation_fingerprint
            ),
            "missing_evidence": list(self.missing_evidence),
            "violated_contracts": list(self.violated_contracts),
        }
