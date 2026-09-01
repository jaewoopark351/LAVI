#20260901_kpopmodder: Isolate the immutable deposit reconciliation profile contract.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class DepositReconciliationProfile:
    allowed: bool
    normalized_command: str = ""
    item_id: str = ""
    count: int = 0
    reason: str = ""
