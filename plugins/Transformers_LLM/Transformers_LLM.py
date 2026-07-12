# import os#20260626_kpopmodder : 모듈 사용 중지로 인한 주석처리
# import sys

# import gradio as gr

# from plugin_system.interfaces import LLMPluginInterface
# from core.logger import log_print
# from core.event_manager import event_manager, EventType


# current_module_directory = os.path.dirname(__file__)

# if current_module_directory not in sys.path:
#     sys.path.insert(0, current_module_directory)


# from transformers_llm_core.transformers_settings import TransformersSettings
# from transformers_llm_core.transformers_client import TransformersClient


# class Transformers_LLM(LLMPluginInterface):#20260619_kpopmodder
#     def init(self):
#         self.settings = TransformersSettings.load()
#         self.client = TransformersClient(
#             settings=self.settings,
#             log_print=log_print,
#         )
#         event_manager.subscribe(EventType.INTERRUPT, self.handle_interrupt)

#     def handle_interrupt(self):
#         self.client.request_interrupt()

#     def create_ui(self):
#         with gr.Accordion("Transformers LLM settings", open=False):
#             with gr.Row():
#                 self.model_id_input = gr.Textbox(
#                     label="Hugging Face model_id",
#                     value=self.settings.model_id,
#                     placeholder="Bllossom/llama-3.2-Korean-Bllossom-3B",
#                 )
#                 self.model_id_input.change(
#                     fn=self.update_model_id,
#                     inputs=[self.model_id_input],
#                     outputs=[],
#                 )

#             with gr.Row():
#                 self.temperature_slider = gr.Slider(
#                     minimum=0,
#                     maximum=1.5,
#                     value=self.settings.temperature,
#                     step=0.05,
#                     label="temperature",
#                 )
#                 self.temperature_slider.change(
#                     fn=self.update_temperature,
#                     inputs=[self.temperature_slider],
#                     outputs=[],
#                 )

#                 self.max_new_tokens_input = gr.Number(
#                     label="max_new_tokens",
#                     value=self.settings.max_new_tokens,
#                     precision=0,
#                 )
#                 self.max_new_tokens_input.change(
#                     fn=self.update_max_new_tokens,
#                     inputs=[self.max_new_tokens_input],
#                     outputs=[],
#                 )

#             with gr.Row():
#                 self.top_p_slider = gr.Slider(
#                     minimum=0.1,
#                     maximum=1.0,
#                     value=self.settings.top_p,
#                     step=0.05,
#                     label="top_p",
#                 )
#                 self.top_p_slider.change(
#                     fn=self.update_top_p,
#                     inputs=[self.top_p_slider],
#                     outputs=[],
#                 )

#                 self.top_k_input = gr.Number(
#                     label="top_k",
#                     value=self.settings.top_k,
#                     precision=0,
#                 )
#                 self.top_k_input.change(
#                     fn=self.update_top_k,
#                     inputs=[self.top_k_input],
#                     outputs=[],
#                 )

#             with gr.Row():
#                 self.repetition_penalty_slider = gr.Slider(
#                     minimum=1.0,
#                     maximum=2.0,
#                     value=self.settings.repetition_penalty,
#                     step=0.05,
#                     label="repetition_penalty",
#                 )
#                 self.repetition_penalty_slider.change(
#                     fn=self.update_repetition_penalty,
#                     inputs=[self.repetition_penalty_slider],
#                     outputs=[],
#                 )

#                 self.max_history_input = gr.Number(
#                     label="Max history pairs",
#                     value=self.settings.max_history_pairs,
#                     precision=0,
#                 )
#                 self.max_history_input.change(
#                     fn=self.update_max_history_pairs,
#                     inputs=[self.max_history_input],
#                     outputs=[],
#                 )

#             with gr.Row():
#                 self.torch_dtype_input = gr.Dropdown(
#                     label="torch_dtype",
#                     choices=["float16", "bfloat16", "float32", "auto"],
#                     value=self.settings.torch_dtype,
#                 )
#                 self.torch_dtype_input.change(
#                     fn=self.update_torch_dtype,
#                     inputs=[self.torch_dtype_input],
#                     outputs=[],
#                 )

#                 self.device_map_input = gr.Textbox(
#                     label="device_map",
#                     value=self.settings.device_map,
#                     placeholder="auto",
#                 )
#                 self.device_map_input.change(
#                     fn=self.update_device_map,
#                     inputs=[self.device_map_input],
#                     outputs=[],
#                 )

#             self.reload_button = gr.Button("Reload Transformers model")
#             self.reload_button.click(
#                 fn=self.reload_model,
#                 inputs=[],
#                 outputs=[],
#             )

#     def _reload_client(self):
#         try:
#             self.client.unload()
#         except Exception as e:
#             log_print(f"[Transformers_LLM] unload warning: {e}")

#         self.client = TransformersClient(
#             settings=self.settings,
#             log_print=log_print,
#         )

#     def reload_model(self):
#         self._reload_client()
#         self.client.load()

#     def update_model_id(self, value):
#         if value is None:
#             value = ""

#         value = value.strip() or "Bllossom/llama-3.2-Korean-Bllossom-3B"
#         self.settings.save("model_id", value)
#         self._reload_client()

#     def update_temperature(self, value):
#         try:
#             value = float(value)
#         except Exception:
#             value = 0.7

#         self.settings.save("temperature", value)

#     def update_max_new_tokens(self, value):
#         try:
#             value = max(16, int(value))
#         except Exception:
#             value = 256

#         self.settings.save("max_new_tokens", value)

#     def update_top_p(self, value):
#         try:
#             value = float(value)
#         except Exception:
#             value = 0.9

#         self.settings.save("top_p", value)

#     def update_top_k(self, value):
#         try:
#             value = max(1, int(value))
#         except Exception:
#             value = 50

#         self.settings.save("top_k", value)

#     def update_repetition_penalty(self, value):
#         try:
#             value = float(value)
#         except Exception:
#             value = 1.1

#         self.settings.save("repetition_penalty", value)

#     def update_max_history_pairs(self, value):
#         try:
#             value = max(0, int(value))
#         except Exception:
#             value = 5

#         self.settings.save("max_history_pairs", value)

#     def update_torch_dtype(self, value):
#         if value is None:
#             value = "float16"

#         self.settings.save("torch_dtype", value)
#         self._reload_client()

#     def update_device_map(self, value):
#         if value is None:
#             value = "auto"

#         value = value.strip() or "auto"
#         self.settings.save("device_map", value)
#         self._reload_client()

#     def predict(self, message, history, system_prompt):
#         try:
#             yield from self.client.stream(
#                 message=message,
#                 history=history,
#                 system_prompt=system_prompt,
#             )
#         except Exception as e:
#             error_message = f"[Transformers_LLM 오류] {e}"
#             log_print(error_message)
#             yield error_message
