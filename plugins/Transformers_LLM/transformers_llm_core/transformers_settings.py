# from dataclasses import dataclass#20260626_kpopmodder : 모듈 사용 중지로 인한 주석처리

# from core.config_manager import config_manager


# def _to_float(value, default):
#     try:
#         if value is None:
#             return default
#         return float(value)
#     except Exception:
#         return default


# def _to_int(value, default):
#     try:
#         if value is None:
#             return default
#         return int(value)
#     except Exception:
#         return default


# def _to_bool(value, default=False):
#     try:
#         if value is None:
#             return default
#         return str(value).strip().lower() in ("true", "1", "yes", "y", "on")
#     except Exception:
#         return default


# @dataclass
# class TransformersSettings:
#     section_name: str = "Transformers_LLM"

#     # 20260619_kpopmodder
#     # llama-cpp-python/GGUF를 쓰지 않고 Hugging Face Transformers 원본 모델을 직접 로딩한다.
#     model_id: str = "Bllossom/llama-3.2-Korean-Bllossom-3B"

#     max_history_pairs: int = 5
#     temperature: float = 0.7
#     top_p: float = 0.9
#     top_k: int = 50
#     max_new_tokens: int = 256
#     repetition_penalty: float = 1.1

#     # 20260619_kpopmodder
#     # fp16부터 시작한다. 4bit/bitsandbytes는 Windows + Python/CUDA 조합 확인 후 별도 실험.
#     torch_dtype: str = "float16"
#     device_map: str = "auto"  #20260626_kpopmodder: Fallback only when config/gpu_device_config.json has no explicit map.
#     trust_remote_code: bool = False
#     verbose: bool = True

#     @classmethod
#     def load(cls):
#         config = config_manager.load_section("Transformers_LLM")

#         return cls(
#             model_id=config.get(
#                 "model_id",
#                 "Bllossom/llama-3.2-Korean-Bllossom-3B"
#             ),
#             max_history_pairs=max(
#                 0,
#                 _to_int(config.get("max_history_pairs", "5"), 5)
#             ),
#             temperature=_to_float(config.get("temperature", "0.7"), 0.7),
#             top_p=_to_float(config.get("top_p", "0.9"), 0.9),
#             top_k=_to_int(config.get("top_k", "50"), 50),
#             max_new_tokens=max(
#                 16,
#                 _to_int(config.get("max_new_tokens", "256"), 256)
#             ),
#             repetition_penalty=_to_float(
#                 config.get("repetition_penalty", "1.1"),
#                 1.1
#             ),
#             torch_dtype=config.get("torch_dtype", "float16"),
#             device_map=config.get("device_map", "auto"),
#             trust_remote_code=_to_bool(
#                 config.get("trust_remote_code", "False"),
#                 False
#             ),
#             verbose=_to_bool(config.get("verbose", "True"), True),
#         )

#     def save(self, key, value):
#         setattr(self, key, value)
#         config_manager.save_config(
#             self.section_name,
#             key,
#             str(value)
#         )
