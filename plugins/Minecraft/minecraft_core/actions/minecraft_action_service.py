#20260725_kpopmodder: Added orchestrator for focused Minecraft bridge action services.
from __future__ import annotations

from typing import Any, Dict

from ..minecraft_config import MinecraftConfig
from .minecraft_action_service_components import MinecraftActionServiceComponents


class MinecraftActionService:
    def __init__(
        self,
        config_manager: MinecraftConfig | None = None,
        client_factory=None,
    ):
        components = MinecraftActionServiceComponents(
            config_manager=config_manager,
            client_factory=client_factory,
        )
        self.config_manager = components.config_manager
        self.client_provider = components.client_provider
        self.read_actions = components.read_actions
        self.write_actions = components.write_actions
        self.status_service = components.status_service
        self.reload_service = components.reload_service

    @property
    def client(self):
        return self.client_provider.client

    def reload(self) -> Dict[str, Any]:
        return self.reload_service.reload(self.public_config)

    def health(self) -> Dict[str, Any]:
        return self.read_actions.health()

    def status(self) -> Dict[str, Any]:
        return self.read_actions.status()

    def inventory(self) -> Dict[str, Any]:
        return self.read_actions.inventory()

    def current_action(self) -> Dict[str, Any]:
        return self.read_actions.current_action()

    def get_item(self, item: Any, count: Any = 1) -> Dict[str, Any]:
        return self.write_actions.get_item(item, count)

    def goto(
        self,
        target: Any = None,
        *,
        x: Any = None,
        y: Any = None,
        z: Any = None,
        dimension: Any = None,
    ) -> Dict[str, Any]:
        return self.write_actions.goto(
            target,
            x=x,
            y=y,
            z=z,
            dimension=dimension,
        )

    def stop(self) -> Dict[str, Any]:
        return self.write_actions.stop()

    def get_status(self) -> Dict[str, Any]:
        return self.status_service.get_status()

    def public_config(self) -> Dict[str, Any]:
        return self.status_service.public_config()
