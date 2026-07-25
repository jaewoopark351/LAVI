#20260725_kpopmodder: Added write action registry so adding actions does not grow the service constructor.
from __future__ import annotations

from typing import Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from ..minecraft_config import MinecraftConfig
from .minecraft_get_item_action import MinecraftGetItemAction
from .minecraft_goto_action import MinecraftGotoAction
from .minecraft_stop_action import MinecraftStopAction


class MinecraftWriteActionRegistry:
    def __init__(self, actions: Dict[str, object]):
        self.actions = actions

    @classmethod
    def defaults(
        cls,
        config_manager: MinecraftConfig,
        client_provider: MinecraftBridgeClientProvider,
    ) -> "MinecraftWriteActionRegistry":
        return cls(
            {
                "get_item": MinecraftGetItemAction(config_manager, client_provider),
                "goto": MinecraftGotoAction(config_manager, client_provider),
                "stop": MinecraftStopAction(client_provider),
            }
        )

    def get(self, action_name: str):
        try:
            return self.actions[action_name]
        except KeyError as error:
            raise ValueError(f"Unsupported Minecraft write action: {action_name}") from error
