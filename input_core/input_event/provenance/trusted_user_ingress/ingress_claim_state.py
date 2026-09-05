#20260905_kpopmodder: Defines the closed trusted-ingress ownership state machine.
from enum import Enum


class IngressClaimState(str, Enum):
    REGISTERED = "REGISTERED"
    QUEUE_OWNED = "QUEUE_OWNED"
    DISPATCH_OWNED = "DISPATCH_OWNED"
    CONSUMED = "CONSUMED"
    ABANDONED = "ABANDONED"


__all__ = ("IngressClaimState",)
