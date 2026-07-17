#20260717_kpopmodder: Isolates OpenAI chat streaming provider behavior.
import os

try:
    from openai import OpenAI
except Exception:  # pragma: no cover - optional at import time for tests.
    OpenAI = None


class OpenAIChatProvider:
    def __init__(self, settings, log_print, client_factory=None):
        self.settings = settings
        self.log_print = log_print
        self.client_factory = client_factory
        self.client = None

    def stream(self, message, history, system_prompt):
        client = self._get_client()
        messages = self._build_messages(message, history, system_prompt)

        self.log_print("[Hybrid_OpenAI_LLM] Route: openai_chat")
        self.log_print(f"[Hybrid_OpenAI_LLM] model: {self.settings.openai_model_name}")

        stream = client.chat.completions.create(
            model=self.settings.openai_model_name,
            messages=messages,
            temperature=self.settings.temperature,
            top_p=1.0,
            stream=True,
        )

        output = ""
        for chunk in stream:
            try:
                delta = chunk.choices[0].delta.content or ""
            except Exception as e:
                self.log_print(f"[Hybrid_OpenAI_LLM] OpenAI stream chunk error: {e}")
                continue

            if not delta:
                continue

            output += delta
            yield output

        self.log_print(f"[Hybrid_OpenAI_LLM] response: {output}")

    def _get_client(self):
        if self.client is not None:
            return self.client

        if self.client_factory is not None:
            self.client = self.client_factory()
            return self.client

        if OpenAI is None:
            raise RuntimeError("openai package is not available")

        key = os.getenv("OPENAI_API_KEY", "") or self.settings.openai_api_key
        if not key:
            raise RuntimeError("OpenAI API key is not configured")

        self.client = OpenAI(api_key=key)
        return self.client

    def _build_messages(self, message, history, system_prompt):
        messages = []
        if system_prompt and str(system_prompt).strip():
            messages.append({
                "role": "system",
                "content": str(system_prompt).strip(),
            })
        else:
            messages.append({
                "role": "system",
                "content": (
                    "You are a Korean AI VTuber assistant. "
                    "Answer naturally and keep casual replies concise."
                ),
            })

        trimmed_history = history[-self.settings.max_history_pairs:] if history else []
        for entry in trimmed_history:
            try:
                user, assistant = entry
            except Exception:
                continue
            if user:
                messages.append({"role": "user", "content": str(user)})
            if assistant:
                messages.append({"role": "assistant", "content": str(assistant)})

        messages.append({"role": "user", "content": str(message or "")})
        return messages
