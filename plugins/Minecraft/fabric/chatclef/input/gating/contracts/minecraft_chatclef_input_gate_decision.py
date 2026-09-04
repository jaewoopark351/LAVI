#20260905_kpopmodder: Keep one immutable gate decision separate from route execution.
from __future__ import annotations

from dataclasses import dataclass

from .minecraft_chatclef_input_route_kind import (
    MinecraftChatClefInputRouteKind,
)


@dataclass(frozen=True)
class MinecraftChatClefInputGateDecision:
    consider: bool
    route_kind: MinecraftChatClefInputRouteKind

    @classmethod
    def none(cls) -> "MinecraftChatClefInputGateDecision":
        return cls(False, MinecraftChatClefInputRouteKind.NONE)

    @classmethod
    def generic(cls) -> "MinecraftChatClefInputGateDecision":
        return cls(True, MinecraftChatClefInputRouteKind.GENERIC)

    @classmethod
    def h5_auto_deposit_trust(cls) -> "MinecraftChatClefInputGateDecision":
        return cls(True, MinecraftChatClefInputRouteKind.H5_AUTO_DEPOSIT_TRUST)
