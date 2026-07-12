# import os


# class HybridRouter:#20260618_kpopmodder
#     DEFAULT_KEYWORDS = [
#         "코드", "프로그래밍", "파이썬", "python", "에러", "오류",
#         "수학", "계산", "증명", "논문", "전문", "법률", "의학",
#         "네트워크", "Cisco", "Azure", "OpenAI", "API", "암호학",
#         "양자", "보안", "GitHub", "Git", "설계", "분석"
#     ]

#     def __init__(self, base_dir, log_print):
#         self.base_dir = base_dir
#         self.log_print = log_print
#         self.difficult_keywords = self.load_difficult_keywords()

#     def load_difficult_keywords(self):
#         keyword_path = os.path.join(self.base_dir, "difficult_keywords.txt")

#         if not os.path.exists(keyword_path):
#             with open(keyword_path, "w", encoding="utf-8") as f:
#                 f.write("\n".join(self.DEFAULT_KEYWORDS))

#             self.log_print(f"[HybridLLM] Created difficult_keywords.txt: {keyword_path}")
#             return self.DEFAULT_KEYWORDS

#         keywords = []

#         with open(keyword_path, "r", encoding="utf-8") as f:
#             for line in f:
#                 keyword = line.strip()

#                 if not keyword:
#                     continue

#                 if keyword.startswith("#"):
#                     continue

#                 keywords.append(keyword)

#         if not keywords:
#             self.log_print("[HybridLLM] difficult_keywords.txt is empty. Using default keywords.")
#             return self.DEFAULT_KEYWORDS

#         self.log_print(f"[HybridLLM] Loaded keywords: {keywords}")
#         return keywords

#     def should_use_openai(self, message):
#         message_lower = message.lower()

#         for keyword in self.difficult_keywords:
#             if keyword.lower() in message_lower:
#                 self.log_print(f"[HybridLLM] GPT keyword detected: {keyword}")
#                 return True

#         if len(message) >= 120:
#             self.log_print("[HybridLLM] Long question detected. Using GPT.")
#             return True

#         return False