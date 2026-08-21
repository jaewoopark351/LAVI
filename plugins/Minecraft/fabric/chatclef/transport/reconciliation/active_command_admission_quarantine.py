#20260821_kpopmodder: Keep post-reconciliation command admission blocked until Java retirement is proven.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from .reconciliation_identity import ReconciliationIdentity


@dataclass(frozen=True)
class ActiveCommandAdmissionQuarantine:
    active: bool = False
    reason: str = ""
    released_identity: ReconciliationIdentity | None = None
    java_context_retirement_verified: bool = False
    retirement_evidence_kind: str = "none"
    activated_at_ms: int = 0

    @classmethod
    def inactive(cls) -> "ActiveCommandAdmissionQuarantine":
        return cls()

    @classmethod
    def for_reconciled_unknown(
        cls,
        *,
        identity: ReconciliationIdentity,
        activated_at_ms: int,
    ) -> "ActiveCommandAdmissionQuarantine":
        return cls(
            active=True,
            reason="java_command_context_retirement_unverified",
            released_identity=identity,
            java_context_retirement_verified=False,
            retirement_evidence_kind="none",
            activated_at_ms=activated_at_ms,
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "active": self.active,
            "reason": self.reason,
            "released_identity": (
                None
                if self.released_identity is None
                else self.released_identity.to_dict()
            ),
            "java_context_retirement_verified": (
                self.java_context_retirement_verified
            ),
            "retirement_evidence_kind": self.retirement_evidence_kind,
            "persistence_mode": "process_lifetime",
            "restart_strategy": "fail_closed_without_runtime_capability",
            "clearance_policy": "no_auto_clear",
            "activated_at_ms": self.activated_at_ms,
        }
