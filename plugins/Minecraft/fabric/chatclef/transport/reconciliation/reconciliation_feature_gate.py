#20260821_kpopmodder: Separate requested feature config from runtime readiness for stale-active mutation.
from __future__ import annotations

from dataclasses import dataclass, field

from .reconciliation_runtime_capability import ReconciliationRuntimeCapability


@dataclass(frozen=True)
class ReconciliationFeatureGate:
    requested_enabled: bool = False
    runtime_capability: ReconciliationRuntimeCapability = field(
        default_factory=ReconciliationRuntimeCapability.production_blocked
    )

    @property
    def runtime_ready(self) -> bool:
        return self.runtime_capability.runtime_ready

    @property
    def effective_enabled(self) -> bool:
        return self.requested_enabled and self.runtime_ready

    @property
    def blocked_reason(self) -> str:
        if not self.requested_enabled:
            return "feature_flag_disabled"
        if not self.runtime_ready:
            return (
                self.runtime_capability.blocked_reason
                or "runtime_readiness_unavailable"
            )
        return "none"

    def to_dict(self) -> dict[str, object]:
        return {
            "requested_enabled": self.requested_enabled,
            "runtime_ready": self.runtime_ready,
            "effective_enabled": self.effective_enabled,
            "blocked_reason": self.blocked_reason,
            "runtime_capability_source": (
                "test_injected" if self.runtime_ready else "production_blocked"
            ),
            "restart_strategy": "fail_closed_without_runtime_capability",
            "evidence_contract_verified": (
                self.runtime_capability.evidence_contract_verified
            ),
            "guarded_mutation_allowed": (
                self.runtime_capability.guarded_mutation_allowed
            ),
        }
