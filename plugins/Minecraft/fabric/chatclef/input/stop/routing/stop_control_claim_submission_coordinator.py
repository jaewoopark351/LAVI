#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .submission import (
    StopControlClaimLifecycleCoordinator,
    StopControlExtensionSubmitter,
)


class StopControlClaimSubmissionCoordinator:
    def __init__(self, *, extension: object, claim_registry: object):
        self._claim_lifecycle = StopControlClaimLifecycleCoordinator(
            claim_registry
        )
        self._transport_submitter = StopControlExtensionSubmitter(extension)

    def submit(
        self,
        *,
        event: object,
        eligibility_proof: object,
        normalized_phrase: str,
    ) -> tuple[object | None, str]:
        receipt, reason = self._claim_lifecycle.issue(
            event=event,
            eligibility_proof=eligibility_proof,
            normalized_phrase=normalized_phrase,
        )
        if receipt is None:
            return None, reason

        result, reason = self._transport_submitter.submit(
            event=event,
            eligibility_proof=eligibility_proof,
            receipt=receipt,
        )
        if reason:
            self._claim_lifecycle.abandon(
                receipt=receipt,
                event=event,
                eligibility_proof=eligibility_proof,
            )
        return result, reason


__all__ = ("StopControlClaimSubmissionCoordinator",)
