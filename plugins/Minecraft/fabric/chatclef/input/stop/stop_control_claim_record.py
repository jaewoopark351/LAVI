#20260905_kpopmodder: Store one STOP claim record independently from registry behavior.
from __future__ import annotations

from dataclasses import dataclass


@dataclass
class StopControlClaimRecord:
    event: object
    proof: object
    phrase: str
    nonce: object
    receipt: object
    spent: bool = False


__all__ = ("StopControlClaimRecord",)
