# import gc#20260626_kpopmodder : 모듈 사용 중지로 인한 주석처리
# import os
# import threading
# import traceback

# import torch
# from transformers import (
#     AutoModelForCausalLM,
#     AutoTokenizer,
#     StoppingCriteria,
#     StoppingCriteriaList,
#     TextIteratorStreamer,
# )

# from core.gpu_device_manager import gpu_device_manager#20260626_kpopmodder

# #20260620_kpopmodder
# DEFAULT_SYSTEM_PROMPT = """
# 당신은 한국어 AI VTuber 어시스턴트입니다.
# 항상 한국어로 자연스럽고 친근하게 대답하세요.

# 사용자의 질문이 짧거나 불완전해도 가능한 의도를 추론해서 바로 답변하세요.
# 불필요하게 되묻지 마세요.
# 정말 정보가 부족할 때만 짧게 확인 질문을 하세요.

# 사용자가 "설명해줘", "알려줘", "추천해줘", "줄거리", "뭐야"라고 말하면
# 질문으로 되묻지 말고 바로 답변하세요.

# 설명 요청을 받으면 최소 3문단 이상으로 답하세요.
# 답변 구조는 가능하면 개요 → 핵심 내용 → 특징 순서로 작성하세요.

# 당신은 Meta AI, OpenAI, Claude, Gemini라고 주장하지 마세요.
# 당신은 이 프로젝트의 로컬 AI VTuber 어시스턴트입니다.
# """.strip()

# class InterruptStoppingCriteria(StoppingCriteria):
#     #20260620_kpopmodder: Stop Transformers generation at the next token after an interrupt.
#     def __init__(self, stop_event):
#         self.stop_event = stop_event

#     def __call__(self, input_ids, scores, **kwargs):
#         return self.stop_event.is_set()


# class TransformersClient:#20260619_kpopmodder
#     def __init__(self, settings, log_print):
#         self.settings = settings
#         self.log_print = log_print
#         self.tokenizer = None
#         self.model = None
#         self.device = None
#         self._generation_lock = threading.Lock()
#         self._active_stop_events = set()

#         # 20260619_kpopmodder
#         # Hugging Face 기본 캐시가 아니라 플러그인 내부 model 폴더에 모델을 저장한다.
#         self.model_cache_dir = self._resolve_model_cache_dir()

#     def request_interrupt(self):
#         with self._generation_lock:
#             stop_events = list(self._active_stop_events)

#         for stop_event in stop_events:
#             stop_event.set()

#         if stop_events:
#             self.log_print("[Transformers_LLM] generation interrupt requested")

#     def _resolve_model_cache_dir(self):
#         core_dir = os.path.dirname(os.path.abspath(__file__))
#         plugin_dir = os.path.dirname(core_dir)
#         model_dir = os.path.join(plugin_dir, "model")

#         os.makedirs(model_dir, exist_ok=True)

#         return model_dir

#     def _resolve_dtype(self):
#         value = str(self.settings.torch_dtype).strip().lower()

#         if value in ("float16", "fp16", "half"):
#             return torch.float16

#         if value in ("bfloat16", "bf16"):
#             return torch.bfloat16

#         if value in ("float32", "fp32"):
#             return torch.float32

#         return "auto"

#     def _get_eos_token_ids(self):
#         eos_token_ids = []

#         if self.tokenizer is None:
#             return None

#         if self.tokenizer.eos_token_id is not None:
#             eos_token_ids.append(self.tokenizer.eos_token_id)

#         # 20260619_kpopmodder
#         # Llama 3 / Bllossom 계열은 assistant turn 종료에 <|eot_id|>를 쓰는 경우가 있다.
#         # 이걸 eos_token_id에 같이 넣지 않으면 assistant 답변을 여러 번 반복할 수 있다.
#         try:
#             eot_id = self.tokenizer.convert_tokens_to_ids("<|eot_id|>")
#             if isinstance(eot_id, int) and eot_id >= 0 and eot_id not in eos_token_ids:
#                 eos_token_ids.append(eot_id)
#         except Exception:
#             pass

#         if not eos_token_ids:
#             return None

#         return eos_token_ids

#     def unload(self):
#         self.tokenizer = None
#         self.model = None
#         self.device = None

#         gc.collect()

#         if torch.cuda.is_available():
#             torch.cuda.empty_cache()

#     def load(self):
#         if self.model is not None and self.tokenizer is not None:
#             return

#         model_id = self.settings.model_id.strip()

#         if not model_id:
#             raise RuntimeError("[Transformers_LLM] model_id is empty.")

#         self.log_print(f"[Transformers_LLM] Loading tokenizer: {model_id}")
#         self.log_print(f"[Transformers_LLM] model cache dir: {self.model_cache_dir}")

#         self.tokenizer = AutoTokenizer.from_pretrained(
#             model_id,
#             cache_dir=self.model_cache_dir,
#             trust_remote_code=self.settings.trust_remote_code,
#         )

#         if self.tokenizer.pad_token_id is None:
#             if self.tokenizer.eos_token is not None:
#                 self.tokenizer.pad_token = self.tokenizer.eos_token
#             else:
#                 self.tokenizer.add_special_tokens({"pad_token": "<|pad|>"})

#         dtype = self._resolve_dtype()
#         resolved_device_map = gpu_device_manager.get_device_map(
#             "Transformers_LLM",
#             default=self.settings.device_map,
#         )#20260626_kpopmodder: Prefer gpu_device_config over Transformers auto sharding.
#         resolved_max_memory = gpu_device_manager.get_max_memory(
#             "Transformers_LLM"
#         )#20260626_kpopmodder

#         self.log_print(
#             "[Transformers_LLM] Loading model: "
#             f"{model_id}, dtype={self.settings.torch_dtype}, "
#             f"device_map={resolved_device_map}"
#         )
#         self.log_print(
#             f"[Transformers_LLM] resolved device_map={resolved_device_map}"
#         )#20260626_kpopmodder
#         self.log_print(
#             f"[Transformers_LLM] resolved max_memory={resolved_max_memory}"
#         )#20260626_kpopmodder

#         # 20260619_kpopmodder
#         # transformers 5.x에서는 torch_dtype 대신 dtype 사용.
#         # cache_dir를 지정해서 모델을 plugins/Transformers_LLM/model 아래에 저장한다.
#         model_kwargs = {
#             "cache_dir": self.model_cache_dir,
#             "dtype": dtype,
#             "device_map": resolved_device_map,
#             "trust_remote_code": self.settings.trust_remote_code,
#             "low_cpu_mem_usage": True,
#         }#20260626_kpopmodder

#         if resolved_max_memory is not None:
#             model_kwargs["max_memory"] = resolved_max_memory#20260626_kpopmodder

#         self.model = AutoModelForCausalLM.from_pretrained(
#             model_id,
#             **model_kwargs,
#         )

#         # tokenizer에 pad_token을 새로 추가한 경우 embedding 크기 보정
#         try:
#             if len(self.tokenizer) > self.model.get_input_embeddings().num_embeddings:
#                 self.model.resize_token_embeddings(len(self.tokenizer))
#         except Exception:
#             pass

#         self.model.eval()

#         try:
#             self.device = next(self.model.parameters()).device
#         except Exception:
#             self.device = torch.device(
#                 "cuda" if torch.cuda.is_available() else "cpu"
#             )

#         self.log_print(f"[Transformers_LLM] Model loaded on {self.device}")

#     def build_messages(self, message, history, system_prompt):
#         messages = []

#         if system_prompt and str(system_prompt).strip():
#             messages.append({
#                 "role": "system",
#                 "content": str(system_prompt).strip()
#             })
#         else:
#             messages.append({
#                 "role": "system",
#                 "content": DEFAULT_SYSTEM_PROMPT
#             })

#         trimmed_history = history[-self.settings.max_history_pairs:] if history else []

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
#             "content": str(message)
#         })

#         return messages

#     def build_prompt(self, messages):
#         if hasattr(self.tokenizer, "apply_chat_template"):
#             try:
#                 return self.tokenizer.apply_chat_template(
#                     messages,
#                     tokenize=False,
#                     add_generation_prompt=True,
#                 )
#             except Exception as e:
#                 self.log_print(
#                     f"[Transformers_LLM] apply_chat_template failed: {e}"
#                 )

#         # 20260619_kpopmodder
#         # tokenizer chat template이 없을 때 fallback.
#         lines = []

#         for msg in messages:
#             role = msg.get("role", "user")
#             content = msg.get("content", "")

#             if role == "system":
#                 lines.append(f"[System]\n{content}")
#             elif role == "assistant":
#                 lines.append(f"[Assistant]\n{content}")
#             else:
#                 lines.append(f"[User]\n{content}")

#         lines.append("[Assistant]\n")

#         return "\n\n".join(lines)

#     def _run_generate(self, generation_kwargs, streamer):
#         try:
#             with torch.inference_mode():
#                 self.model.generate(**generation_kwargs)
#         except Exception as e:
#             self.log_print(f"[Transformers_LLM] generate failed: {e}")
#             self.log_print(traceback.format_exc())

#             # 20260619_kpopmodder
#             # generation thread에서 예외가 나면 streamer가 기다리다 멈출 수 있으므로 종료 시도.
#             try:
#                 streamer.end()
#             except Exception:
#                 pass

#     def stream(self, message, history, system_prompt):
#         self.load()

#         stop_event = threading.Event()

#         messages = self.build_messages(
#             message=message,
#             history=history,
#             system_prompt=system_prompt,
#         )

#         prompt = self.build_prompt(messages)

#         if self.settings.verbose:
#             self.log_print(f"[Transformers_LLM] message: {message}")
#             self.log_print(
#                 "[Transformers_LLM] generation settings: "
#                 f"temperature={self.settings.temperature}, "
#                 f"max_new_tokens={self.settings.max_new_tokens}, "
#                 f"top_p={self.settings.top_p}, "
#                 f"top_k={self.settings.top_k}, "
#                 f"repetition_penalty={self.settings.repetition_penalty}"
#             )

#         inputs = self.tokenizer(
#             prompt,
#             return_tensors="pt",
#         )

#         inputs = {
#             key: value.to(self.device)
#             for key, value in inputs.items()
#         }

#         streamer = TextIteratorStreamer(
#             self.tokenizer,
#             skip_prompt=True,
#             skip_special_tokens=True,
#             clean_up_tokenization_spaces=False,
#         )

#         eos_token_ids = self._get_eos_token_ids()

#         do_sample = self.settings.temperature > 0

#         generation_kwargs = dict(
#             **inputs,
#             streamer=streamer,
#             max_new_tokens=self.settings.max_new_tokens,
#             repetition_penalty=self.settings.repetition_penalty,
#             do_sample=do_sample,
#             pad_token_id=self.tokenizer.pad_token_id,
#             eos_token_id=eos_token_ids,
#             stopping_criteria=StoppingCriteriaList([
#                 InterruptStoppingCriteria(stop_event)
#             ]),
#         )

#         # 20260619_kpopmodder
#         # do_sample=False일 때 temperature/top_p/top_k를 넣으면 transformers가 경고를 낼 수 있다.
#         if do_sample:
#             generation_kwargs.update({
#                 "temperature": self.settings.temperature,
#                 "top_p": self.settings.top_p,
#                 "top_k": self.settings.top_k,
#             })

#         thread = threading.Thread(
#             target=self._run_generate,
#             args=(generation_kwargs, streamer),
#         )
#         thread.daemon = True

#         with self._generation_lock:
#             self._active_stop_events.add(stop_event)

#         thread.start()

#         output = ""

#         try:
#             for text in streamer:
#                 if stop_event.is_set():
#                     break

#                 if not text:
#                     continue

#                 output += text
#                 yield output

#             thread.join(timeout=1)

#             if stop_event.is_set():
#                 #20260620_kpopmodder: Clear partial output so interrupted text is not sent back to TTS.
#                 self.log_print("[Transformers_LLM] generation interrupted")
#                 yield ""
#                 return

#             self.log_print(f"[Transformers_LLM] response: {output}")

#         finally:
#             with self._generation_lock:
#                 self._active_stop_events.discard(stop_event)
