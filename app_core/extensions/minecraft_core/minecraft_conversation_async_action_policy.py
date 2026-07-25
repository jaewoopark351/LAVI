#20260725_kpopmodder: Added policy for which Minecraft conversation actions should finish in the background.
from __future__ import annotations

from typing import Any, Mapping

from .minecraft_conversation_action_metadata_reader import (
    MinecraftConversationActionMetadataReader,
)
from .minecraft_conversation_command_route import MinecraftConversationCommandRoute


class MinecraftConversationAsyncActionPolicy:
    ASYNC_ACTIONS = {
        "get",
        "get_item",
        "get_and_equip",
        "getandequip",
        "craft",
        "make",
        "equip",
        "hold",
        "select",
        "goto",
    }

    def __init__(
        self,
        metadata_reader: MinecraftConversationActionMetadataReader | None = None,
    ):
        self.metadata_reader = metadata_reader or MinecraftConversationActionMetadataReader()

    def should_run_async(
        self,
        route: MinecraftConversationCommandRoute,
        preview_result: Mapping[str, Any] | None,
    ) -> bool:
        if not isinstance(preview_result, Mapping):
            return False
        if not preview_result.get("ok") or preview_result.get("accepted") is not True:
            return False
        action = self.metadata_reader.action_type(route, preview_result)
        return action in self.ASYNC_ACTIONS
