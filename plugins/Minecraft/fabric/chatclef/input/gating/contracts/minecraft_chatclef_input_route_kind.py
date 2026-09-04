#20260905_kpopmodder: Define typed input-route identities without routing behavior.
from __future__ import annotations

from enum import Enum


class MinecraftChatClefInputRouteKind(str, Enum):
    NONE = "none"
    GENERIC = "generic"
    H5_AUTO_DEPOSIT_TRUST = "h5_auto_deposit_trust"
