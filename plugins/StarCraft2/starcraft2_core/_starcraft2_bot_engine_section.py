#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260713_kpopmodder: Move StarCraft2 UI section builders into core helper module.

from __future__ import annotations

from typing import Any, List, Tuple

import gradio as gr

class _StarCraft2BotEngineSection:
    def __init__(self, owner: Any, sc2_race_choices: List[str]):
        self.owner = owner
        self.sc2_race_choices = sc2_race_choices
        self.inputs = []
        self.outputs = []

    def build(self, config: dict) -> Tuple[list[Any], list[Any]]:
        owner = self.owner
        with gr.Accordion("Bot Engine", open=False):
            with gr.Row():
                owner.map_name_box = gr.Textbox(
                    label="Map Name",
                    value=str(config.get("map_name", "AbyssalReefLE")),
                    lines=1,
                )
                owner.engine_dropdown = gr.Dropdown(
                    label="Engine",
                    choices=owner.engine_registry.names(),
                    value=str(config.get("engine", "internal_lav_bot")),
                    interactive=True,
                )
            with gr.Row():
                owner.race_dropdown = gr.Dropdown(
                    label="Player Race",
                    choices=self.sc2_race_choices,
                    value=str(config.get("race", "Terran")),
                    interactive=True,
                )
                owner.enemy_race_dropdown = gr.Dropdown(
                    label="Enemy Race",
                    choices=self.sc2_race_choices,
                    value=str(config.get("enemy_race", "Zerg")),
                    interactive=True,
                )
                owner.enemy_difficulty_dropdown = gr.Dropdown(
                    label="Enemy Difficulty",
                    choices=["VeryEasy", "Easy", "Medium", "MediumHard", "Hard"],
                    value=str(config.get("enemy_difficulty", "Easy")),
                    interactive=True,
                )
            owner.realtime_box = gr.Checkbox(
                label="Realtime",
                value=bool(config.get("realtime", False)),
            )
            with gr.Row():
                owner.external_exe_path_box = gr.Textbox(
                    label="External Exe Path",
                    value=str(config.get("external_exe", {}).get("path", "")),
                    lines=1,
                )
                owner.micromachine_path_box = gr.Textbox(
                    label="MicroMachine Exe Path",
                    value=str(config.get("micromachine", {}).get("path", "")),
                    lines=1,
                )
                owner.ares_sc2_script_box = gr.Textbox(
                    label="Ares-sc2 Script Path",
                    value=str(config.get("ares_sc2", {}).get("script_path", "")),
                    lines=1,
                )
                owner.external_jar_path_box = gr.Textbox(
                    label="External Jar Path",
                    value=str(config.get("external_jar", {}).get("jar_path", "")),
                    lines=1,
                )
            with gr.Row():
                owner.start_button = gr.Button("Start")
                owner.stop_button = gr.Button("Stop")
                owner.status_button = gr.Button("Status")
            owner.status_box = gr.Textbox(
                label="Status",
                value=owner._status_json(),
                lines=12,
                interactive=False,
            )
            owner.last_event_box = gr.Textbox(
                label="Last Event",
                value="",
                lines=4,
                interactive=False,
            )
            owner.last_error_box = gr.Textbox(
                label="Last Error",
                value="",
                lines=2,
                interactive=False,
            )

        #20260713_kpopmodder: Keep callback inputs/outputs grouped with the section.
        self.inputs = [
            owner.enabled_box,
            owner.starcraft2_path_box,
            owner.map_name_box,
            owner.race_dropdown,
            owner.enemy_race_dropdown,
            owner.enemy_difficulty_dropdown,
            owner.realtime_box,
            owner.engine_dropdown,
            owner.external_exe_path_box,
            owner.micromachine_path_box,
            owner.ares_sc2_script_box,
            owner.external_jar_path_box,
        ]
        self.outputs = [
            owner.config_status_box,
            owner.status_box,
            owner.last_event_box,
            owner.last_error_box,
        ]
        return self.inputs, self.outputs

    def bind(self):
        owner = self.owner
        owner.start_button.click(
            fn=owner.on_start_click,
            inputs=self.inputs,
            outputs=self.outputs,
        )
        owner.stop_button.click(
            fn=owner.on_stop_click,
            inputs=[],
            outputs=self.outputs,
            queue=False,
        )
        owner.status_button.click(
            fn=owner.on_status_click,
            inputs=[],
            outputs=self.outputs,
            queue=False,
        )
