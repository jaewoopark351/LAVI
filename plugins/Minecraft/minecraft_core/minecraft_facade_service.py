#20260725_kpopmodder: Added a small facade so UI and GameExtension share bridge behavior.
from __future__ import annotations

from typing import Any, Dict

from .minecraft_config import MinecraftConfig
from .minecraft_facade_components import MinecraftFacadeComponents


class MinecraftFacadeService:
    def __init__(
        self,
        config_manager: MinecraftConfig | None = None,
        client_factory=None,
    ):
        components = MinecraftFacadeComponents(
            config_manager=config_manager,
            client_factory=client_factory,
        )
        self.config_manager = components.config_manager
        self.client_factory = components.client_factory
        self.action_service = components.action_service
        self.command_router = components.command_router
        self.status_json_formatter = components.status_json_formatter

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

    def get_and_equip(self, item: Any, count: Any = 1) -> Dict[str, Any]:
        return self.action_service.get_and_equip(item, count)

    def equip(self, item: Any) -> Dict[str, Any]:
        return self.action_service.equip(item)

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
