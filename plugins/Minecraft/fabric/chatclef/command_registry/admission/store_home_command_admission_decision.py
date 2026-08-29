#20260827_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class StoreHomeCommandAdmissionDecision:
    allowed: bool
    reason_code: str = ""
    message: str = ""
