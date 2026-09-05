#20260905_kpopmodder: Carry one immutable trusted Korean admission result.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class TrustedKoreanInputAdmissionOutcome:
    event: object
    proof: object
    reason: object
    should_fall_through: bool


__all__ = ("TrustedKoreanInputAdmissionOutcome",)
