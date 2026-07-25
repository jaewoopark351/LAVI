#20260725_kpopmodder: Added action service composition to keep dependency assembly in one place.
from __future__ import annotations

from ..bridge.chatclef_bridge_client import ChatClefBridgeClient
from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from ..minecraft_config import MinecraftConfig
from ..status.minecraft_status_service import MinecraftStatusService
from .minecraft_read_action_service import MinecraftReadActionService
from .minecraft_reload_service import MinecraftReloadService
from .minecraft_write_action_service import MinecraftWriteActionService


class MinecraftActionServiceComponents:
    def __init__(
        self,
        config_manager: MinecraftConfig | None = None,
        client_factory=None,
    ):
        self.config_manager = config_manager or MinecraftConfig()
        self.client_provider = MinecraftBridgeClientProvider(
            self.config_manager,
            client_factory or ChatClefBridgeClient,
        )
        self.read_actions = MinecraftReadActionService(self.client_provider)
        self.write_actions = MinecraftWriteActionService(
            self.config_manager,
            self.client_provider,
        )
        self.status_service = MinecraftStatusService(
            self.config_manager,
            self.read_actions,
        )
        self.reload_service = MinecraftReloadService(
            self.config_manager,
            self.client_provider,
        )
