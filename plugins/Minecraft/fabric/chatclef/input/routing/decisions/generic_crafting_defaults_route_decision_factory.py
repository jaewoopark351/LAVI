#20260905_kpopmodder: Build request-scoped generic-crafting rejection decisions.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class GenericCraftingDefaultsRouteDecisionFactory:
    def rejection(
        self,
        reason_code: str,
        message: str,
        translation: Mapping[str, Any] | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        reason = str(reason_code or "generic_crafting_defaults_rejected")
        text = str(message or reason).strip()
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_generic_crafting_defaults_rejected",
            response_text="",
            result={
                "ok": False,
                "error": reason,
                "message": text,
                "details": {
                    "reason_code": reason,
                    "generic_crafting_defaults": True,
                },
            },
            translation=dict(translation or {}),
        )
