#20260725_kpopmodder: Added action-specific Minecraft reply phrases.
from __future__ import annotations

from typing import Any, Mapping

from .minecraft_conversation_action_metadata_reader import (
    MinecraftConversationActionMetadataReader,
)
from .minecraft_conversation_command_route import MinecraftConversationCommandRoute
from .minecraft_conversation_item_name_formatter import (
    MinecraftConversationItemNameFormatter,
)


class MinecraftConversationActionPhraseBuilder:
    def __init__(
        self,
        item_name_formatter: MinecraftConversationItemNameFormatter | None = None,
        metadata_reader: MinecraftConversationActionMetadataReader | None = None,
    ):
        self.item_name_formatter = (
            item_name_formatter or MinecraftConversationItemNameFormatter()
        )
        self.metadata_reader = metadata_reader or MinecraftConversationActionMetadataReader()

    def build(
        self,
        route: MinecraftConversationCommandRoute,
        result: Mapping[str, Any],
    ) -> str:
        action = self.metadata_reader.action_type(route, result)
        request = self.metadata_reader.request(route, result)
        item_text = self._item_text(request)
        target = str(request.get("target") or "").strip()

        if action in {"get_item", "get"} and item_text:
            return f"{item_text} \uad6c\ud574\ubcfc\uac8c"
        if action in {"craft", "make"} and item_text:
            return f"{item_text} \ub9cc\ub4e4\uc5b4\ubcfc\uac8c"
        if action in {"equip", "hold"} and item_text:
            return f"{item_text} \ub4e4\uc5b4\ubcfc\uac8c"
        if action in {"get_and_equip", "getandequip"} and item_text:
            return f"{item_text} \ucc59\uaca8\uc11c \uc7a5\ucc29\ud574\ubcfc\uac8c"
        if action == "goto":
            return self._goto_phrase(target)
        if action == "stop":
            return "\uc9c0\uae08 \ud558\ub358 \uac70 \uba48\ucd9c\uac8c"
        if action == "inventory":
            return "\uc778\ubca4\ud1a0\ub9ac \ud655\uc778\ud574\ubcfc\uac8c"
        if action == "health":
            return "\uccb4\ub825 \ud655\uc778\ud574\ubcfc\uac8c"
        if action == "status":
            return "\uc0c1\ud0dc \ud655\uc778\ud574\ubcfc\uac8c"
        if action == "current_action":
            return "\uc9c0\uae08 \ud558\ub294 \uc77c \ud655\uc778\ud574\ubcfc\uac8c"
        return ""

    def _item_text(self, request: Mapping[str, Any]) -> str:
        item = self.item_name_formatter.format(request.get("item"))
        if not item:
            return ""
        count = self._count(request.get("count"))
        if count is not None and count > 1:
            return f"{item} {count}\uac1c"
        return item

    def _count(self, value: Any) -> int | None:
        try:
            count = int(value)
        except (TypeError, ValueError):
            return None
        return count if count > 0 else None

    def _goto_phrase(self, target: str) -> str:
        if target:
            return f"{target}\ub85c \uc774\ub3d9\ud574\ubcfc\uac8c"
        return "\uadf8 \uc88c\ud45c\ub85c \uc774\ub3d9\ud574\ubcfc\uac8c"
