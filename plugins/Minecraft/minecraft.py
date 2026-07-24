#20260725_kpopmodder: Added optional Minecraft facade for LAVI UI and extension commands.
from __future__ import annotations

from typing import Any, Dict

import gradio as gr

from plugins.Minecraft.minecraft_core import MinecraftConfig, MinecraftFacadeService


class Minecraft:
    def __init__(self):
        self.config_manager = MinecraftConfig()
        self.facade_service = MinecraftFacadeService(self.config_manager)

    def create_ui(self):
        with gr.Tab("Minecraft"):
            status_box = gr.Textbox(
                label="Bridge Status",
                value=self.initial_status_text(),
                lines=16,
                interactive=False,
            )
            with gr.Row():
                health_button = gr.Button("Health")
                status_button = gr.Button("Status")
                inventory_button = gr.Button("Inventory")
                current_action_button = gr.Button("Current Action")
                stop_button = gr.Button("Stop")
            with gr.Row():
                item_box = gr.Textbox(label="Item", value="oak_log", lines=1)
                count_box = gr.Textbox(label="Count", value="1", lines=1)
                get_item_button = gr.Button("Get Item")

            health_button.click(fn=self.on_health_click, outputs=status_box)
            status_button.click(fn=self.on_status_click, outputs=status_box)
            inventory_button.click(fn=self.on_inventory_click, outputs=status_box)
            current_action_button.click(
                fn=self.on_current_action_click,
                outputs=status_box,
            )
            stop_button.click(fn=self.on_stop_click, outputs=status_box)
            get_item_button.click(
                fn=self.on_get_item_click,
                inputs=[item_box, count_box],
                outputs=status_box,
            )

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

    def stop(self) -> Dict[str, Any]:
        return self.facade_service.stop()

    def handle_command(self, command: Any) -> Dict[str, Any]:
        return self.facade_service.handle_command(command)

    def get_status(self) -> Dict[str, Any]:
        return self.facade_service.get_status()

    def reload(self) -> Dict[str, Any]:
        return self.facade_service.reload()

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
        return self.facade_service.status_json(self.health())

    def on_status_click(self) -> str:
        return self.facade_service.status_json(self.get_status())

    def on_inventory_click(self) -> str:
        return self.facade_service.status_json(self.inventory())

    def on_current_action_click(self) -> str:
        return self.facade_service.status_json(self.current_action())

    def on_get_item_click(self, item: Any, count: Any) -> str:
        return self.facade_service.status_json(self.get_item(item, count))

    def on_stop_click(self) -> str:
        return self.facade_service.status_json(self.stop())
