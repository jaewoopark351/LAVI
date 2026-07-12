# class PromptBuilder:#20260618_kpopmodder
#     DEFAULT_SYSTEM_PROMPT = (
#         "너는 한국어로 자연스럽게 말하는 AI 버튜버다. "
#         "평소에는 짧고 친근하게 대답한다. "
#         "전문적인 질문에는 정확하고 이해하기 쉽게 설명한다."
#     )

#     def build_messages(self, message, history, system_prompt, max_history_pairs):
#         messages = []

#         if system_prompt and system_prompt.strip():
#             messages.append({
#                 "role": "system",
#                 "content": system_prompt.strip()
#             })
#         else:
#             messages.append({
#                 "role": "system",
#                 "content": self.DEFAULT_SYSTEM_PROMPT
#             })

#         trimmed_history = history[-max_history_pairs:] if history else []

#         for entry in trimmed_history:
#             try:
#                 user, ai = entry
#             except Exception:
#                 continue

#             if user:
#                 messages.append({
#                     "role": "user",
#                     "content": str(user)
#                 })

#             if ai:
#                 messages.append({
#                     "role": "assistant",
#                     "content": str(ai)
#                 })

#         messages.append({
#             "role": "user",
#             "content": message
#         })

#         return messages