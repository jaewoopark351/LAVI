#20260901_kpopmodder: Preserve one immutable fail-closed P1 StoreHome runtime evidence projection.
from __future__ import annotations

from dataclasses import dataclass


P1_STORE_HOME_EVIDENCE_KEYS = (
    "runtime_reported_completion",
    "trusted_binding_verified",
    "transfer_observed",
    "typed_terminal_observed",
)

P1_STORE_HOME_FALSE_EVIDENCE = tuple(
    (key, False) for key in P1_STORE_HOME_EVIDENCE_KEYS
)
P1_STORE_HOME_TRUE_EVIDENCE = tuple(
    (key, True) for key in P1_STORE_HOME_EVIDENCE_KEYS
)


@dataclass(frozen=True, slots=True)
class P1StoreHomeRuntimeEvidenceProjection:
    ok: bool
    reason: str
    operation_id: str
    command_request_id: str
    evidence: tuple[tuple[str, bool], ...]

    def evidence_mapping(self) -> dict[str, bool]:
        return dict(self.evidence)
