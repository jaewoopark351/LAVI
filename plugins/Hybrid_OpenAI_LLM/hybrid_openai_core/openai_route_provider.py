#20260717_kpopmodder: Isolates OpenAI-backed route selection from command overrides.
import json
import os

from .route_decision import RouteDecision
from .routing_constants import VALID_ROUTES

try:
    from openai import OpenAI
except Exception:  # pragma: no cover - optional at import time for tests.
    OpenAI = None


class OpenAIRouteProvider:
    SIMPLE_CHAT_TERMS = (
        "안녕",
        "하이",
        "ㅎㅇ",
        "고마워",
        "좋아",
        "오케이",
        "그래",
        "응",
        "아니",
        "ㅋㅋ",
        "ㅎㅎ",
        "hello",
        "hi",
        "thanks",
        "ok",
    )
    OPENAI_HINT_TERMS = (
        "코드",
        "검수",
        "디버깅",
        "오류",
        "에러",
        "설계",
        "구현",
        "수정",
        "분석",
        "리팩터",
        "테스트",
        "커밋",
        "codex",
        "프롬프트",
        "지시문",
        "기억",
        "전에",
        "예전에",
        "기억나",
        "기억남",
        "remember",
        "debug",
        "review",
        "architecture",
        "design",
        "implement",
        "fix",
        "test",
    )

    def __init__(
        self,
        settings,
        log_print,
        client_factory=None,
    ):
        self.settings = settings
        self.log_print = log_print
        self.client_factory = client_factory
        self.client = None

    def route(self, message, history=None, system_prompt=""):
        try:
            raw_response = self._call_openai(message, history, system_prompt)
            return self.parse_response(raw_response)
        except Exception as e:
            self.log_print(f"[Hybrid_OpenAI_LLM] route provider fallback: {type(e).__name__}")
            return self.rule_fallback(message)

    def parse_response(self, raw_response):
        data = json.loads(self._extract_json_object(raw_response))
        route = str(data.get("route") or "").strip().lower()
        if route not in VALID_ROUTES:
            raise ValueError("invalid route")

        return RouteDecision(
            route=route,
            reason=str(data.get("reason") or "openai_route").strip(),
        )

    def rule_fallback(self, message):
        normalized = str(message or "").strip().lower()
        if any(term in normalized for term in self.OPENAI_HINT_TERMS):
            return RouteDecision(
                route="openai_chat",
                reason="route_fallback_openai_hint",
                fallback_used=True,
            )

        if len(normalized) <= 30 and any(
            term in normalized for term in self.SIMPLE_CHAT_TERMS
        ):
            return RouteDecision(
                route="local_light",
                reason="route_fallback_simple_chat",
                fallback_used=True,
            )

        return RouteDecision(
            route="openai_chat",
            reason="route_fallback_conservative",
            fallback_used=True,
        )

    def _call_openai(self, message, history=None, system_prompt=""):
        client = self._get_client()
        prompt = self._build_prompt()
        user_payload = {
            "message": str(message or ""),
            "recent_history": self._trim_history(history),
            "has_system_prompt": bool(str(system_prompt or "").strip()),
        }
        kwargs = {
            "model": self.settings.route_model_name,
            "messages": [
                {"role": "system", "content": prompt},
                {
                    "role": "user",
                    "content": json.dumps(user_payload, ensure_ascii=False),
                },
            ],
            "temperature": self.settings.route_temperature,
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

        kwargs = {"api_key": key}
        try:
            timeout = float(self.settings.route_timeout_sec)
            if timeout > 0:
                kwargs["timeout"] = timeout
        except Exception:
            pass

        self.client = OpenAI(**kwargs)
        return self.client

    def _build_prompt(self):
        return (
            "You are a router for a local AI VTuber app.\n"
            "Return JSON only. Do not answer the user.\n"
            'Allowed route values are "openai_chat" and "local_light".\n'
            "Use openai_chat for code review, debugging, project design, "
            "Codex instructions, memory-based answers, long reasoning, "
            "factual uncertainty, and any source-code or runtime stability work.\n"
            "Use local_light only for short casual chat, greetings, and brief reactions.\n"
            'Return exactly: {"route":"openai_chat|local_light","reason":"short_reason"}'
        )

    def _trim_history(self, history):
        if not history:
            return []
        trimmed = []
        for entry in list(history)[-2:]:
            try:
                user, assistant = entry
            except Exception:
                continue
            trimmed.append({
                "user": str(user or "")[:120],
                "assistant": str(assistant or "")[:120],
            })
        return trimmed

    def _response_text(self, response):
        try:
            return response.choices[0].message.content or ""
        except Exception:
            return str(response or "")

    def _extract_json_object(self, text):
        text = str(text or "").strip()
        start = text.find("{")
        end = text.rfind("}")
        if start < 0 or end < start:
            raise ValueError("route provider returned no JSON object")
        return text[start:end + 1]
