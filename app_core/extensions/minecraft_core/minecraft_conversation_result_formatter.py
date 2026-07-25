#20260725_kpopmodder: Added formatter for Minecraft conversation command responses.
from __future__ import annotations

from typing import Any, Mapping

from .minecraft_conversation_command_route import MinecraftConversationCommandRoute


class MinecraftConversationResultFormatter:
    def extension_missing(self, route: MinecraftConversationCommandRoute) -> str:
        return "Minecraft extension is not available."

    def command_failed(
        self,
        route: MinecraftConversationCommandRoute,
        error: Exception,
    ) -> str:
        return f"Minecraft command failed: {type(error).__name__}: {error}"

    def format(
        self,
        route: MinecraftConversationCommandRoute,
        result: Any,
    ) -> str:
        if not isinstance(result, Mapping):
            return f"Minecraft command handled: {route.command}"

        ok = bool(result.get("ok"))
        action = str(result.get("action") or route.command).strip()
        message = str(result.get("message") or "").strip()
        error = str(result.get("error") or "").strip()

        if not ok:
            detail = error or message or "unknown error"
            return f"Minecraft command failed: {detail}"

        if result.get("accepted") is True:
            return f"Minecraft command accepted: {action}"

        if message:
            return f"Minecraft command handled: {message}"

        return f"Minecraft command handled: {action}"
