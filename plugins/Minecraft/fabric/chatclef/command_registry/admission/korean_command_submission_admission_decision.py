#20260827_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class KoreanCommandSubmissionAdmissionDecision:
    allowed: bool
    command_name: str
    source: str
    reason_code: str = ""
    message: str = ""
    expose_admission_reason: bool = False
