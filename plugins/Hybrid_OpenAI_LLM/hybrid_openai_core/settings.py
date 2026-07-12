from dataclasses import dataclass

from core.config_manager import config_manager


def _to_float(value, default):
    try:
        if value is None:
            return default
        return float(value)
    except Exception:
        return default


def _to_int(value, default):
    try:
        if value is None:
            return default
        return int(value)
    except Exception:
        return default


def _to_bool(value, default=False):
    try:
        if value is None:
            return default
        return str(value).strip().lower() in ("true", "1", "yes", "y", "on")
    except Exception:
        return default


@dataclass
class HybridOpenAISettings:
    section_name: str = "Hybrid_OpenAI_LLM"

    openai_model_name: str = "gpt-4o-mini"
    openai_api_key: str = ""
    temperature: float = 0.8
    max_history_pairs: int = 8

    route_model_name: str = "gpt-4o-mini"
    route_temperature: float = 0.0
    route_timeout_sec: int = 5

    memory_router_model_name: str = "gpt-4o-mini"
    memory_router_timeout_sec: int = 5
    memory_router_enabled: bool = True

    local_enabled: bool = False
    local_model_id: str = "Qwen/Qwen2.5-0.5B-Instruct"
    local_max_history_pairs: int = 2
    local_temperature: float = 0.7
    local_top_p: float = 0.8
    local_top_k: int = 20
    local_max_new_tokens: int = 96
    local_repetition_penalty: float = 1.1
    local_torch_dtype: str = "float16"
    local_device_map: str = "auto"
    local_trust_remote_code: bool = False
    local_verbose: bool = True

    @classmethod
    def load(cls):
        config = config_manager.load_section("Hybrid_OpenAI_LLM")
        chatgpt_config = config_manager.load_section("ChatGPT_OpenAI")

        return cls(
            openai_model_name=config.get(
                "openai_model_name",
                chatgpt_config.get("model_name", "gpt-4o-mini"),
            ),
            openai_api_key=config.get(
                "openai_api_key",
                chatgpt_config.get("api_key", ""),
            ),
            temperature=_to_float(config.get("temperature", "0.8"), 0.8),
            max_history_pairs=max(
                0,
                _to_int(config.get("max_history_pairs", "8"), 8),
            ),
            route_model_name=config.get(
                "route_model_name",
                config.get("openai_model_name", "gpt-4o-mini"),
            ),
            route_temperature=_to_float(
                config.get("route_temperature", "0.0"),
                0.0,
            ),
            route_timeout_sec=max(
                1,
                _to_int(config.get("route_timeout_sec", "5"), 5),
            ),
            memory_router_model_name=config.get(
                "memory_router_model_name",
                config.get("route_model_name", "gpt-4o-mini"),
            ),
            memory_router_timeout_sec=max(
                1,
                _to_int(config.get("memory_router_timeout_sec", "5"), 5),
            ),
            memory_router_enabled=_to_bool(
                config.get("memory_router_enabled", "True"),
                True,
            ),
            local_enabled=_to_bool(config.get("local_enabled", "False"), False),
            local_model_id=config.get(
                "local_model_id",
                "Qwen/Qwen2.5-0.5B-Instruct",
            ),
            local_max_history_pairs=max(
                0,
                _to_int(config.get("local_max_history_pairs", "2"), 2),
            ),
            local_temperature=_to_float(
                config.get("local_temperature", "0.7"),
                0.7,
            ),
            local_top_p=_to_float(config.get("local_top_p", "0.8"), 0.8),
            local_top_k=max(1, _to_int(config.get("local_top_k", "20"), 20)),
            local_max_new_tokens=max(
                16,
                _to_int(config.get("local_max_new_tokens", "96"), 96),
            ),
            local_repetition_penalty=_to_float(
                config.get("local_repetition_penalty", "1.1"),
                1.1,
            ),
            local_torch_dtype=config.get("local_torch_dtype", "float16"),
            local_device_map=config.get("local_device_map", "auto"),
            local_trust_remote_code=_to_bool(
                config.get("local_trust_remote_code", "False"),
                False,
            ),
            local_verbose=_to_bool(config.get("local_verbose", "True"), True),
        )

    def save(self, key, value):
        setattr(self, key, value)
        config_manager.save_config(
            self.section_name,
            key,
            str(value),
        )
