#20260725_kpopmodder: Added reload service so config/client refresh is separate from action facade methods.
from __future__ import annotations

from typing import Callable, Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from ..minecraft_config import MinecraftConfig


class MinecraftReloadService:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        client_provider: MinecraftBridgeClientProvider,
    ):
        self.config_manager = config_manager
        self.client_provider = client_provider

    def reload(
        self,
        public_config_builder: Callable[[], Dict[str, object]],
    ) -> Dict[str, object]:
        self.config_manager.reload()
        self.client_provider.reload()
        return {
            "ok": True,
            "action": "reload",
            "config": public_config_builder(),
        }
