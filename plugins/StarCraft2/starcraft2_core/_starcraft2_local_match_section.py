#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260713_kpopmodder: Move StarCraft2 UI section builders into core helper module.

from __future__ import annotations

from typing import Any, List, Tuple

import gradio as gr

class _StarCraft2LocalMatchSection:
    def __init__(self, owner: Any, sc2_race_choices: List[str]):
        self.owner = owner
        self.sc2_race_choices = sc2_race_choices
        self.inputs = []
        self.start_inputs = []

    def build(self, config: dict) -> Tuple[list[Any], list[Any]]:
        owner = self.owner
        local_match_config = owner.config_manager.get_section("local_match")
        ladder_config = owner.config_manager.get_section("ladder_proxy")
        local_match_args = local_match_config.get("args", ladder_config.get("args", []))
        if isinstance(local_match_args, list):
            local_match_args_text = " ".join(str(item) for item in local_match_args)
        else:
            local_match_args_text = str(local_match_args or "")
        with gr.Accordion("Local Match", open=False):
            with gr.Row():
                owner.local_match_exe_path_box = gr.Textbox(
                    label="Local Match Exe Path",
                    value=str(local_match_config.get("executable_path", "")),
                    lines=1,
                )
                owner.local_match_working_dir_box = gr.Textbox(
                    label="Local Match Working Dir",
                    value=str(local_match_config.get("working_directory", "")),
                    lines=1,
                )
            with gr.Row():
                #20260710_kpopmodder: Keep the human race separate from the selectable AI race.
                owner.local_match_race_dropdown = gr.Dropdown(
                    label="Local Human Race",
                    choices=self.sc2_race_choices,
                    value=owner.local_match_race_from_args(
                        local_match_args
                    ),
                    interactive=True,
                )
                owner.local_match_ai_race_dropdown = gr.Dropdown(
                    label="Local AI Race",
                    choices=self.sc2_race_choices,
                    value=owner.local_match_ai_race_from_args(
                        local_match_args
                    ),
                    interactive=True,
                )
                owner.local_match_ports_box = gr.Textbox(
                    label="Local Proxy Ports",
                    value=",".join(
                        str(port) for port in local_match_config.get("ports", [5677, 5678])
                    )
                    if isinstance(local_match_config.get("ports", [5677, 5678]), list)
                    else str(local_match_config.get("ports", "5677,5678")),
                    lines=1,
                )
            owner.local_match_bot_display_name_box = gr.Textbox(
                label="Local AI Display Name",
                value=str(local_match_config.get("bot_display_name", "LAVI")),
                lines=1,
            )
            owner.local_match_args_box = gr.Textbox(
                label="Local Human vs AI Args",
                value=local_match_args_text,
                lines=1,
            )
            with gr.Row():
                owner.local_human_vs_changeling_button = gr.Button("Local Human vs AI")
                owner.local_match_stop_button = gr.Button("Stop Local Match")
                owner.local_match_status_button = gr.Button("Local Match Status")
            owner.local_match_status_box = gr.Textbox(
                label="Local Match Status",
                value=owner.local_match_status_json(),
                lines=8,
                interactive=False,
            )
            self.inputs = [
                owner.local_match_exe_path_box,
                owner.local_match_working_dir_box,
                owner.local_match_args_box,
                owner.local_match_ports_box,
            ]
            self.start_inputs = self.inputs + [
                owner.local_match_ai_race_dropdown,
                owner.local_match_bot_display_name_box,
            ]
        return self.inputs, self.start_inputs

    def bind(self):
        owner = self.owner
        owner.local_match_race_dropdown.change(
            fn=owner.on_local_match_race_change,
            inputs=[owner.local_match_race_dropdown, owner.local_match_args_box],
            outputs=[owner.local_match_args_box],
            queue=False,
        )
        owner.local_match_ai_race_dropdown.change(
            fn=owner.on_local_match_ai_race_change,
            inputs=[owner.local_match_ai_race_dropdown, owner.local_match_args_box],
            outputs=[owner.local_match_args_box],
            queue=False,
        )
        owner.local_human_vs_changeling_button.click(
            fn=owner.on_local_human_vs_changeling_click,
            inputs=self.start_inputs,
            outputs=[owner.local_match_status_box],
            queue=False,
        )
        owner.local_match_stop_button.click(
            fn=owner.on_local_match_stop_click,
            inputs=[],
            outputs=[owner.local_match_status_box],
            queue=False,
        )
        owner.local_match_status_button.click(
            fn=owner.on_local_match_status_click,
            inputs=self.inputs,
            outputs=[owner.local_match_status_box],
            queue=False,
        )
