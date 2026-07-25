#20260725_kpopmodder: Added formatter for Minecraft conversation command responses.
from __future__ import annotations

from typing import Any, Mapping

from .minecraft_conversation_command_route import MinecraftConversationCommandRoute
from .minecraft_conversation_reply_builder import MinecraftConversationReplyBuilder


class MinecraftConversationResultFormatter:
    def __init__(
        self,
        reply_builder: MinecraftConversationReplyBuilder | None = None,
    ):
        self.reply_builder = reply_builder or MinecraftConversationReplyBuilder()

    def extension_missing(self, route: MinecraftConversationCommandRoute) -> str:
        return self.reply_builder.extension_missing()

    def command_failed(
        self,
        route: MinecraftConversationCommandRoute,
        error: Exception,
    ) -> str:
        return self.reply_builder.failed(f"{type(error).__name__}: {error}")

    def format(
        self,
        route: MinecraftConversationCommandRoute,
        result: Any,
    ) -> str:
        if not isinstance(result, Mapping):
            return self.reply_builder.handled()

        ok = bool(result.get("ok"))
        message = str(result.get("message") or "").strip()
        error = str(result.get("error") or "").strip()

        if not ok:
            detail = error or message or "unknown error"
            return self.reply_builder.failed(detail)

        if result.get("accepted") is True:
            return self.reply_builder.accepted(route, result)

        if message:
            return self.reply_builder.handled(message)

        return self.reply_builder.handled()
