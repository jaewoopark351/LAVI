#20260905_kpopmodder: Carry one immutable H5 translation-boundary admission result.
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AutoDepositTrustTranslationAdmissionDecision:
    allowed: bool
    reason_code: str = ""
    message: str = ""


__all__ = ["AutoDepositTrustTranslationAdmissionDecision"]
