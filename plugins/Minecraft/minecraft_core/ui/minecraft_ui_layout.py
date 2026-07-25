#20260725_kpopmodder: Added layout builder so Gradio component creation is separate from event wiring.
from __future__ import annotations

from typing import Dict

import gradio as gr


class MinecraftUiLayout:
    def create(self, initial_status_text: str) -> Dict[str, object]:
        with gr.Tab("Minecraft"):
            status_box = gr.Textbox(
                label="Bridge Status",
                value=initial_status_text,
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

        return {
            "status_box": status_box,
            "health_button": health_button,
            "status_button": status_button,
            "inventory_button": inventory_button,
            "current_action_button": current_action_button,
            "stop_button": stop_button,
            "item_box": item_box,
            "count_box": count_box,
            "get_item_button": get_item_button,
            "goto_target_box": goto_target_box,
            "goto_button": goto_button,
        }
