#20260725_kpopmodder: Added natural reply text for Minecraft actions still running after the first wait.
from __future__ import annotations

from typing import Any, Mapping

from .minecraft_conversation_action_metadata_reader import (
    MinecraftConversationActionMetadataReader,
)
from .minecraft_conversation_command_route import MinecraftConversationCommandRoute
from .minecraft_conversation_item_name_formatter import (
    MinecraftConversationItemNameFormatter,
)


class MinecraftConversationPendingReplyBuilder:
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
        item_text = self.item_name_formatter.format(request.get("item"))

        if action in {"craft", "make"} and item_text:
            return f"\uc544\uc9c1 {item_text} \ub9cc\ub4dc\ub294 \uc911\uc774\uc57c. \uc2dc\uac04\uc774 \uc870\uae08 \ub354 \uac78\ub9ac\uace0 \uc788\uc5b4."
        if action in {"get_item", "get"} and item_text:
            return f"\uc544\uc9c1 {item_text} \uad6c\ud558\ub294 \uc911\uc774\uc57c. \uc2dc\uac04\uc774 \uc870\uae08 \ub354 \uac78\ub9ac\uace0 \uc788\uc5b4."
        if action in {"get_and_equip", "getandequip"} and item_text:
            return f"\uc544\uc9c1 {item_text} \ucc59\uae30\ub294 \uc911\uc774\uc57c. \ub05d\ub098\uba74 \ub2e4\uc2dc \ub9d0\ud574\uc904\uac8c."
        if action == "goto":
            return "\uc544\uc9c1 \uc774\ub3d9 \uc911\uc774\uc57c. \ub3c4\ucc29\ud558\uba74 \ub2e4\uc2dc \ub9d0\ud574\uc904\uac8c."
        return "\uc544\uc9c1 \ub9c8\ud06c\uc5d0\uc11c \uc9c4\ud589 \uc911\uc774\uc57c. \ub05d\ub098\uba74 \ub2e4\uc2dc \ub9d0\ud574\uc904\uac8c."
