#20260725_kpopmodder: Added optional Minecraft facade for LAVI UI and extension commands.
from __future__ import annotations

from typing import Any, Dict

from plugins.Minecraft.minecraft_core import (
    MinecraftConfig,
    MinecraftFacadeService,
)
from plugins.Minecraft.minecraft_core.ui.minecraft_ui_builder import MinecraftUiBuilder
from plugins.Minecraft.minecraft_core.ui.minecraft_ui_controller import (
    MinecraftUiController,
)


class Minecraft:
    def __init__(self):
        self.config_manager = MinecraftConfig()
        self.facade_service = MinecraftFacadeService(self.config_manager)
        self.ui_controller = MinecraftUiController(
            self.config_manager,
            self.facade_service,
        )
        self.ui_builder = MinecraftUiBuilder(self.ui_controller)

    def create_ui(self):
        self.ui_builder.create_ui()

    def start(self) -> None:
        return None

    def shutdown(self) -> None:
        return None

    def health(self) -> Dict[str, Any]:
        return self.facade_service.health()

    def status(self) -> Dict[str, Any]:
        return self.facade_service.status()

    def inventory(self) -> Dict[str, Any]:
        return self.facade_service.inventory()

    def current_action(self) -> Dict[str, Any]:
        return self.facade_service.current_action()

    def get_item(self, item: Any, count: Any = 1) -> Dict[str, Any]:
        return self.facade_service.get_item(item, count)

    def goto(self, target: Any) -> Dict[str, Any]:
        return self.facade_service.goto(target)

    def stop(self) -> Dict[str, Any]:
        return self.facade_service.stop()

    def handle_command(self, command: Any) -> Dict[str, Any]:
        return self.facade_service.handle_command(command)

    def get_status(self) -> Dict[str, Any]:
        return self.facade_service.get_status()

    def reload(self) -> Dict[str, Any]:
        return self.facade_service.reload()

    def initial_status_text(self) -> str:
        return self.ui_controller.initial_status_text()

    def on_health_click(self) -> str:
        return self.ui_controller.on_health_click()

    def on_status_click(self) -> str:
        return self.ui_controller.on_status_click()

    def on_inventory_click(self) -> str:
        return self.ui_controller.on_inventory_click()

    def on_current_action_click(self) -> str:
        return self.ui_controller.on_current_action_click()

    def on_get_item_click(self, item: Any, count: Any) -> str:
        return self.ui_controller.on_get_item_click(item, count)

    def on_goto_click(self, target: Any) -> str:
        return self.ui_controller.on_goto_click(target)

    def on_stop_click(self) -> str:
        return self.ui_controller.on_stop_click()
