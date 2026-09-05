#20260905_kpopmodder: Observe trusted Korean admission without affecting routing.
from __future__ import annotations


class TrustedKoreanFeatureAdmissionObserver:
    def __init__(self, *, projector, logger):
        self._projector = projector
        self._logger = logger

    def observe(
        self,
        event: object,
        *,
        eligibility_reason: object,
        proof_issued: bool,
        route_decision: object,
    ) -> None:
        try:
            record = self._projector.project(
                event=event,
                eligibility_reason=eligibility_reason,
                proof_issued=proof_issued,
                route_decision=route_decision,
            )
            self._logger.log(record)
        except Exception:
            return


__all__ = ("TrustedKoreanFeatureAdmissionObserver",)
