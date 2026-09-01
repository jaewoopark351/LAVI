#20260831_kpopmodder: Separate harness uncertainty from product and handoff defects.
from __future__ import annotations

from enum import Enum


class AutomaticDepositVerdict(str, Enum):
    PASS = "PASS"
    FAIL = "FAIL"
    INCONCLUSIVE = "INCONCLUSIVE"
    HANDOFF_DEFECT_FOUND = "HANDOFF_DEFECT_FOUND"
