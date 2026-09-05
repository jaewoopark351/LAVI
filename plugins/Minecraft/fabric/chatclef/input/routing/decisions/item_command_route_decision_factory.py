#20260905_kpopmodder: Build owned-invalid item-command route decisions.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class ItemCommandRouteDecisionFactory:
    def translation_rejection(
        self,
        translation: Mapping[str, Any],
        reason_code: str,
        message: str,
    ) -> MinecraftChatClefInputRouteDecision:
        reason = str(reason_code or "item_command_translation_rejected")
        text = str(message or "").strip()
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_item_command_translation_rejected",
            response_text="",
            result={
                "ok": False,
                "error": reason,
                "message": text,
                "details": {
                    "reason_code": reason,
                    "item_command_owned_invalid": True,
                },
            },
            translation=dict(translation),
        )
