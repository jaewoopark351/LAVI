#20260725_kpopmodder: Added action-specific Minecraft completion phrases for delayed notifications.
from __future__ import annotations

from typing import Any, Mapping

from .minecraft_conversation_action_metadata_reader import (
    MinecraftConversationActionMetadataReader,
)
from .minecraft_conversation_command_route import MinecraftConversationCommandRoute
from .minecraft_conversation_item_name_formatter import (
    MinecraftConversationItemNameFormatter,
)


class MinecraftConversationCompletionPhraseBuilder:
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
            return f"{item_text} \uad6c\ud574\ub1a8\uc5b4"
        if action in {"craft", "make"} and item_text:
            return f"{item_text} \ub9cc\ub4e4\uc5b4\ub1a8\uc5b4"
        if action in {"equip", "hold"} and item_text:
            return f"{item_text} \uc7a5\ucc29\ud588\uc5b4"
        if action in {"get_and_equip", "getandequip"} and item_text:
            return f"{item_text} \ucc59\uaca8\uc11c \uc7a5\ucc29\ud588\uc5b4"
        if action == "goto":
            return self._goto_phrase(target)
        if action == "stop":
            return "\uba48\ucd9c\uc5c8\uc5b4"
        return "\ub9c8\ud06c \uc791\uc5c5 \ub05d\ub0ac\uc5b4"

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
            return f"{target}\uc5d0 \ub3c4\ucc29\ud588\uc5b4"
        return "\uadf8 \uc88c\ud45c\uc5d0 \ub3c4\ucc29\ud588\uc5b4"
