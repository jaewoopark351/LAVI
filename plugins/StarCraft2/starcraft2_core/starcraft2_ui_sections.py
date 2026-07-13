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


#20260713_kpopmodder: Keep local-match UI construction and callbacks in helper class.
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
