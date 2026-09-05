#20260905_kpopmodder: Own trusted Korean input normalization and eligibility admission.
from __future__ import annotations

from .trusted_korean_input_admission_outcome import (
    TrustedKoreanInputAdmissionOutcome,
)


class TrustedKoreanInputAdmission:
    _SCOPE_OUT_REASONS = frozenset(
        (
            "input_source_not_eligible",
            "input_language_not_korean",
        )
    )

    def __init__(self, *, owner: object, input_event_normalizer, admission):
        self._owner = owner
        self._input_event_normalizer = input_event_normalizer
        self._admission = admission

    def admit(
        self,
        value: object,
        consumed_ingress_evidence: object,
    ) -> TrustedKoreanInputAdmissionOutcome:
        event = self._input_event_normalizer.normalize(value)
        proof, reason = self._admission.issue(
            event=event,
            consumed_ingress_evidence=consumed_ingress_evidence,
            owner=self._owner,
        )
        return TrustedKoreanInputAdmissionOutcome(
            event=event,
            proof=proof,
            reason=reason,
            should_fall_through=(
                proof is None and reason in self._SCOPE_OUT_REASONS
            ),
        )


__all__ = ("TrustedKoreanInputAdmission",)
