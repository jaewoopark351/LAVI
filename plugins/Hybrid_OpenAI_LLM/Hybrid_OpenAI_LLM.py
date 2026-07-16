import os
import sys

import gradio as gr

from plugin_system.interfaces import LLMPluginInterface
from core.logger import log_print
from core.event_manager import event_manager, EventType


current_module_directory = os.path.dirname(__file__)
if current_module_directory not in sys.path:
    sys.path.insert(0, current_module_directory)


from hybrid_openai_core.engine import RouterFirstHybridEngine
from hybrid_openai_core.providers import (
    DisabledLocalLightChatProvider,
    OpenAIChatProvider,
)
from hybrid_openai_core.routing import (
    CommandOverrideRouter,
    MemoryRouterProvider_OpenAI,
    OpenAIRouteProvider,
)
from hybrid_openai_core.settings import HybridOpenAISettings


class Hybrid_OpenAI_LLM(LLMPluginInterface):
    PLUGIN_METADATA = {
        "id": "Hybrid_OpenAI_LLM",
        "display_name": "Hybrid OpenAI LLM",
        "api_version": "1",
        "dependency_group": "Full",
        "capabilities": ("llm", "openai_chat", "memory_router"),
        "required_python_packages": ("openai",),
        "required_files": (),
        "required_executables": (),
        "required_services": ("OpenAI API",),
        "supports_offline": False,
        "supports_cpu": True,
    }

    def init(self):
        self.base_dir = os.path.dirname(__file__)
        self.settings = HybridOpenAISettings.load()
        #20260629_kpopmodder: Keep the interrupt subscription owned by this plugin instance.
        self._interrupt_subscription = getattr(
            self,
            "_interrupt_subscription",
            None,
        )
        self._build_runtime()
        self._subscribe_interrupt()

    def _subscribe_interrupt(self):
        subscription = getattr(self, "_interrupt_subscription", None)
        if subscription is not None and subscription.active:
            return

        #20260629_kpopmodder: Store the EventSubscription so shutdown can unsubscribe it.
        self._interrupt_subscription = event_manager.subscribe(
            EventType.INTERRUPT,
            self.handle_interrupt,
        )

    def _build_runtime(self):
        old_local_provider = getattr(self, "local_provider", None)
        if old_local_provider is not None:
            try:
                old_local_provider.unload()
            except Exception as e:
                log_print(f"[Hybrid_OpenAI_LLM] local unload warning: {e}")

        self.command_router = CommandOverrideRouter()
        self.route_provider = OpenAIRouteProvider(
            settings=self.settings,
            log_print=log_print,
        )
        self.memory_router_provider = MemoryRouterProvider_OpenAI(
            settings=self.settings,
            log_print=log_print,
        )
        self.openai_provider = OpenAIChatProvider(
            settings=self.settings,
            log_print=log_print,
        )
        if self.settings.local_enabled:
            log_print(
                "[Hybrid_OpenAI_LLM] local_enabled ignored; "
                "local_light is disabled in OpenAI-only mode"
            )
        self.local_provider = DisabledLocalLightChatProvider(
            log_print=log_print,
        )
        self.engine = RouterFirstHybridEngine(
            command_router=self.command_router,
            route_provider=self.route_provider,
            memory_router_provider=self.memory_router_provider,
            openai_provider=self.openai_provider,
            local_provider=self.local_provider,
            log_print=log_print,
        )

    def shutdown(self):
        self._unsubscribe_interrupt()
        try:
            self.local_provider.unload()
        except Exception as e:
            log_print(f"[Hybrid_OpenAI_LLM] shutdown warning: {e}")

    def _unsubscribe_interrupt(self):
        subscription = getattr(self, "_interrupt_subscription", None)
        if subscription is None:
            return

        try:
            #20260629_kpopmodder: Unsubscribe only active interrupt subscriptions during shutdown.
            if subscription.active:
                subscription.unsubscribe()
        except Exception as e:
            log_print(f"[Hybrid_OpenAI_LLM] interrupt unsubscribe warning: {e}")
        finally:
            self._interrupt_subscription = None

    def handle_interrupt(self):
        try:
            self.local_provider.request_interrupt()
        except Exception as e:
            log_print(f"[Hybrid_OpenAI_LLM] interrupt warning: {e}")

    def create_ui(self):
        with gr.Accordion("Hybrid OpenAI LLM settings", open=False):
            with gr.Row():
                self.openai_model_name_input = gr.Textbox(
                    label="OpenAI chat model",
                    value=self.settings.openai_model_name,
                    placeholder="gpt-4o-mini",
                )
                self.openai_model_name_input.change(
                    fn=self.update_openai_model_name,
                    inputs=[self.openai_model_name_input],
                    outputs=[],
                )

                self.route_model_name_input = gr.Textbox(
                    label="OpenAI route model",
                    value=self.settings.route_model_name,
                    placeholder="gpt-4o-mini",
                )
                self.route_model_name_input.change(
                    fn=self.update_route_model_name,
                    inputs=[self.route_model_name_input],
                    outputs=[],
                )

            with gr.Row():
                self.temperature_slider = gr.Slider(
                    minimum=0,
                    maximum=1.5,
                    value=self.settings.temperature,
                    step=0.05,
                    label="OpenAI temperature",
                )
                self.temperature_slider.change(
                    fn=self.update_temperature,
                    inputs=[self.temperature_slider],
                    outputs=[],
                )

                self.max_history_input = gr.Number(
                    label="OpenAI max history pairs",
                    value=self.settings.max_history_pairs,
                    precision=0,
                )
                self.max_history_input.change(
                    fn=self.update_max_history_pairs,
                    inputs=[self.max_history_input],
                    outputs=[],
                )

            with gr.Row():
                self.local_model_id_input = gr.Textbox(
                    label="Local light model_id",
                    value=self.settings.local_model_id,
                    placeholder="Qwen/Qwen2.5-0.5B-Instruct",
                )
                self.local_model_id_input.change(
                    fn=self.update_local_model_id,
                    inputs=[self.local_model_id_input],
                    outputs=[],
                )

                self.local_max_new_tokens_input = gr.Number(
                    label="Local max_new_tokens",
                    value=self.settings.local_max_new_tokens,
                    precision=0,
                )
                self.local_max_new_tokens_input.change(
                    fn=self.update_local_max_new_tokens,
                    inputs=[self.local_max_new_tokens_input],
                    outputs=[],
                )

            with gr.Row():
                self.local_temperature_slider = gr.Slider(
                    minimum=0,
                    maximum=1.5,
                    value=self.settings.local_temperature,
                    step=0.05,
                    label="Local temperature",
                )
                self.local_temperature_slider.change(
                    fn=self.update_local_temperature,
                    inputs=[self.local_temperature_slider],
                    outputs=[],
                )

                self.local_max_history_input = gr.Number(
                    label="Local max history pairs",
                    value=self.settings.local_max_history_pairs,
                    precision=0,
                )
                self.local_max_history_input.change(
                    fn=self.update_local_max_history_pairs,
                    inputs=[self.local_max_history_input],
                    outputs=[],
                )

            self.reload_button = gr.Button("Reload Hybrid OpenAI LLM")
            self.reload_button.click(
                fn=self.reload_runtime,
                inputs=[],
                outputs=[],
            )

    def reload_runtime(self):
        self._build_runtime()

    def update_openai_model_name(self, value):
        value = str(value or "").strip() or "gpt-4o-mini"
        self.settings.save("openai_model_name", value)
        self._build_runtime()

    def update_route_model_name(self, value):
        value = str(value or "").strip() or "gpt-4o-mini"
        self.settings.save("route_model_name", value)
        self._build_runtime()

    def update_temperature(self, value):
        try:
            value = float(value)
        except Exception:
            value = 0.8
        self.settings.save("temperature", value)

    def update_max_history_pairs(self, value):
        try:
            value = max(0, int(value))
        except Exception:
            value = 8
        self.settings.save("max_history_pairs", value)

    def update_local_model_id(self, value):
        value = str(value or "").strip() or "Qwen/Qwen2.5-0.5B-Instruct"
        self.settings.save("local_model_id", value)
        self._build_runtime()

    def update_local_max_new_tokens(self, value):
        try:
            value = max(16, int(value))
        except Exception:
            value = 96
        self.settings.save("local_max_new_tokens", value)
        self._build_runtime()

    def update_local_temperature(self, value):
        try:
            value = float(value)
        except Exception:
            value = 0.7
        self.settings.save("local_temperature", value)

    def update_local_max_history_pairs(self, value):
        try:
            value = max(0, int(value))
        except Exception:
            value = 2
        self.settings.save("local_max_history_pairs", value)
        self._build_runtime()

    def predict(self, message, history, system_prompt):
        try:
            yield from self.engine.stream(
                message=message,
                history=history,
                system_prompt=system_prompt,
            )
        except Exception as e:
            error_message = f"[Hybrid_OpenAI_LLM 오류] {e}"
            log_print(error_message)
            yield error_message
