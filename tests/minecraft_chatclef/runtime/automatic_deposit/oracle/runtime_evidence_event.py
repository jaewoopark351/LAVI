#20260831_kpopmodder: Preserve one strictly correlated runtime evidence event.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AutomaticDepositRuntimeEvidenceEvent:
    sequence: int
    run_id: str
    row_id: str
    operation_id: str
    artifact_sha256: str
    fixture_fingerprint: str
    owner: str
    evidence_key: str
    evidence_value: str
