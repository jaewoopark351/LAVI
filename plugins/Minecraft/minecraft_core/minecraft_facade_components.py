#20260725_kpopmodder: Added composition object so MinecraftFacadeService only exposes facade methods.
from __future__ import annotations

from .actions.minecraft_action_service import MinecraftActionService
from .bridge.chatclef_bridge_client import ChatClefBridgeClient
from .commands.minecraft_command_router import MinecraftCommandRouter
from .minecraft_config import MinecraftConfig
from .status.minecraft_status_json_formatter import MinecraftStatusJsonFormatter


class MinecraftFacadeComponents:
    def __init__(
        self,
        config_manager: MinecraftConfig | None = None,
        client_factory=None,
    ):
        self.config_manager = config_manager or MinecraftConfig()
        self.client_factory = client_factory or ChatClefBridgeClient
        self.action_service = MinecraftActionService(
            self.config_manager,
            self.client_factory,
        )
        self.command_router = MinecraftCommandRouter(self.action_service)
        self.status_json_formatter = MinecraftStatusJsonFormatter()
