#20260725_kpopmodder: Added Gradio UI builder for the Minecraft bridge tab.
from __future__ import annotations

import gradio as gr

from .minecraft_ui_controller import MinecraftUiController


class MinecraftUiBuilder:
    def __init__(self, controller: MinecraftUiController):
        self.controller = controller

    def create_ui(self) -> None:
        with gr.Tab("Minecraft"):
            status_box = gr.Textbox(
                label="Bridge Status",
                value=self.controller.initial_status_text(),
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
            with gr.Row():
                goto_target_box = gr.Textbox(
                    label="Go To Target",
                    value="0 64 0 overworld",
                    lines=1,
                )
                goto_button = gr.Button("Go To")

            health_button.click(
                fn=self.controller.on_health_click,
                outputs=status_box,
            )
            status_button.click(
                fn=self.controller.on_status_click,
                outputs=status_box,
            )
            inventory_button.click(
                fn=self.controller.on_inventory_click,
                outputs=status_box,
            )
            current_action_button.click(
                fn=self.controller.on_current_action_click,
                outputs=status_box,
            )
            stop_button.click(
                fn=self.controller.on_stop_click,
                outputs=status_box,
            )
            get_item_button.click(
                fn=self.controller.on_get_item_click,
                inputs=[item_box, count_box],
                outputs=status_box,
            )
            goto_button.click(
                fn=self.controller.on_goto_click,
                inputs=goto_target_box,
                outputs=status_box,
            )
