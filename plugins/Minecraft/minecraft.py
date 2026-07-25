#20260725_kpopmodder: Added optional Minecraft facade for LAVI UI and extension commands.
from __future__ import annotations

from typing import Any, Dict

from plugins.Minecraft.minecraft_core.minecraft_plugin_components import (
    MinecraftPluginComponents,
)


class Minecraft:
    def __init__(self):
        components = MinecraftPluginComponents()
        self.config_manager = components.config_manager
        self.facade_service = components.facade_service
        self.ui_controller = components.ui_controller
        self.ui_builder = components.ui_builder

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

    def get_and_equip(self, item: Any, count: Any = 1) -> Dict[str, Any]:
        return self.facade_service.get_and_equip(item, count)

    def equip(self, item: Any) -> Dict[str, Any]:
        return self.facade_service.equip(item)

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

    def on_get_and_equip_click(self, item: Any, count: Any) -> str:
        return self.ui_controller.on_get_and_equip_click(item, count)

    def on_equip_click(self, item: Any) -> str:
        return self.ui_controller.on_equip_click(item)

    def on_goto_click(self, target: Any) -> str:
        return self.ui_controller.on_goto_click(target)

    def on_stop_click(self) -> str:
        return self.ui_controller.on_stop_click()
