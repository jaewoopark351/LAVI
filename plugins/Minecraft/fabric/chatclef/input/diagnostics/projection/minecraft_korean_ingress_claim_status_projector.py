#20260905_kpopmodder: Project only trusted-ingress claim status.
from __future__ import annotations


class MinecraftKoreanIngressClaimStatusProjector:
    _EVIDENCE_REJECTION_REASONS = frozenset(
        {
            "consumed_ingress_evidence_invalid",
            "consumed_ingress_authority_unavailable",
            "consumed_ingress_authority_failed",
            "consumed_ingress_evidence_foreign",
            "consumed_ingress_evidence_spent",
            "eligibility_owner_missing",
        }
    )

    def project(self, eligibility_reason: object, *, proof_issued: bool) -> str:
        if proof_issued:
            return "consumed"
        if eligibility_reason in self._EVIDENCE_REJECTION_REASONS:
            return "rejected"
        return "consumed"


__all__ = ("MinecraftKoreanIngressClaimStatusProjector",)
