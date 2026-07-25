#20260725_kpopmodder: Added event binder so Minecraft UI layout stays free of callback wiring.
from __future__ import annotations

from typing import Dict

from .minecraft_ui_controller import MinecraftUiController


class MinecraftUiEventBinder:
    def __init__(self, controller: MinecraftUiController):
        self.controller = controller

    def bind(self, components: Dict[str, object]) -> None:
        status_box = components["status_box"]
        components["health_button"].click(
            fn=self.controller.on_health_click,
            outputs=status_box,
        )
        components["status_button"].click(
            fn=self.controller.on_status_click,
            outputs=status_box,
        )
        components["inventory_button"].click(
            fn=self.controller.on_inventory_click,
            outputs=status_box,
        )
        components["current_action_button"].click(
            fn=self.controller.on_current_action_click,
            outputs=status_box,
        )
        components["stop_button"].click(
            fn=self.controller.on_stop_click,
            outputs=status_box,
        )
        components["run_command_button"].click(
            fn=self.controller.on_run_command_click,
            inputs=components["command_box"],
            outputs=status_box,
        )
        components["get_item_button"].click(
            fn=self.controller.on_get_item_click,
            inputs=[components["item_box"], components["count_box"]],
            outputs=status_box,
        )
        components["equip_button"].click(
            fn=self.controller.on_equip_click,
            inputs=components["equip_item_box"],
            outputs=status_box,
        )
        components["goto_button"].click(
            fn=self.controller.on_goto_click,
            inputs=components["goto_target_box"],
            outputs=status_box,
        )
