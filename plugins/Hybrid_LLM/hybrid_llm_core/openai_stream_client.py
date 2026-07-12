# import os

# from openai import OpenAI


# class OpenAIStreamClient:#20260618_kpopmodder
#     def __init__(self, settings, log_print):
#         self.settings = settings
#         self.log_print = log_print
#         self.client = None

#     def reset(self):
#         self.client = None

#     def get_client(self):
#         if self.client is None:
#             key = self.settings.openai_api_key or os.getenv("OPENAI_API_KEY", "")

#             if not key:
#                 raise RuntimeError(
#                     "OpenAI API Key가 없습니다. UI에서 키를 입력하거나 OPENAI_API_KEY 환경변수를 설정하세요."
#                 )

#             self.client = OpenAI(api_key=key)

#         return self.client

#     def stream(self, messages):
#         client = self.get_client()

#         stream = client.chat.completions.create(
#             model=self.settings.openai_model_name,
#             messages=messages,
#             temperature=self.settings.temperature,
#             top_p=1.0,
#             stream=True
#         )

#         output = ""

#         for chunk in stream:
#             try:
#                 text = chunk.choices[0].delta.content or ""

#                 if not text:
#                     continue

#                 output += text
#                 yield output

#             except Exception as e:
#                 self.log_print(f"[HybridLLM] OpenAI stream error: {e}")

#         self.log_print(f"[HybridLLM][OpenAI] response: {output}")