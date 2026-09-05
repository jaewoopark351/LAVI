#20260905_kpopmodder: Isolate route-owned STOP claim issue and abandonment.
from __future__ import annotations


class StopControlClaimLifecycleCoordinator:
    def __init__(self, claim_registry: object):
        self._claim_registry = claim_registry

    def issue(
        self,
        *,
        event: object,
        eligibility_proof: object,
        normalized_phrase: str,
    ) -> tuple[object | None, str]:
        return self._claim_registry.issue(
            event=event,
            eligibility_proof=eligibility_proof,
            normalized_phrase=normalized_phrase,
        )

    def abandon(
        self,
        *,
        receipt: object,
        event: object,
        eligibility_proof: object,
    ) -> None:
        self._claim_registry.spend(
            receipt,
            event=event,
            eligibility_proof=eligibility_proof,
        )


__all__ = ("StopControlClaimLifecycleCoordinator",)
