#20260725_kpopmodder: Added natural Minecraft conversation reply text builder.
from __future__ import annotations

from typing import Any, Mapping

from .minecraft_conversation_action_phrase_builder import (
    MinecraftConversationActionPhraseBuilder,
)
from .minecraft_conversation_command_route import MinecraftConversationCommandRoute
from .minecraft_conversation_failure_reply_builder import (
    MinecraftConversationFailureReplyBuilder,
)


class MinecraftConversationReplyBuilder:
    def __init__(
        self,
        phrase_builder: MinecraftConversationActionPhraseBuilder | None = None,
        failure_reply_builder: MinecraftConversationFailureReplyBuilder | None = None,
    ):
        self.phrase_builder = phrase_builder or MinecraftConversationActionPhraseBuilder()
        self.failure_reply_builder = (
            failure_reply_builder or MinecraftConversationFailureReplyBuilder()
        )

    def extension_missing(self) -> str:
        return "\uc544\uc9c1 \ub9c8\ud06c\uc640 \uc5f0\uacb0\uc774 \uc548 \ubd99\uc5c8\uc5b4. \ub9c8\ud06c \ud0ed\uc5d0\uc11c \uc0c1\ud0dc\ubd80\ud130 \ud655\uc778\ud574\ubcfc\uae4c?"

    def failed(self, detail: str) -> str:
        return self.failure_reply_builder.build(detail)

    def accepted(
        self,
        route: MinecraftConversationCommandRoute,
        result: Mapping[str, Any],
    ) -> str:
        phrase = self.phrase_builder.build(route, result)
        if phrase:
            return f"\uc751. {phrase}."
        return "\uc751. \ub9c8\ud06c\uc5d0\uc11c \ud574\ubcfc\uac8c."

    def handled(self, message: str = "") -> str:
        clean_message = str(message or "").strip()
        if clean_message:
            return f"\ud655\uc778\ud588\uc5b4. {clean_message}"
        return "\ud655\uc778\ud588\uc5b4."
