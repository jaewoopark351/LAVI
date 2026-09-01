#20260901_kpopmodder: Define the complete read-only one-shot guard state set.
from __future__ import annotations

from enum import Enum


class OneShotGuardState(str, Enum):
    CLEAR = "CLEAR"
    LIVE_RUN_BLOCK_PRESENT = "LIVE_RUN_BLOCK_PRESENT"
    RECONCILIATION_REQUIRED = "RECONCILIATION_REQUIRED"
    GUARD_STATE_UNREADABLE = "GUARD_STATE_UNREADABLE"
