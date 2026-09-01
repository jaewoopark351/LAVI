#20260831_kpopmodder: Describe one immutable runtime evidence requirement.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class AutomaticDepositEvidenceRequirement:
    key: str
    owner: str
    expected_value: str = "present"
