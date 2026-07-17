#20260717_kpopmodder: Isolates command phrase overrides from OpenAI route probing.
import re

from .route_decision import RouteDecision


class CommandOverrideRouter:
    COMMAND_TERMS = (
        "openai에게 물어봐",
        "open ai에게 물어봐",
        "openai한테 물어봐",
        "open ai한테 물어봐",
        "gpt에게 물어봐",
        "gpt한테 물어봐",
        "챗gpt에게 물어봐",
        "챗gpt한테 물어봐",
        "챗GPT에게 물어봐",
        "챗GPT한테 물어봐",
        "chatgpt에게 물어봐",
        "chatgpt한테 물어봐",
        "외부 ai에게 물어봐",
        "외부 ai한테 물어봐",
        "고급 모델로 답해줘",
        "고급모델로 답해줘",
    )

    def route(self, message):
        normalized = self._normalize(message)
        if not normalized:
            return None

        for term in self.COMMAND_TERMS:
            if self._normalize(term) in normalized:
                return RouteDecision(
                    route="openai_chat",
                    reason="command_override",
                    forced=True,
                )
        return None

    def _normalize(self, text):
        return re.sub(r"\s+", "", str(text or "").strip().lower())
