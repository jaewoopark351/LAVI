#20260620_kpopmodder: GPTSoVITS helper modules are grouped under gpt_sovits_core without changing behavior.
import os

import gradio as gr

from core.logger import log_print


class GPTSoVITSSettingsController:#20260616_kpopmodder
    def __init__(self, gpt_sovits, rvc, get_use_rvc_callback, set_use_rvc_callback):
        self.gpt_sovits = gpt_sovits
        self.rvc = rvc

        self.get_use_rvc_callback = get_use_rvc_callback
        self.set_use_rvc_callback = set_use_rvc_callback

    def on_gpt_sovits_url_change(self, value):
        self.gpt_sovits.gpt_sovits_url = value.strip()

    def on_gpt_ckpt_change(self, choice):
        self.gpt_sovits.set_gpt_weight_by_name(choice)

    def on_sovits_model_change(self, choice):
        self.gpt_sovits.set_sovits_weight_by_name(choice)

    def on_gpt_sovits_refresh(self):
        self.gpt_sovits.update_model_list()

        return (
            gr.update(
                choices=self.gpt_sovits.gpt_ckpt_names,
                value=(
                    os.path.basename(self.gpt_sovits.gpt_weight_path)
                    if self.gpt_sovits.gpt_weight_path
                    else None
                )
            ),
            gr.update(
                choices=self.gpt_sovits.sovits_model_names,
                value=(
                    os.path.basename(self.gpt_sovits.sovits_weight_path)
                    if self.gpt_sovits.sovits_weight_path
                    else None
                )
            )
        )

    def on_text_language_change(self, value):
        self.gpt_sovits.text_language = value.strip() or "ko"

    def on_prompt_language_change(self, value):
        self.gpt_sovits.prompt_language = value.strip() or "ko"

    def on_ref_audio_path_change(self, value):
        self.gpt_sovits.ref_audio_path = value.strip()

    def on_prompt_text_change(self, value):
        self.gpt_sovits.prompt_text = value.strip()

    def on_use_rvc_change(self, use):
        self.set_use_rvc_callback(use)
        log_print(f"[GPTSoVITSSettingsController] use_rvc={use}")

    def on_rvc_model_change(self, choice):
        self.rvc.load_model(choice)

    def on_rvc_refresh(self):
        self.rvc.update_model_list()

        return gr.update(
            choices=self.rvc.rvc_model_names,
            value=(
                self.rvc.rvc_model_names[0]
                if self.rvc.rvc_model_names
                else None
            )
        )

    def on_transpose_change(self, value):
        self.rvc.set_transpose(value)

    def on_index_rate_change(self, value):
        self.rvc.set_index_rate(value)

    def on_protect_change(self, value):
        self.rvc.set_protect(value)

    def download_model_from_url(self, url):
        self.rvc.download_model_from_url(url)
