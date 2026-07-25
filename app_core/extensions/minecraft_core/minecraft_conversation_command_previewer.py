#20260725_kpopmodder: Added preview adapter so chat replies do not need to execute Minecraft actions synchronously.
from __future__ import annotations

from typing import Any, Mapping

from core.logger import log_print

from .minecraft_conversation_command_route import MinecraftConversationCommandRoute


class MinecraftConversationCommandPreviewer:
    def preview(
        self,
        extension: Any,
        route: MinecraftConversationCommandRoute,
    ) -> Mapping[str, Any] | None:
        preview_command = getattr(extension, "preview_command", None)
        if not callable(preview_command):
            return None
        try:
            result = preview_command(route.command)
        except Exception as error:
            log_print(f"[MinecraftConversation] preview failed: {error}")
            return None
        return result if isinstance(result, Mapping) else None
