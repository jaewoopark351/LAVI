#20260725_kpopmodder: Added UI callback controller so the Minecraft plugin facade stays focused.
from __future__ import annotations

from typing import Any

from ..minecraft_config import MinecraftConfig
from ..minecraft_facade_service import MinecraftFacadeService


class MinecraftUiController:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        facade_service: MinecraftFacadeService,
    ):
        self.config_manager = config_manager
        self.facade_service = facade_service

    def initial_status_text(self) -> str:
        return self.facade_service.status_json(
            {
                "ok": True,
                "message": self.config_manager.config_message(),
                "config": self.facade_service.public_config(),
                "bridge_checked": False,
            }
        )

    def on_health_click(self) -> str:
        return self.facade_service.status_json(self.facade_service.health())

    def on_status_click(self) -> str:
        return self.facade_service.status_json(self.facade_service.get_status())

    def on_inventory_click(self) -> str:
        return self.facade_service.status_json(self.facade_service.inventory())

    def on_current_action_click(self) -> str:
        return self.facade_service.status_json(self.facade_service.current_action())

    def on_get_item_click(self, item: Any, count: Any) -> str:
        return self.facade_service.status_json(
            self.facade_service.get_item(item, count)
        )

    def on_get_and_equip_click(self, item: Any, count: Any) -> str:
        return self.facade_service.status_json(
            self.facade_service.get_and_equip(item, count)
        )

    def on_equip_click(self, item: Any) -> str:
        return self.facade_service.status_json(self.facade_service.equip(item))

    def on_goto_click(self, target: Any) -> str:
        return self.facade_service.status_json(self.facade_service.goto(target))

    def on_stop_click(self) -> str:
        return self.facade_service.status_json(self.facade_service.stop())

    def on_run_command_click(self, command: Any) -> str:
        return self.facade_service.status_json(
            self.facade_service.handle_command(command)
        )
