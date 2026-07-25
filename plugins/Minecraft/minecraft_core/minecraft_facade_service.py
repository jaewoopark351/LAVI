#20260725_kpopmodder: Added a small facade so UI and GameExtension share bridge behavior.
from __future__ import annotations

from typing import Any, Dict

from .actions.minecraft_action_service import MinecraftActionService
from .bridge.chatclef_bridge_client import ChatClefBridgeClient
from .commands.minecraft_command_router import MinecraftCommandRouter
from .minecraft_config import MinecraftConfig
from .status.minecraft_status_json_formatter import MinecraftStatusJsonFormatter


class MinecraftFacadeService:
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

    def reload(self) -> Dict[str, Any]:
        return self.action_service.reload()

    def health(self) -> Dict[str, Any]:
        return self.action_service.health()

    def status(self) -> Dict[str, Any]:
        return self.action_service.status()

    def inventory(self) -> Dict[str, Any]:
        return self.action_service.inventory()

    def current_action(self) -> Dict[str, Any]:
        return self.action_service.current_action()

    def get_item(self, item: Any, count: Any = 1) -> Dict[str, Any]:
        return self.action_service.get_item(item, count)

    def goto(
        self,
        target: Any = None,
        *,
        x: Any = None,
        y: Any = None,
        z: Any = None,
        dimension: Any = None,
    ) -> Dict[str, Any]:
        return self.action_service.goto(
            target,
            x=x,
            y=y,
            z=z,
            dimension=dimension,
        )

    def stop(self) -> Dict[str, Any]:
        return self.action_service.stop()

    def handle_command(self, command: Any) -> Dict[str, Any]:
        return self.command_router.handle_command(command)

    def get_status(self) -> Dict[str, Any]:
        return self.action_service.get_status()

    def status_json(self, payload: Dict[str, Any] | None = None) -> str:
        return self.status_json_formatter.format(
            payload if isinstance(payload, dict) else self.get_status()
        )

    def public_config(self) -> Dict[str, Any]:
        return self.action_service.public_config()

    @property
    def client(self):
        return self.action_service.client
