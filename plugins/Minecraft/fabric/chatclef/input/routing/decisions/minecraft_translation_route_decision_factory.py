#20260905_kpopmodder: Build decisions for ordinary translation rejection and failures.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.response import ChatClefCommandResponseRenderer


class MinecraftTranslationRouteDecisionFactory:
    def __init__(self, response_renderer: ChatClefCommandResponseRenderer):
        self._response_renderer = response_renderer

    def rejection(
        self,
        translation: Mapping[str, Any],
    ) -> MinecraftChatClefInputRouteDecision:
        reason_code = str(
            translation.get("reason_code")
            or translation.get("status")
            or "translation_rejected"
        )
        message = str(translation.get("message") or reason_code).strip()
        data = translation.get("data")
        details = dict(data) if isinstance(data, Mapping) else {}
        result = {
            "ok": False,
            "status": dict(translation),
            "error": reason_code,
            "message": message,
            "details": details,
        }
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_translation_rejected",
            response_text=self._response_renderer.render_translation_rejection(
                translation
            ),
            result=result,
            translation=dict(translation),
        )

    def malformed(self, error: Exception) -> MinecraftChatClefInputRouteDecision:
        message = f"{type(error).__name__}: {error}"
        result = {
            "ok": False,
            "error": "malformed_translation_result",
            "message": message,
            "details": {},
        }
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_translation_malformed",
            response_text=self._response_renderer.render_operation_failure(
                "malformed_translation_result",
                message,
            ),
            result=result,
            translation={},
        )

    def operation_failure(
        self,
        reason: str,
        error: Exception,
        translation: Mapping[str, Any] | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        message = f"{type(error).__name__}: {error}"
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason,
            response_text=self._response_renderer.render_operation_failure(
                reason,
                message,
            ),
            result={"ok": False, "error": reason, "message": message},
            translation=dict(translation or {}),
        )
