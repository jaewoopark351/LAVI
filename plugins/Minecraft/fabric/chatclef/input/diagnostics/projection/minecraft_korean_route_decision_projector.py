#20260905_kpopmodder: Project only the bounded route decision outcome.
from __future__ import annotations


class MinecraftKoreanRouteDecisionProjector:
    def project(
        self,
        eligibility_reason: object,
        *,
        proof_issued: bool,
        handled: bool,
    ) -> str:
        if proof_issued:
            return "handled" if handled else "fallthrough"
        if eligibility_reason in {
            "input_source_not_eligible",
            "input_language_not_korean",
        }:
            return "fallthrough"
        return "rejected"


__all__ = ("MinecraftKoreanRouteDecisionProjector",)
