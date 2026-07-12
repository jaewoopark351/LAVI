# import os

# from llama_cpp import Llama, llama_cpp


# class LocalGGUFClient:#20260618_kpopmodder
#     def __init__(self, settings, base_dir, log_print):
#         self.settings = settings
#         self.base_dir = base_dir
#         self.log_print = log_print
#         self.llm = None

#     def load(self):
#         model_directory = os.path.join(self.base_dir, "models")
#         model_path = os.path.join(model_directory, self.settings.local_model_filename)

#         if not os.path.exists(model_path):
#             self.log_print(f"[HybridLLM] Local GGUF model not found: {model_path}")
#             return False

#         self.log_print(f"[HybridLLM] Loading local model: {model_path}")

#         try:
#             system_info = llama_cpp.llama_print_system_info().decode(errors="ignore")
#             self.log_print("[HybridLLM] llama.cpp system info:")
#             self.log_print(system_info)
#         except Exception as e:
#             self.log_print(f"[HybridLLM] Failed to print llama.cpp system info: {e}")

#         self.log_print(
#             "[HybridLLM] GGUF settings: "
#             f"n_gpu_layers={self.settings.n_gpu_layers}, "
#             f"main_gpu={self.settings.main_gpu}, "
#             f"n_batch={self.settings.n_batch}, "
#             f"n_ctx={self.settings.context_length}"
#         )

#         self.llm = Llama(
#             model_path=model_path,
#             n_ctx=self.settings.context_length,
#             n_gpu_layers=self.settings.n_gpu_layers,
#             main_gpu=self.settings.main_gpu,
#             n_batch=self.settings.n_batch,
#             seed=-1,
#             verbose=self.settings.verbose
#         )

#         return True

#     def is_loaded(self):
#         return self.llm is not None

#     def count_tokens(self, messages):
#         if self.llm is None:
#             return 0

#         total = 0

#         for msg in messages:
#             content = str(msg.get("content", ""))
#             total += len(self.llm.tokenize(content.encode("utf-8")))

#         self.log_print(f"[HybridLLM] Tokens in context: {total}")
#         return total

#     def trim_messages_to_context(self, messages):
#         while (
#             self.count_tokens(messages) > self.settings.context_length
#             and len(messages) > 1
#         ):
#             messages.pop(1)

#         return messages

#     def stream(self, messages):
#         if self.llm is None:
#             yield "[HybridLLM 오류] 로컬 GGUF 모델을 찾을 수 없습니다."
#             return

#         messages = self.trim_messages_to_context(messages)

#         completion_chunks = self.llm.create_chat_completion(
#             messages=messages,
#             stream=True,
#             temperature=self.settings.temperature
#         )

#         output = ""

#         for completion_chunk in completion_chunks:
#             try:
#                 delta = completion_chunk["choices"][0]["delta"]
#                 text = delta.get("content", "")

#                 if not text:
#                     continue

#                 output += text
#                 yield output

#             except Exception as e:
#                 self.log_print(f"[HybridLLM] Local stream error: {e}")

#         self.log_print(f"[HybridLLM][Local] response: {output}")