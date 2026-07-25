#20260725_kpopmodder: Added write action coordinator for Minecraft bridge actions.
from __future__ import annotations

from typing import Any, Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from ..minecraft_config import MinecraftConfig
from .minecraft_write_action_registry import MinecraftWriteActionRegistry


class MinecraftWriteActionService:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        client_provider: MinecraftBridgeClientProvider,
        action_registry: MinecraftWriteActionRegistry | None = None,
    ):
        self.action_registry = action_registry or MinecraftWriteActionRegistry.defaults(
            config_manager,
            client_provider,
        )

    def get_item(self, item: Any, count: Any = 1) -> Dict[str, object]:
        return self.action_registry.get("get_item").run(item, count)

    def equip(self, item: Any) -> Dict[str, object]:
        return self.action_registry.get("equip").run(item)

    def goto(
        self,
        target: Any = None,
        *,
        x: Any = None,
        y: Any = None,
        z: Any = None,
        dimension: Any = None,
    ) -> Dict[str, object]:
        return self.action_registry.get("goto").run(
            target,
            x=x,
            y=y,
            z=z,
            dimension=dimension,
        )

    def stop(self) -> Dict[str, object]:
        return self.action_registry.get("stop").run()
