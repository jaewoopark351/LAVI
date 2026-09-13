#20260913_kpopmodder: Freeze diagnostic strings without adding command-result evidence.
from __future__ import annotations

from dataclasses import dataclass, fields
import re


@dataclass(frozen=True, slots=True)
class GotoTerminalDiagnosticRecord:
    boundary: str
    owner: str
    role: str
    reason: str
    event_id: str
    request_id: str
    correlation_id: str
    session_id: str
    generation: str
    status: str
    verified: str
    profile_id: str
    rollout_state: str
    evaluator_id: str
    family: str
    result_reason: str
    result_fidelity: str
    projection_present: str
    output_chars: str
    output_sha256: str
    #20260913_kpopmodder: Record failure evidence separately from arrival verification.
    failure_projection_present: str = "false"

    def __post_init__(self) -> None:
        if any(
            type(getattr(self, field.name)) is not str
            or re.fullmatch(r"[A-Za-z0-9_.-]{1,96}", getattr(self, field.name), re.ASCII) is None
            for field in fields(self)
        ):
            raise ValueError("GOTO diagnostic fields must be bounded exact strings")
