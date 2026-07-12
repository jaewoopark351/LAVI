#20260626_kpopmodder: Dedicated OpenAI provider for MemoryRouter; avoids reusing heavy local LLMs.
import os

from core.config_manager import config_manager

try:
    from openai import OpenAI
except Exception:  # pragma: no cover - runtime dependency is optional for tests.
    OpenAI = None


class OpenAIMemoryRouterProvider:
    def __init__(
        self,
        api_key="",
        model_name="gpt-4o-mini",
        temperature=0.0,
    ):
        self.api_key = str(api_key or "").strip()
        self.model_name = str(model_name or "gpt-4o-mini").strip()
        self.temperature = self._float_or_default(temperature, 0.0)
        self.client = None

    @classmethod
    def from_config(cls, memory_router_config=None):
        memory_router_config = memory_router_config or {}
        chatgpt_config = config_manager.load_section("ChatGPT_OpenAI")

        api_key = (
            memory_router_config.get("openai_api_key")
            or memory_router_config.get("memory_router_openai_api_key")
            or chatgpt_config.get("api_key", "")
        )
        model_name = (
            memory_router_config.get("openai_model")
            or memory_router_config.get("memory_router_openai_model")
            or chatgpt_config.get("model_name", "gpt-4o-mini")
        )
        temperature = (
            memory_router_config.get("openai_temperature")
            or memory_router_config.get("memory_router_openai_temperature")
            or 0.0
        )

        return cls(
            api_key=api_key,
            model_name=model_name,
            temperature=temperature,
        )

    def __call__(self, system_prompt, user_input, timeout_sec=None):
        client = self._get_client(timeout_sec=timeout_sec)
        messages = [
            {
                "role": "system",
                "content": str(system_prompt or "").strip(),
            },
            {
                "role": "user",
                "content": str(user_input or "").strip(),
            },
        ]

        kwargs = {
            "model": self.model_name,
            "messages": messages,
            "temperature": self.temperature,
            "top_p": 1.0,
        }

        try:
            response = client.chat.completions.create(
                **kwargs,
                response_format={"type": "json_object"},
            )
        except TypeError:
            response = client.chat.completions.create(**kwargs)

        return self._response_text(response)

    def _get_client(self, timeout_sec=None):
        if OpenAI is None:
            raise RuntimeError("openai package is not available")

        if self.client is None:
            key = self.api_key or os.getenv("OPENAI_API_KEY", "")
            if not key:
                raise RuntimeError("OpenAI API key is not configured")

            kwargs = {"api_key": key}
            try:
                timeout_value = float(timeout_sec or 0)
                if timeout_value > 0:
                    kwargs["timeout"] = timeout_value
            except Exception:
                pass

            self.client = OpenAI(**kwargs)

        return self.client

    def _response_text(self, response):
        try:
            return response.choices[0].message.content or ""
        except Exception:
            return str(response or "")

    def _float_or_default(self, value, default):
        try:
            return float(value)
        except Exception:
            return default
