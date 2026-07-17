#20260628_kpopmodder: Route screen questions separately from memory retrieval.
import json
import logging
import re
import time

from llm_core.screen_question_router_core.screen_question_decision import (
    ScreenQuestionDecision,
)


SCREEN_ROUTER_LOGGER_NAME = "LAV.llm_core"
SCREEN_ROUTER_PROVIDER_NAMES = {
    "openai",
    "chatgpt",
    "chatgpt_openai",
    "openai_router",
}


class ScreenQuestionRouter:#20260628_kpopmodder: AI/rule intent gate for latest screen context.
    """Decide whether the current user input needs latest ScreenVision context."""

    SCREEN_QUESTION_PATTERNS = (
        "화면",
        "스크린",
        "보이는",
        "보였",
        "봤어",
        "봤니",
        "뭐 보여",
        "뭐 보였",
        "뭐 있었",
        "방금 뭐",
        "아까 뭐",
        "지금 뭐",
        "창",
        "오류",
        "에러",
        "자막",
        "버튼",
        "메뉴",
        "게임 화면",
        "screen",
        "window",
        "error",
    )

    SYSTEM_PROMPT = """You are a router for a VTuber app.
Return JSON only.
Decide whether the user is asking about the current/recent PC screen, window,
visible UI, visible error, visible text, game screen, or ScreenVision output.

You must not answer the user.
You must not ask follow-up questions.
Use need_screen=true only when latest screen observation is needed to answer.

Return this JSON shape:
{
  "intent": "screen_question" or "none",
  "need_screen": true or false,
  "reason": "short_snake_case_reason",
  "confidence": 0.0
}
"""

    def __init__(
        self,
        enabled=True,
        provider="rule",
        timeout_sec=2,
        fallback_to_keyword=True,
        ai_response_callback=None,
    ):
        self.enabled = bool(enabled)
        self.provider = str(provider or "rule").strip().lower()
        self.timeout_sec = max(1, int(timeout_sec or 2))
        self.fallback_to_keyword = bool(fallback_to_keyword)
        self.ai_response_callback = ai_response_callback
        self._ai_provider_unavailable = False
        self._ai_provider_unavailable_reason = ""

    def set_ai_response_callback(self, callback):
        self.ai_response_callback = callback

    def route(self, user_input, has_latest_screen_observation=False):#20260628_kpopmodder
        started_at = time.perf_counter()
        user_input = self._clean_text(user_input)
        has_latest_screen_observation = bool(has_latest_screen_observation)

        if not self.enabled or self.provider == "off":
            decision = self._none_decision("router_disabled")
            self._log_decision(user_input, decision, started_at)
            return decision

        if not has_latest_screen_observation:#20260628_kpopmodder: Avoid external routing when there is no screen context to inject.
            decision = self.rule_based_decision(
                user_input,
                has_latest_screen_observation=False,
            )
            self._log_decision(user_input, decision, started_at)
            return decision

        if self.provider in {
            "ai",
            "auto",
            "openai",
            "chatgpt",
            "chatgpt_openai",
            "openai_router",
        }:
            if self._should_skip_unavailable_ai_provider():
                return self._fallback_after_ai_router_failure(
                    user_input,
                    has_latest_screen_observation,
                    started_at,
                    reason_prefix="ai_router_unavailable_cached",
                )

            try:
                raw_response = self._call_ai_router(
                    user_input,
                    has_latest_screen_observation,
                )
                decision = self.parse_ai_response(
                    user_input,
                    raw_response,
                    has_latest_screen_observation=has_latest_screen_observation,
                )
                self._log_decision(user_input, decision, started_at)
                return decision
            except Exception as exc:
                self._log_router_error(exc)
                self._mark_ai_provider_unavailable(exc)
                return self._fallback_after_ai_router_failure(
                    user_input,
                    has_latest_screen_observation,
                    started_at,
                    reason_prefix="ai_router_failed",
                )

        decision = self.rule_based_decision(
            user_input,
            has_latest_screen_observation=has_latest_screen_observation,
        )
        self._log_decision(user_input, decision, started_at)
        return decision

    def parse_ai_response(
        self,
        user_input,
        raw_response,
        has_latest_screen_observation=False,
    ):#20260628_kpopmodder: Keep OpenAI output JSON-only and non-answering.
        text = self._strip_code_fence(raw_response)
        json_text = self._extract_json_object(text)
        if not json_text:
            raise ValueError("screen question router returned no JSON object")

        data = json.loads(json_text)
        if not isinstance(data, dict):
            raise ValueError("screen question router JSON must be an object")

        return self._decision_from_dict(
            user_input,
            data,
            has_latest_screen_observation=has_latest_screen_observation,
        )

    def rule_based_decision(
        self,
        user_input,
        has_latest_screen_observation=False,
        fallback_used=False,
        reason_prefix="rule",
    ):#20260628_kpopmodder: Local fallback preserves old keyword behavior.
        user_input = self._clean_text(user_input)
        if not user_input:
            return self._none_decision(
                f"{reason_prefix}_empty_input",
                fallback_used=fallback_used,
            )

        if not has_latest_screen_observation:
            return self._none_decision(
                f"{reason_prefix}_no_screen_observation",
                fallback_used=fallback_used,
            )

        compact = re.sub(r"\s+", "", user_input.lower())
        if any(
            pattern.replace(" ", "").lower() in compact
            for pattern in self.SCREEN_QUESTION_PATTERNS
        ):
            return ScreenQuestionDecision(
                intent="screen_question",
                need_screen=True,
                reason=f"{reason_prefix}_screen_question",
                confidence=0.6,
                fallback_used=fallback_used,
            )

        return self._none_decision(
            f"{reason_prefix}_no_screen_question",
            fallback_used=fallback_used,
        )

    def _call_ai_router(self, user_input, has_latest_screen_observation):#20260628_kpopmodder: Sends user text/state, not screen observation text.
        callback = self.ai_response_callback
        if callback is None:
            raise RuntimeError("screen question router AI callback is not configured")

        payload = {
            "user_input": user_input,
            "has_latest_screen_observation": bool(has_latest_screen_observation),
        }
        return callback(
            self.SYSTEM_PROMPT,
            json.dumps(payload, ensure_ascii=False),
            timeout_sec=self.timeout_sec,
        )

    def _decision_from_dict(
        self,
        user_input,
        data,
        has_latest_screen_observation=False,
    ):
        intent = self._clean_text(data.get("intent") or "")
        need_screen = bool(data.get("need_screen", False))
        reason = self._clean_text(data.get("reason") or "")
        confidence = self._float_or_default(data.get("confidence"), 0.0)
        confidence = min(1.0, max(0.0, confidence))

        if intent not in {"none", "screen_question"}:
            intent = "screen_question" if need_screen else "none"

        if need_screen and not has_latest_screen_observation:
            return self._none_decision("ai_no_screen_observation")

        if need_screen:
            return ScreenQuestionDecision(
                intent="screen_question",
                need_screen=True,
                reason=reason or "ai_screen_question",
                confidence=confidence,
                fallback_used=False,
            )

        return ScreenQuestionDecision(
            intent="none",
            need_screen=False,
            reason=reason or "ai_no_screen_question",
            confidence=confidence,
            fallback_used=False,
        )

    def _fallback_after_ai_router_failure(
        self,
        user_input,
        has_latest_screen_observation,
        started_at,
        reason_prefix,
    ):#20260628_kpopmodder: Router failure must not break normal chat.
        if not self.fallback_to_keyword:
            decision = self._none_decision(
                reason_prefix,
                fallback_used=True,
            )
            self._log_decision(user_input, decision, started_at)
            return decision

        decision = self.rule_based_decision(
            user_input,
            has_latest_screen_observation=has_latest_screen_observation,
            fallback_used=True,
            reason_prefix=reason_prefix,
        )
        self._log_decision(user_input, decision, started_at)
        return decision

    def _none_decision(self, reason, fallback_used=False):
        return ScreenQuestionDecision(
            intent="none",
            need_screen=False,
            reason=reason,
            confidence=0.0,
            fallback_used=fallback_used,
        )

    def _should_skip_unavailable_ai_provider(self):
        return (
            self._ai_provider_unavailable
            and self._uses_openai_router_provider()
        )

    def _mark_ai_provider_unavailable(self, error):
        if not self._uses_openai_router_provider():
            return
        self._ai_provider_unavailable = True
        self._ai_provider_unavailable_reason = type(error).__name__
        self._log_provider_unavailable(error)

    def _uses_openai_router_provider(self):
        if self.provider in SCREEN_ROUTER_PROVIDER_NAMES:
            return True
        if self.provider not in {"auto", "ai"}:
            return False

        callback = self.ai_response_callback
        callback_name = callback.__class__.__name__ if callback is not None else ""
        return callback_name == "OpenAIScreenQuestionRouterProvider"

    def _strip_code_fence(self, text):
        text = self._clean_text_multiline(text)
        if text.startswith("```json"):
            text = text[7:]
        elif text.startswith("```"):
            text = text[3:]
        if text.endswith("```"):
            text = text[:-3]
        return text.strip()

    def _extract_json_object(self, text):
        text = str(text or "").strip()
        if not text:
            return ""

        start = text.find("{")
        end = text.rfind("}")
        if start < 0 or end < start:
            return ""
        return text[start:end + 1]

    def _clean_text(self, value):
        return str(value or "").strip()

    def _clean_text_multiline(self, value):
        return str(value or "").strip()

    def _float_or_default(self, value, default):
        try:
            return float(value)
        except Exception:
            return default

    def _log_decision(self, user_input, decision, started_at):
        logger = logging.getLogger(SCREEN_ROUTER_LOGGER_NAME)
        try:
            elapsed_ms = int((time.perf_counter() - started_at) * 1000)
            logger.info(
                "[ScreenQuestionRouterDecision] "
                f"provider={self.provider} "
                f"intent={decision.intent} "
                f"need_screen={decision.need_screen} "
                f"reason='{self._log_snippet(decision.reason, 80)}' "
                f"confidence={decision.confidence:.2f} "
                f"fallback_used={decision.fallback_used} "
                f"elapsed_ms={elapsed_ms}"
            )
            logger.debug(
                "[ScreenQuestionRouterDecisionDebug] "
                f"user_input='{self._log_snippet(user_input, 160)}'"
            )
        except Exception:
            return

    def _log_router_error(self, error):
        try:
            logging.getLogger(SCREEN_ROUTER_LOGGER_NAME).warning(
                "[ScreenQuestionRouterError] "
                f"{type(error).__name__}: {self._log_snippet(error, 160)}"
            )
        except Exception:
            return

    def _log_provider_unavailable(self, error):
        try:
            logging.getLogger(SCREEN_ROUTER_LOGGER_NAME).warning(
                "[ScreenQuestionRouterProviderUnavailable] "
                f"{type(error).__name__}: {self._log_snippet(error, 160)}"
            )
        except Exception:
            return

    def _log_snippet(self, value, limit):
        text = str(value or "").replace("\n", " ").replace("\r", " ").strip()
        if len(text) <= limit:
            return text
        return text[: max(0, limit - 3)] + "..."
