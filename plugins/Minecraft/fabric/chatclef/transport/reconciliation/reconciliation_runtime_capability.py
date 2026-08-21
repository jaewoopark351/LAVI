#20260821_kpopmodder: Keep production stale-active mutation blocked unless an explicit runtime capability is injected.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class ReconciliationRuntimeCapability:
    runtime_ready: bool
    blocked_reason: str = ""
    evidence_contract_verified: bool = False
    guarded_mutation_allowed: bool = False

    @classmethod
    def production_blocked(
        cls,
        reason: str = "runtime_readiness_unavailable",
    ) -> "ReconciliationRuntimeCapability":
        return cls(
            runtime_ready=False,
            blocked_reason=reason,
            evidence_contract_verified=False,
            guarded_mutation_allowed=False,
        )

    @classmethod
    def for_test(
        cls,
        *,
        evidence_contract_verified: bool = True,
        guarded_mutation_allowed: bool = True,
    ) -> "ReconciliationRuntimeCapability":
        runtime_ready = evidence_contract_verified and guarded_mutation_allowed
        return cls(
            runtime_ready=runtime_ready,
            blocked_reason="" if runtime_ready else "test_runtime_not_ready",
            evidence_contract_verified=evidence_contract_verified,
            guarded_mutation_allowed=guarded_mutation_allowed,
        )
