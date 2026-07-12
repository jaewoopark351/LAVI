# from dataclasses import dataclass

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


# def _to_bool(value, default=False):#20260618_kpopmodder
#     try:
#         if value is None:
#             return default

#         return str(value).strip().lower() in ("true", "1", "yes", "y", "on")
#     except Exception:
#         return default


# @dataclass
# class HybridSettings:#20260618_kpopmodder#GPU 할당량 설정하는 클래스#GPU 증설 이후 소스코드 수정 필요
#     section_name: str = "Hybrid_LLM"

#     # Safe default for 16GB VRAM + GPT-SoVITS shared GPU.
#     context_length: int = 8192#20260618_kpopmodder
#     #context_length: int = 4096#20260618_kpopmodder
#     temperature: float = 0.8
#     openai_api_key: str = ""
#     openai_model_name: str = "gpt-4o-mini"
#     local_model_filename: str = "ggml-model-Q5_K_M.gguf"
#     max_history_pairs: int = 10

#     # GGUF GPU settings
#     # -1 loads all possible layers to GPU, but it can cause VRAM OOM.
#     # Use 20 first, then raise gradually after stability test.
#     n_gpu_layers: int = -1#20260618_kpopmodder
#     #n_gpu_layers: int = 20#20260618_kpopmodder
#     main_gpu: int = 0
#     n_batch: int = 512#20260618_kpopmodder
#     #n_batch: int = 128#20260618_kpopmodder
#     verbose: bool = False

#     @classmethod
#     def load(cls):
#         config = config_manager.load_section("Hybrid_LLM")

#         context_length = _to_int(
#             config.get("context_length", "4096"),
#             4096
#         )

#         max_history_pairs = _to_int(
#             config.get("max_history_pairs", "10"),
#             10
#         )

#         n_gpu_layers = _to_int(
#             config.get("n_gpu_layers", "20"),
#             20
#         )

#         main_gpu = _to_int(
#             config.get("main_gpu", "0"),
#             0
#         )

#         n_batch = _to_int(
#             config.get("n_batch", "128"),
#             128
#         )

#         # Safety guard
#         context_length = max(1024, context_length)
#         max_history_pairs = max(0, max_history_pairs)
#         n_batch = max(1, n_batch)

#         return cls(
#             context_length=context_length,
#             temperature=_to_float(config.get("temperature", "0.8"), 0.8),
#             openai_api_key=config.get("openai_api_key", ""),
#             openai_model_name=config.get("openai_model_name", "gpt-4o-mini"),
#             local_model_filename=config.get(
#                 "local_model_filename",
#                 "ggml-model-Q5_K_M.gguf"
#             ),
#             max_history_pairs=max_history_pairs,
#             n_gpu_layers=n_gpu_layers,
#             main_gpu=main_gpu,
#             n_batch=n_batch,
#             verbose=_to_bool(config.get("verbose", "False"), False),
#         )

#     def save(self, key, value):
#         setattr(self, key, value)

#         config_manager.save_config(
#             self.section_name,
#             key,
#             str(value)
#         )