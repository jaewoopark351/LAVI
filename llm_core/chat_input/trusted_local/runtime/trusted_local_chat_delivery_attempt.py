#20260905_kpopmodder: Carry one immutable local-Chat registration attempt.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class TrustedLocalChatDeliveryAttempt:
    registrar: object
    delivery: object


__all__ = ("TrustedLocalChatDeliveryAttempt",)
