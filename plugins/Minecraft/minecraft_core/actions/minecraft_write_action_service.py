#20260725_kpopmodder: Added write action coordinator for Minecraft bridge actions.
from __future__ import annotations

from typing import Any, Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from ..minecraft_config import MinecraftConfig
from .minecraft_get_item_action import MinecraftGetItemAction
from .minecraft_goto_action import MinecraftGotoAction
from .minecraft_stop_action import MinecraftStopAction


class MinecraftWriteActionService:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        client_provider: MinecraftBridgeClientProvider,
    ):
        self.get_item_action = MinecraftGetItemAction(
            config_manager,
            client_provider,
        )
        self.goto_action = MinecraftGotoAction(
            config_manager,
            client_provider,
        )
        self.stop_action = MinecraftStopAction(client_provider)

    def get_item(self, item: Any, count: Any = 1) -> Dict[str, object]:
        return self.get_item_action.run(item, count)

    def goto(
        self,
        target: Any = None,
        *,
        x: Any = None,
        y: Any = None,
        z: Any = None,
        dimension: Any = None,
    ) -> Dict[str, object]:
        return self.goto_action.run(
            target,
            x=x,
            y=y,
            z=z,
            dimension=dimension,
        )

    def stop(self) -> Dict[str, object]:
        return self.stop_action.run()
