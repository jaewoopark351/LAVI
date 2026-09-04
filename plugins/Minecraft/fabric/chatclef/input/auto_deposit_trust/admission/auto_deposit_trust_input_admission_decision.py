#20260905_kpopmodder: Represent one immutable H5 input-provenance admission result.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class AutoDepositTrustInputAdmissionDecision:
    allowed: bool
    reason_code: str = ""
    message: str = ""
