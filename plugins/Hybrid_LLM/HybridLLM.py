# import os
# import sys

# import gradio as gr

# from plugin_system.interfaces import LLMPluginInterface
# from core.logger import log_print

# current_module_directory = os.path.dirname(__file__)
# if current_module_directory not in sys.path:
#     sys.path.insert(0, current_module_directory)

# from hybrid_llm_core.hybrid_settings import HybridSettings
# from hybrid_llm_core.prompt_builder import PromptBuilder
# from hybrid_llm_core.hybrid_router import HybridRouter
# from hybrid_llm_core.local_gguf_client import LocalGGUFClient
# from hybrid_llm_core.openai_stream_client import OpenAIStreamClient


# class HybridLLM(LLMPluginInterface):#20260618_kpopmodder
#     def init(self):
#         self.base_dir = os.path.dirname(__file__)

#         self.settings = HybridSettings.load()
#         self.prompt_builder = PromptBuilder()
#         self.router = HybridRouter(self.base_dir, log_print)

#         self.openai_client = OpenAIStreamClient(self.settings, log_print)

#         self.local_client = LocalGGUFClient(
#             settings=self.settings,
#             base_dir=self.base_dir,
#             log_print=log_print
#         )
#         self.local_client.load()

#     def create_ui(self):
#         with gr.Accordion("Hybrid LLM settings", open=False):
#             with gr.Row():
#                 self.openai_api_key_input = gr.Textbox(
#                     label="OpenAI API Key",
#                     value=self.settings.openai_api_key,
#                     type="password",
#                     placeholder="sk-proj-..."
#                 )
#                 self.openai_api_key_input.change(
#                     fn=self.update_openai_api_key,
#                     inputs=[self.openai_api_key_input],
#                     outputs=[]
#                 )

#                 self.openai_model_name_input = gr.Textbox(
#                     label="OpenAI Model",
#                     value=self.settings.openai_model_name,
#                     placeholder="gpt-4o-mini"
#                 )
#                 self.openai_model_name_input.change(
#                     fn=self.update_openai_model_name,
#                     inputs=[self.openai_model_name_input],
#                     outputs=[]
#                 )

#             with gr.Row():
#                 self.local_model_filename_input = gr.Textbox(
#                     label="Local GGUF filename",
#                     value=self.settings.local_model_filename,
#                     placeholder="ggml-model-Q5_K_M.gguf"
#                 )
#                 self.local_model_filename_input.change(
#                     fn=self.update_local_model_filename,
#                     inputs=[self.local_model_filename_input],
#                     outputs=[]
#                 )

#                 self.temperature_slider = gr.Slider(
#                     minimum=0,
#                     maximum=1.5,
#                     value=self.settings.temperature,
#                     step=0.1,
#                     label="temperature"
#                 )
#                 self.temperature_slider.change(
#                     fn=self.update_temperature,
#                     inputs=[self.temperature_slider],
#                     outputs=[]
#                 )

#             with gr.Row():
#                 self.max_history_input = gr.Number(
#                     label="Max history pairs",
#                     value=self.settings.max_history_pairs,
#                     precision=0
#                 )
#                 self.max_history_input.change(
#                     fn=self.update_max_history_pairs,
#                     inputs=[self.max_history_input],
#                     outputs=[]
#                 )

#             with gr.Row():
#                 self.n_gpu_layers_input = gr.Number(
#                     label="GGUF n_gpu_layers (-1 = auto/all)",
#                     value=self.settings.n_gpu_layers,
#                     precision=0
#                 )
#                 self.n_gpu_layers_input.change(
#                     fn=self.update_n_gpu_layers,
#                     inputs=[self.n_gpu_layers_input],
#                     outputs=[]
#                 )

#                 self.n_batch_input = gr.Number(
#                     label="GGUF n_batch",
#                     value=self.settings.n_batch,
#                     precision=0
#                 )
#                 self.n_batch_input.change(
#                     fn=self.update_n_batch,
#                     inputs=[self.n_batch_input],
#                     outputs=[]
#                 )

#     def update_openai_api_key(self, value):
#         if value is None:
#             value = ""

#         self.settings.save("openai_api_key", value.strip())
#         self.openai_client.reset()

#     def update_openai_model_name(self, value):
#         if value is None:
#             value = ""

#         self.settings.save("openai_model_name", value.strip() or "gpt-4o-mini")
#         self.openai_client.reset()

#     def update_local_model_filename(self, value):
#         if value is None:
#             value = ""

#         self.settings.save("local_model_filename", value.strip())

#         # 모델 파일명 변경은 즉시 반영하려면 재로드 필요
#         self.local_client = LocalGGUFClient(
#             settings=self.settings,
#             base_dir=self.base_dir,
#             log_print=log_print
#         )
#         self.local_client.load()

#     def update_temperature(self, value):
#         try:
#             if value is None:
#                 value = 0.8
#             value = float(value)
#         except Exception:
#             value = 0.8

#         self.settings.save("temperature", value)

#     def update_max_history_pairs(self, value):
#         try:
#             if value is None:
#                 value = 10
#             value = max(0, int(value))
#         except Exception:
#             value = 10

#         self.settings.save("max_history_pairs", value)

#     def update_n_gpu_layers(self, value):
#         try:
#             if value is None:
#                 value = -1
#             value = int(value)
#         except Exception:
#             value = -1

#         self.settings.save("n_gpu_layers", value)

#         # GPU 설정 변경은 모델 재로드 필요
#         self.local_client = LocalGGUFClient(
#             settings=self.settings,
#             base_dir=self.base_dir,
#             log_print=log_print
#         )
#         self.local_client.load()

#     def update_n_batch(self, value):
#         try:
#             if value is None:
#                 value = 512
#             value = max(1, int(value))
#         except Exception:
#             value = 512

#         self.settings.save("n_batch", value)

#         # batch 변경도 모델 재로드 필요
#         self.local_client = LocalGGUFClient(
#             settings=self.settings,
#             base_dir=self.base_dir,
#             log_print=log_print
#         )
#         self.local_client.load()

#     def build_messages(self, message, history, system_prompt):
#         return self.prompt_builder.build_messages(
#             message=message,
#             history=history,
#             system_prompt=system_prompt,
#             max_history_pairs=self.settings.max_history_pairs
#         )

#     def predict_openai(self, message, history, system_prompt):
#         messages = self.build_messages(message, history, system_prompt)
#         yield from self.openai_client.stream(messages)

#     def predict_local(self, message, history, system_prompt):
#         messages = self.build_messages(message, history, system_prompt)
#         yield from self.local_client.stream(messages)

#     def predict(self, message, history, system_prompt):
#         try:
#             if self.router.should_use_openai(message):
#                 log_print("[HybridLLM] Route: OpenAI")
#                 yield from self.predict_openai(message, history, system_prompt)
#             else:
#                 log_print("[HybridLLM] Route: Local GGUF")
#                 yield from self.predict_local(message, history, system_prompt)

#         except Exception as e:
#             error_message = f"[HybridLLM 오류] {e}"
#             log_print(error_message)
#             yield error_message

# # import os
# # import gradio as gr
# # from openai import OpenAI
# # from llama_cpp import Llama

# # from plugin_system.interfaces import LLMPluginInterface
# # from core.config_manager import config_manager
# # from core.logger import log_print, debug_print#20260612_kpopmodder


# # class HybridLLM(LLMPluginInterface):#20260610_kpopmodder
# #     context_length = 8192

# #     plugin_config = config_manager.load_section("Hybrid_LLM")#20260618_kpopmodder

# #     #temperature = 0.8#20260618_kpopmodder
# #     try:#20260618_kpopmodder
# #         temperature = float(plugin_config.get("temperature", "0.8") or "0.8")
# #     except Exception:
# #         temperature = 0.8

# #     openai_api_key = plugin_config.get("openai_api_key", "")
# #     openai_model_name = plugin_config.get("openai_model_name", "gpt-4o-mini")

# #     local_model_filename = plugin_config.get(
# #         "local_model_filename",
# #         "ggml-model-Q5_K_M.gguf"
# #     )

# #     #max_history_pairs = int(plugin_config.get("max_history_pairs", "10") or "10")

# #     try:#20260618_kpopmodder
# #         max_history_pairs = int(plugin_config.get("max_history_pairs", "10") or "10")
# #     except Exception:
# #         max_history_pairs = 10

# #     def init(self):
# #         self.openai_client = None
# #         self.local_llm = None

# #         self.difficult_keywords = self.load_difficult_keywords()

# #         current_module_directory = os.path.dirname(__file__)
# #         model_directory = os.path.join(current_module_directory, "models")
# #         model_path = os.path.join(model_directory, self.local_model_filename)

# #         if not os.path.exists(model_path):
# #             log_print(f"[HybridLLM] Local GGUF model not found: {model_path}")#20260612_kpopmodder
# #             return

# #         log_print(f"[HybridLLM] Loading local model: {model_path}")#20260612_kpopmodder

# #         self.local_llm = Llama(
# #             model_path=model_path,
# #             n_ctx=self.context_length,
# #             n_gpu_layers=-1,
# #             seed=-1
# #         )

# #     def load_difficult_keywords(self):
# #         current_module_directory = os.path.dirname(__file__)
# #         keyword_path = os.path.join(current_module_directory, "difficult_keywords.txt")

# #         default_keywords = [
# #             "코드", "프로그래밍", "파이썬", "python", "에러", "오류",
# #             "수학", "계산", "증명", "논문", "전문", "법률", "의학",
# #             "네트워크", "Cisco", "Azure", "OpenAI", "API", "암호학",
# #             "양자", "보안", "GitHub", "Git", "설계", "분석"
# #         ]

# #         if not os.path.exists(keyword_path):
# #             with open(keyword_path, "w", encoding="utf-8") as f:
# #                 f.write("\n".join(default_keywords))

# #             log_print(f"[HybridLLM] Created difficult_keywords.txt: {keyword_path}")#20260612_kpopmodder
# #             return default_keywords

# #         keywords = []

# #         with open(keyword_path, "r", encoding="utf-8") as f:
# #             for line in f:
# #                 keyword = line.strip()

# #                 if not keyword:
# #                     continue

# #                 if keyword.startswith("#"):
# #                     continue

# #                 keywords.append(keyword)

# #         if not keywords:
# #             log_print("[HybridLLM] difficult_keywords.txt is empty. Using default keywords.")#20260612_kpopmodder
# #             return default_keywords

# #         log_print(f"[HybridLLM] Loaded keywords: {keywords}")#20260612_kpopmodder
# #         return keywords

# #     # def create_ui(self):#20260618_kpopmodder
# #     #     with gr.Accordion("Hybrid LLM settings", open=False):
# #     #         with gr.Row():
# #     #             self.openai_api_key_input = gr.Textbox(
# #     #                 label="OpenAI API Key",
# #     #                 value=self.openai_api_key,
# #     #                 type="password",
# #     #                 placeholder="sk-proj-..."
# #     #             )
# #     #             self.openai_api_key_input.change(
# #     #                 fn=self.update_openai_api_key,
# #     #                 inputs=self.openai_api_key_input
# #     #             )

# #     #             self.openai_model_name_input = gr.Textbox(
# #     #                 label="OpenAI Model",
# #     #                 value=self.openai_model_name,
# #     #                 placeholder="gpt-4o-mini"
# #     #             )
# #     #             self.openai_model_name_input.change(
# #     #                 fn=self.update_openai_model_name,
# #     #                 inputs=self.openai_model_name_input
# #     #             )

# #     #         with gr.Row():
# #     #             self.local_model_filename_input = gr.Textbox(
# #     #                 label="Local GGUF filename",
# #     #                 value=self.local_model_filename,
# #     #                 placeholder="ggml-model-Q5_K_M.gguf"
# #     #             )
# #     #             self.local_model_filename_input.change(
# #     #                 fn=self.update_local_model_filename,
# #     #                 inputs=self.local_model_filename_input
# #     #             )

# #     #             self.temperature_slider = gr.Slider(
# #     #                 minimum=0,
# #     #                 maximum=1.5,
# #     #                 value=self.temperature,
# #     #                 step=0.1,
# #     #                 label="temperature"
# #     #             )
# #     #             self.temperature_slider.change(
# #     #                 fn=self.update_temperature,
# #     #                 inputs=self.temperature_slider
# #     #             )

# #     #         with gr.Row():
# #     #             self.max_history_input = gr.Number(
# #     #                 label="Max history pairs",
# #     #                 value=self.max_history_pairs,
# #     #                 precision=0
# #     #             )
# #     #             # self.max_history_input.change(#20260618_kpopmodder
# #     #             #     fn=self.update_max_history_pairs,
# #     #             #     inputs=self.max_history_input
# #     #             # )
# #     #             self.max_history_input.change(#20260618_kpopmodder
# #     #                 fn=self.update_max_history_pairs,
# #     #                 inputs=[self.max_history_input],
# #     #                 outputs=[]
# #     #             )

# #     def create_ui(self):#20260618_kpopmodder
# #         with gr.Accordion("Hybrid LLM settings", open=False):
# #             with gr.Row():
# #                 self.openai_api_key_input = gr.Textbox(
# #                     label="OpenAI API Key",
# #                     value=self.openai_api_key,
# #                     type="password",
# #                     placeholder="sk-proj-..."
# #                 )
# #                 self.openai_api_key_input.change(
# #                     fn=self.update_openai_api_key,
# #                     inputs=[self.openai_api_key_input],
# #                     outputs=[]
# #                 )

# #                 self.openai_model_name_input = gr.Textbox(
# #                     label="OpenAI Model",
# #                     value=self.openai_model_name,
# #                     placeholder="gpt-4o-mini"
# #                 )
# #                 self.openai_model_name_input.change(
# #                     fn=self.update_openai_model_name,
# #                     inputs=[self.openai_model_name_input],
# #                     outputs=[]
# #                 )

# #             with gr.Row():
# #                 self.local_model_filename_input = gr.Textbox(
# #                     label="Local GGUF filename",
# #                     value=self.local_model_filename,
# #                     placeholder="ggml-model-Q5_K_M.gguf"
# #                 )
# #                 self.local_model_filename_input.change(
# #                     fn=self.update_local_model_filename,
# #                     inputs=[self.local_model_filename_input],
# #                     outputs=[]
# #                 )

# #                 self.temperature_slider = gr.Slider(
# #                     minimum=0,
# #                     maximum=1.5,
# #                     value=self.temperature,
# #                     step=0.1,
# #                     label="temperature"
# #                 )
# #                 self.temperature_slider.change(
# #                     fn=self.update_temperature,
# #                     inputs=[self.temperature_slider],
# #                     outputs=[]
# #                 )

# #             with gr.Row():
# #                 self.max_history_input = gr.Number(
# #                     label="Max history pairs",
# #                     value=self.max_history_pairs,
# #                     precision=0
# #                 )
# #                 self.max_history_input.change(
# #                     fn=self.update_max_history_pairs,
# #                     inputs=[self.max_history_input],
# #                     outputs=[]
# #                 )

# #     def update_openai_api_key(self, value):
# #         if value is None:#20260618_kpopmodder
# #             value = ""

# #         self.openai_api_key = value.strip()
# #         self.openai_client = None
# #         config_manager.save_config(
# #             "Hybrid_LLM",
# #             "openai_api_key",
# #             self.openai_api_key
# #         )

# #     def update_openai_model_name(self, value):
# #         if value is None:#20260618_kpopmodder
# #             value = ""

# #         self.openai_model_name = value.strip() or "gpt-4o-mini"
# #         self.openai_client = None
# #         config_manager.save_config(
# #             "Hybrid_LLM",
# #             "openai_model_name",
# #             self.openai_model_name
# #         )

# #     def update_local_model_filename(self, value):
# #         if value is None:#20260618_kpopmodder
# #             value = ""

# #         self.local_model_filename = value.strip()
# #         config_manager.save_config(
# #             "Hybrid_LLM",
# #             "local_model_filename",
# #             self.local_model_filename
# #         )

# #     def update_temperature(self, value):
# #         #self.temperature = float(value)#20260618_kpopmodder
# #         try:#20260618_kpopmodder
# #             if value is None:
# #                 value = 0.8

# #             self.temperature = float(value)

# #         except Exception:#20260618_kpopmodder
# #             self.temperature = 0.8

# #         config_manager.save_config(#20260618_kpopmodder
# #             "Hybrid_LLM",
# #             "temperature",
# #             str(self.temperature)
# #         )

# #     def update_max_history_pairs(self, value):
# #         try:
# #             if value is None:#20260618_kpopmodder
# #                 value = 10
# #             self.max_history_pairs = max(0, int(value))
# #         except Exception:
# #             self.max_history_pairs = 10

# #         config_manager.save_config(
# #             "Hybrid_LLM",
# #             "max_history_pairs",
# #             str(self.max_history_pairs)
# #         )

# #     def should_use_openai(self, message):
# #         message_lower = message.lower()

# #         for keyword in self.difficult_keywords:
# #             if keyword.lower() in message_lower:
# #                 log_print(f"[HybridLLM] GPT keyword detected: {keyword}")#20260612_kpopmodder
# #                 return True

# #         if len(message) >= 120:
# #             log_print("[HybridLLM] Long question detected. Using GPT.")#20260612_kpopmodder
# #             return True

# #         return False

# #     def build_messages(self, message, history, system_prompt):
# #         messages = []

# #         if system_prompt and system_prompt.strip():
# #             messages.append({
# #                 "role": "system",
# #                 "content": system_prompt.strip()
# #             })
# #         else:
# #             messages.append({
# #                 "role": "system",
# #                 "content": (
# #                     "너는 한국어로 자연스럽게 말하는 AI 버튜버다. "
# #                     "평소에는 짧고 친근하게 대답한다. "
# #                     "전문적인 질문에는 정확하고 이해하기 쉽게 설명한다."
# #                 )
# #             })

# #         trimmed_history = history[-self.max_history_pairs:] if history else []

# #         for entry in trimmed_history:
# #             try:
# #                 user, ai = entry
# #             except Exception:
# #                 continue

# #             if user:
# #                 messages.append({
# #                     "role": "user",
# #                     "content": str(user)
# #                 })

# #             if ai:
# #                 messages.append({
# #                     "role": "assistant",
# #                     "content": str(ai)
# #                 })

# #         messages.append({
# #             "role": "user",
# #             "content": message
# #         })

# #         return messages

# #     def get_openai_client(self):
# #         if self.openai_client is None:
# #             key = self.openai_api_key or os.getenv("OPENAI_API_KEY", "")

# #             if not key:
# #                 raise RuntimeError(
# #                     "OpenAI API Key가 없습니다. UI에서 키를 입력하거나 OPENAI_API_KEY 환경변수를 설정하세요."
# #                 )

# #             self.openai_client = OpenAI(api_key=key)

# #         return self.openai_client

# #     def predict_openai(self, message, history, system_prompt):
# #         client = self.get_openai_client()
# #         messages = self.build_messages(message, history, system_prompt)

# #         stream = client.chat.completions.create(
# #             model=self.openai_model_name,
# #             messages=messages,
# #             temperature=self.temperature,
# #             top_p=1.0,
# #             stream=True
# #         )

# #         output = ""

# #         for chunk in stream:
# #             try:
# #                 text = chunk.choices[0].delta.content or ""

# #                 if not text:
# #                     continue

# # #                log_print(text, end="", flush=True)#20260612_kpopmodder
# #                 output += text
# #                 yield output

# #             except Exception as e:
# #                 log_print(f"[HybridLLM] OpenAI stream error: {e}")#20260612_kpopmodder
        
# #         log_print(f"[HybridLLM][OpenAI] response: {output}")#20260612_kpopmodder

# #     def predict_local(self, message, history, system_prompt):
# #         if self.local_llm is None:
# #             yield "[HybridLLM 오류] 로컬 GGUF 모델을 찾을 수 없습니다."
# #             return

# #         messages = self.build_messages(message, history, system_prompt)

# #         def count_tokens(msg_list):
# #             total = 0

# #             for msg in msg_list:
# #                 content = str(msg.get("content", ""))
# #                 total += len(self.local_llm.tokenize(content.encode("utf-8")))

# #             log_print(f"[HybridLLM] Tokens in context: {total}")#20260612_kpopmodder
# #             return total

# #         while count_tokens(messages) > self.context_length and len(messages) > 1:
# #             messages.pop(1)

# #         completion_chunks = self.local_llm.create_chat_completion(
# #             messages=messages,
# #             stream=True,
# #             temperature=self.temperature
# #         )

# #         output = ""

# #         for completion_chunk in completion_chunks:
# #             try:
# #                 delta = completion_chunk["choices"][0]["delta"]
# #                 text = delta.get("content", "")

# #                 if not text:
# #                     continue

# # #                log_print(text, end="", flush=True)#20260612_kpopmodder
# #                 output += text
# #                 yield output

# #             except Exception as e:#20260612_kpopmodder
# #                 log_print(f"[HybridLLM] Local stream error: {e}")#20260612_kpopmodder
        
# #         log_print(f"[HybridLLM][Local] response: {output}")#20260612_kpopmodder

# #     def predict(self, message, history, system_prompt):
# #         try:
# #             if self.should_use_openai(message):
# #                 log_print("[HybridLLM] Route: OpenAI")#20260612_kpopmodder
# #                 for output in self.predict_openai(message, history, system_prompt):
# #                     yield output
# #             else:
# #                 log_print("[HybridLLM] Route: Local GGUF")#20260612_kpopmodder
# #                 for output in self.predict_local(message, history, system_prompt):
# #                     yield output

# #         except Exception as e:
# #             error_message = f"[HybridLLM 오류] {e}"
# #             log_print(error_message)#20260612_kpopmodder
# #             yield error_message