#20260803_kpopmodder: Keep Minecraft input route decisions explicit and testable.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class MinecraftChatClefInputRouteDecision:
    handled: bool
    reason: str
    response_text: str = ""
    result: dict[str, Any] = field(default_factory=dict)
    translation: dict[str, Any] = field(default_factory=dict)

    @classmethod
    def not_handled(cls, reason: str) -> "MinecraftChatClefInputRouteDecision":
        return cls(handled=False, reason=str(reason or "not_handled"))

    @classmethod
    def handled_result(
        cls,
        *,
        reason: str,
        response_text: str,
        result: dict[str, Any] | None = None,
        translation: dict[str, Any] | None = None,
    ) -> "MinecraftChatClefInputRouteDecision":
        return cls(
            handled=True,
            reason=str(reason or "handled"),
            response_text=str(response_text or ""),
            result=dict(result or {}),
            translation=dict(translation or {}),
        )
