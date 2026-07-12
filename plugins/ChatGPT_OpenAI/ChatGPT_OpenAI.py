import os
import gradio as gr
from openai import OpenAI

from plugin_system.interfaces import LLMPluginInterface
from core.config_manager import config_manager
from core.logger import log_print, debug_print#20260612_kpopmodder


class ChatGPT_OpenAI(LLMPluginInterface):#20260610_kpopmodder
    context_length = 4096
    temperature = 0.8

    plugin_config = config_manager.load_section("ChatGPT_OpenAI")

    api_key = plugin_config.get("api_key", "")
    save_api_key_to_config = (
        str(plugin_config.get("save_api_key_to_config", "false")).strip().lower()
        in {"1", "true", "yes", "on"}
    )#20260703_kpopmodder: Plaintext API key persistence must be explicit.
    model_name = plugin_config.get("model_name", "gpt-4o-mini")
    max_history_pairs = int(plugin_config.get("max_history_pairs", "10") or "10")

    def init(self):
        self.client = None

    def create_ui(self):
        with gr.Accordion("ChatGPT OpenAI settings", open=False):
            with gr.Row():
                self.api_key_input = gr.Textbox(
                    label="OpenAI API Key",
                    value=self.api_key,
                    type="password",
                    placeholder="sk-..."
                )
                self.save_api_key_checkbox = gr.Checkbox(
                    label="Save API key to config.ini",
                    value=self.save_api_key_to_config,
                )
                self.api_key_input.change(
                    fn=self.update_api_key,
                    inputs=[self.api_key_input, self.save_api_key_checkbox]
                )
                self.save_api_key_checkbox.change(
                    fn=self.update_api_key_save_option,
                    inputs=self.save_api_key_checkbox,
                )

                self.model_name_input = gr.Textbox(
                    label="Model Name",
                    value=self.model_name,
                    placeholder="gpt-4o-mini"
                )
                self.model_name_input.change(
                    fn=self.update_model_name,
                    inputs=self.model_name_input
                )

            with gr.Row():
                self.temperature_slider = gr.Slider(
                    minimum=0,
                    maximum=1.5,
                    value=self.temperature,
                    step=0.1,
                    label="temperature"
                )
                self.temperature_slider.change(
                    fn=self.update_temperature,
                    inputs=self.temperature_slider
                )

                self.max_history_input = gr.Number(
                    label="Max history pairs",
                    value=self.max_history_pairs,
                    precision=0
                )
                self.max_history_input.change(
                    fn=self.update_max_history_pairs,
                    inputs=self.max_history_input
                )

    def update_api_key(self, value, save_to_config=None):
        self.api_key = value.strip()
        self.client = None
        if save_to_config is None:
            save_to_config = self.save_api_key_to_config
        self.save_api_key_to_config = bool(save_to_config)
        config_manager.save_config(
            "ChatGPT_OpenAI",
            "save_api_key_to_config",
            "true" if self.save_api_key_to_config else "false",
        )
        if self.save_api_key_to_config:
            config_manager.save_config("ChatGPT_OpenAI", "api_key", self.api_key)
        else:
            config_manager.remove_config("ChatGPT_OpenAI", "api_key")

    def update_api_key_save_option(self, value):
        self.save_api_key_to_config = bool(value)
        config_manager.save_config(
            "ChatGPT_OpenAI",
            "save_api_key_to_config",
            "true" if self.save_api_key_to_config else "false",
        )
        if self.save_api_key_to_config:
            config_manager.save_config("ChatGPT_OpenAI", "api_key", self.api_key)
        else:
            config_manager.remove_config("ChatGPT_OpenAI", "api_key")

    def update_model_name(self, value):
        self.model_name = value.strip() or "gpt-4o-mini"
        self.client = None
        config_manager.save_config("ChatGPT_OpenAI", "model_name", self.model_name)

    def update_temperature(self, value):
        self.temperature = float(value)

    def update_max_history_pairs(self, value):
        try:
            self.max_history_pairs = max(0, int(value))
        except Exception:
            self.max_history_pairs = 10

        config_manager.save_config(
            "ChatGPT_OpenAI",
            "max_history_pairs",
            str(self.max_history_pairs)
        )

    def _get_client(self):
        if self.client is None:
            key = os.getenv("OPENAI_API_KEY", "").strip() or self.api_key

            if not key:
                raise RuntimeError(
                    "OpenAI API key가 없습니다. UI에서 API Key를 넣거나 OPENAI_API_KEY 환경변수를 설정하세요."
                )

            self.client = OpenAI(api_key=key)

        return self.client

    def _build_messages(self, message, history, system_prompt):
        messages = []

        if system_prompt and system_prompt.strip():
            messages.append({
                "role": "system",
                "content": system_prompt.strip()
            })
        else:
            messages.append({
                "role": "system",
                "content": (
                    "너는 한국어로 자연스럽게 대화하는 AI 버튜버다. "
                    "답변은 너무 길지 않게, 친근하고 생동감 있게 말한다."
                )
            })

        trimmed_history = history[-self.max_history_pairs:] if history else []

        for entry in trimmed_history:
            try:
                user, ai = entry
            except Exception:
                continue

            if user:
                messages.append({"role": "user", "content": str(user)})
            if ai:
                messages.append({"role": "assistant", "content": str(ai)})

        messages.append({
            "role": "user",
            "content": message
        })

        return messages

    def predict(self, message, history, system_prompt):
        try:
            client = self._get_client()
            messages = self._build_messages(message, history, system_prompt)

            log_print(f"message: {message}")#20260612_kpopmodder
            log_print(f"model: {self.model_name}")#20260612_kpopmodder
            log_print(f"temperature: {self.temperature}")#20260612_kpopmodder
            log_print("---------------------------------")#20260612_kpopmodder

            stream = client.chat.completions.create(
                model=self.model_name,
                messages=messages,
                temperature=self.temperature,
                top_p=1.0,
                stream=True
            )

            output = ""

            for chunk in stream:
                try:
                    delta = chunk.choices[0].delta.content or ""
                    if not delta:
                        continue

#                    log_print(delta, end="", flush=True)#20260612_kpopmodder
                    output += delta
                    yield output

                except Exception as e:
                    log_print(f"Stream chunk error: {e}")#20260612_kpopmodder
            
            log_print(f"response: {output}")#20260612_kpopmodder

        except Exception as e:
            error_message = f"[ChatGPT_OpenAI 오류] {e}"
            log_print(error_message)#20260612_kpopmodder
            yield error_message
