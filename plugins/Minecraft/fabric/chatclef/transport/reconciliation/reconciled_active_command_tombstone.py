#20260821_kpopmodder: Keep late old command results audit-only after local UNKNOWN reconciliation.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from .reconciliation_identity import ReconciliationIdentity


@dataclass(frozen=True)
class ReconciledActiveCommandTombstone:
    identity: ReconciliationIdentity
    reconciled_at_ms: int
    expires_at_ms: int
    evidence_sequence: int
    synthetic_unknown_reason: str
    profile: str
    late_event_count: int = 0
    last_late_event_kind: str = ""

    def is_expired(self, now_ms: int) -> bool:
        return now_ms >= self.expires_at_ms

    def with_late_event(self, event_kind: str) -> "ReconciledActiveCommandTombstone":
        return ReconciledActiveCommandTombstone(
            identity=self.identity,
            reconciled_at_ms=self.reconciled_at_ms,
            expires_at_ms=self.expires_at_ms,
            evidence_sequence=self.evidence_sequence,
            synthetic_unknown_reason=self.synthetic_unknown_reason,
            profile=self.profile,
            late_event_count=self.late_event_count + 1,
            last_late_event_kind=event_kind,
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "identity": self.identity.to_dict(),
            "reconciled_at_ms": self.reconciled_at_ms,
            "expires_at_ms": self.expires_at_ms,
            "evidence_sequence": self.evidence_sequence,
            "synthetic_unknown_reason": self.synthetic_unknown_reason,
            "profile": self.profile,
            "late_event_count": self.late_event_count,
            "last_late_event_kind": self.last_late_event_kind,
        }

