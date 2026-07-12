#20260626_kpopmodder: Route memory retrieval before touching memory stores.
import concurrent.futures
import json
import logging
import re
import time

MEMORY_LOGGER_NAME = "LAV.memory_core"#20260627_kpopmodder
from dataclasses import dataclass, field


DEFAULT_MEMORY_SCOPE = ["working", "derived", "long_term"]
VALID_INTENTS = {"none", "search", "save", "forget", "consolidate"}
OPENAI_PROVIDER_NAMES = {
    "openai",
    "chatgpt",
    "chatgpt_openai",
    "openai_router",
}


@dataclass
class MemoryRouteDecision:
    intent: str
    need_memory: bool
    reason: str
    queries: list[str] = field(default_factory=list)
    memory_scope: list[str] = field(default_factory=list)
    max_items: int = 0
    fallback_used: bool = False


class MemoryRouter:
    """Decide whether stored memory should be searched for the user input."""

    PREVIOUS_CONTEXT_TERMS = (
        "아까",
        "전에",
        "그때",
        "지난번",
        "이전",
        "예전",
        "방금",
        "다시 설명",
        "다시 알려",
        "기억나",
        "기억 나",
        "기억해?",
        "기억함",
        "전에",
        "예전에",
        "이전에",
        "지난번",
        "그때",
        "부른",
        "불렀",
        "노래한",
        "했던",
        "했었",
        "remember",
        "recall",
        "previous",
        "before",
        "earlier",
        "last time",
    )
    PROJECT_TERMS = (
        "lav",
        "lav_v0.2",
        "whisper",
        "faster-whisper",
        "gpt-sovits",
        "gptsovits",
        "screenvision",
        "screen vision",
        "cuda",
        "gpu",
        "audiodevicemanager",
        "audio device",
        "memorystore",
        "memoryretriever",
        "memoryrouter",
        "derived_memory",
        "raw_events",
        "long_term_memory",
        "vtube studio",
        "vtubestudio",
    )
    USER_SETTING_TERMS = (
        "우리 프로젝트",
        "내 설정",
        "내가 쓰는",
        "내 환경",
        "코덱스가",
        "codex가",
        "디버깅",
        "수정했던",
        "고쳤던",
        "설정 문제",
        "오류",
        "로그",
    )
    FORGET_TERMS = (
        "기억 지워",
        "잊어줘",
        "삭제해줘",
        "forget memory",
        "delete memory",
    )
    CONSOLIDATE_TERMS = (
        "raw_events 정리",
        "raw_events 요약",
        "derived_memory 재생성",
        "메모리 정리",
        "memory consolidate",
    )
    GENERAL_QUESTION_TERMS = (
        "파이썬",
        "python list",
        "리스트 정렬",
        "방법 알려줘",
        "what is",
        "how to",
        "개념",
    )
    RECALL_TYPO_TERMS = (
        "거억",
        "거억하",
    )
    ACTIVITY_RECALL_SUBJECT_TERMS = (
        "youtube",
        "유튜브",
        "영상",
        "동영상",
        "video",
    )
    ACTIVITY_RECALL_PAST_TERMS = (
        "봤던",
        "본 거",
        "본거",
        "본 영상",
        "본영상",
        "본 동영상",
        "본동영상",
        "시청했던",
        "시청한",
        "보고 있던",
        "보고있던",
        "재생했던",
        "재생한",
        "watched",
        "seen",
    )
    ACTIVITY_RECALL_MEMORY_TERMS = (
        "기억",
        "거억",
        "전부",
        "전체",
        "모두",
        "모든",
        "remember",
        "recall",
        "all",
        "everything",
        "entire",
        "full",
    )
    ACTIVITY_RECALL_REQUEST_TERMS = (
        "알려줘",
        "말해줘",
        "얘기",
        "이야기",
        "뭐였",
        "무엇",
        "목록",
        "리스트",
        "tell",
        "list",
    )
    def __init__(
        self,
        enabled=True,
        provider="rule",
        timeout_sec=5,
        max_items=5,
        fallback_to_keyword=True,
        ai_response_callback=None,
    ):
        self.enabled = bool(enabled)
        self.provider = str(provider or "auto").strip().lower()#20260627_kpopmodder: Normalize config values like " OpenAI ".
        self.timeout_sec = max(1, int(timeout_sec or 5))
        self.max_items = max(1, int(max_items or 5))
        self.fallback_to_keyword = bool(fallback_to_keyword)
        self.ai_response_callback = ai_response_callback
        self.current_llm_response_callback = None
        self._ai_provider_unavailable = False#20260627_kpopmodder
        self._ai_provider_unavailable_reason = ""#20260627_kpopmodder

    def set_ai_response_callback(self, callback):
        self.ai_response_callback = callback

    def set_current_llm_response_callback(self, callback):
        self.current_llm_response_callback = callback

    def route(self, user_input):
        started_at = time.perf_counter()
        user_input = self._clean_text(user_input)
        if not self.enabled or self.provider == "off":
            decision = self._none_decision("router_disabled")
            self._log_decision(user_input, decision, started_at)
            return decision

        if self.provider in {
            "ai",
            "auto",
            "openai",
            "chatgpt",
            "chatgpt_openai",
            "openai_router",
            "llm",
            "current_llm",
            "current_plugin",
        }:
            if self._should_skip_unavailable_ai_provider():
                decision = self._fallback_after_ai_router_failure(
                    user_input,
                    started_at,
                    reason_prefix="ai_router_unavailable_cached",
                )
                return decision

            try:
                raw_response = self._call_ai_router(
                    user_input,
                    callback=self._select_ai_callback(),
                )
            except Exception as exc:
                self._log_router_error(exc)
                self._mark_ai_provider_unavailable(exc)
                decision = self._fallback_after_ai_router_failure(
                    user_input,
                    started_at,
                    reason_prefix="ai_router_failed",
                )
                return decision

            try:
                decision = self.parse_ai_response(user_input, raw_response)
                decision = self._override_ai_skip_if_rule_requires_memory(
                    user_input,
                    decision,
                )
                self._log_decision(user_input, decision, started_at)
                return decision
            except Exception as exc:
                self._log_router_error(exc)
                decision = self._fallback_after_ai_router_failure(
                    user_input,
                    started_at,
                    reason_prefix="ai_router_failed",
                )
                return decision

        decision = self.rule_based_decision(user_input)
        self._log_decision(user_input, decision, started_at)
        return decision

    def parse_ai_response(self, user_input, raw_response):
        user_input = self._clean_text(user_input)
        text = self._strip_code_fence(raw_response)
        json_text = self._extract_json_object(text)
        if not json_text:
            raise ValueError("memory router returned no JSON object")

        data = json.loads(json_text)
        if not isinstance(data, dict):
            raise ValueError("memory router JSON must be an object")

        return self._decision_from_dict(user_input, data)

    def rule_based_decision(
        self,
        user_input,
        fallback_used=False,
        reason_prefix="rule",
    ):
        user_input = self._clean_text(user_input)
        normalized = user_input.lower()

        if self._has_any(normalized, self.FORGET_TERMS):
            return MemoryRouteDecision(
                intent="forget",
                need_memory=False,
                reason=f"{reason_prefix}_forget_command",
                queries=[],
                memory_scope=[],
                max_items=0,
                fallback_used=fallback_used,
            )

        if self._has_any(normalized, self.CONSOLIDATE_TERMS):
            return MemoryRouteDecision(
                intent="consolidate",
                need_memory=False,
                reason=f"{reason_prefix}_consolidate_command",
                queries=[],
                memory_scope=[],
                max_items=0,
                fallback_used=fallback_used,
            )

        score = 0
        reasons = []
        if self._has_any(normalized, self.PREVIOUS_CONTEXT_TERMS):
            score += 3
            reasons.append("previous_context")
        if self._has_any(normalized, self.RECALL_TYPO_TERMS):
            score += 3
            reasons.append("recall_typo")
        if self._is_activity_recall_request(normalized):
            #20260627_kpopmodder: Watched/seen YouTube wording is a memory request even with typos like "거억".
            score += 3
            reasons.append("activity_recall")
        if self._has_any(normalized, self.PROJECT_TERMS):
            score += 2
            reasons.append("project_context")
        if self._has_any(normalized, self.USER_SETTING_TERMS):
            score += 2
            reasons.append("user_setting_or_debug_context")
        if self._has_any(normalized, self.GENERAL_QUESTION_TERMS):
            score -= 1
            reasons.append("general_question")

        need_memory = score >= 2
        if not need_memory:
            return MemoryRouteDecision(
                intent="none",
                need_memory=False,
                reason=f"{reason_prefix}_" + ("_".join(reasons) or "no_memory_signal"),
                queries=[],
                memory_scope=[],
                max_items=0,
                fallback_used=fallback_used,
            )

        return MemoryRouteDecision(
            intent="search",
            need_memory=True,
            reason=f"{reason_prefix}_" + "_".join(reasons),
            queries=[user_input],
            memory_scope=list(DEFAULT_MEMORY_SCOPE),
            max_items=self.max_items,
            fallback_used=fallback_used,
        )

    def build_ai_system_prompt(self):
        return (
            "You are a memory retrieval router for a local AI VTuber app.\n"
            "Return JSON only. Do not answer the user.\n"
            "Decide whether stored memory should be searched before the main LLM replies.\n"
            "Use this JSON shape exactly:\n"
            "{"
            '"intent":"none|search|save|forget|consolidate",'
            '"need_memory":true,'
            '"reason":"short_reason",'
            '"queries":["search query"],'
            '"memory_scope":["working","derived","long_term"],'
            '"max_items":5'
            "}\n"
            "Use need_memory=true only when prior conversation, user settings, "
            "project-specific LAV context, debugging history, or stored memory is needed.\n"
            "Treat Korean typo '거억' as '기억', and treat watched/seen YouTube or video "
            "requests as prior activity memory searches.\n"
            "For general questions, return intent=none and need_memory=false.\n"
        )

    def _select_ai_callback(self):
        if self.provider in {"llm", "current_llm", "current_plugin"}:
            return self.current_llm_response_callback or self.ai_response_callback

        if self.provider in OPENAI_PROVIDER_NAMES:
            return self.ai_response_callback

        return self.ai_response_callback or self.current_llm_response_callback

    def _call_ai_router(self, user_input, callback=None):
        callback = callback or self.ai_response_callback
        if callback is None:
            raise RuntimeError("memory router AI callback is not configured")

        system_prompt = self.build_ai_system_prompt()
        executor = concurrent.futures.ThreadPoolExecutor(max_workers=1)
        future = executor.submit(
            callback,
            system_prompt,
            user_input,
            self.timeout_sec,
        )
        try:
            return future.result(timeout=self.timeout_sec)
        finally:
            executor.shutdown(wait=False, cancel_futures=True)

    def _decision_from_dict(self, user_input, data):
        intent = str(data.get("intent") or "").strip().lower()
        need_memory = bool(data.get("need_memory", False))

        if intent not in VALID_INTENTS:
            intent = "search" if need_memory else "none"
        if intent != "search":
            need_memory = False

        queries = data.get("queries", [])
        if not isinstance(queries, list):
            queries = []
        queries = [self._clean_text(item) for item in queries if self._clean_text(item)]

        memory_scope = data.get("memory_scope", [])
        if not isinstance(memory_scope, list):
            memory_scope = []
        memory_scope = [
            self._clean_text(item)
            for item in memory_scope
            if self._clean_text(item)
        ]

        if need_memory:
            if not queries:
                queries = [user_input]
            if not memory_scope:
                memory_scope = list(DEFAULT_MEMORY_SCOPE)

        max_items = self._int_or_default(data.get("max_items"), self.max_items)
        if need_memory:
            max_items = min(self.max_items, max(1, max_items))
        else:
            max_items = 0

        reason = self._clean_text(data.get("reason")) or "router_json"

        return MemoryRouteDecision(
            intent=intent,
            need_memory=need_memory,
            reason=reason,
            queries=queries,
            memory_scope=memory_scope,
            max_items=max_items,
            fallback_used=bool(data.get("fallback_used", False)),
        )

    def _none_decision(self, reason, fallback_used=False):
        return MemoryRouteDecision(
            intent="none",
            need_memory=False,
            reason=reason,
            queries=[],
            memory_scope=[],
            max_items=0,
            fallback_used=fallback_used,
        )

    def _override_ai_skip_if_rule_requires_memory(self, user_input, decision):
        if decision.need_memory or not self.fallback_to_keyword:
            return decision

        rule_decision = self.rule_based_decision(
            user_input,
            fallback_used=True,
            reason_prefix="ai_router_no_memory_override",
        )
        if rule_decision.need_memory:
            return rule_decision
        return decision

    def _fallback_after_ai_router_failure(
        self,
        user_input,
        started_at,
        reason_prefix,
    ):
        if not self.fallback_to_keyword:
            decision = self._none_decision(
                reason_prefix,
                fallback_used=True,
            )
            self._log_decision(user_input, decision, started_at)
            return decision

        decision = self.rule_based_decision(
            user_input,
            fallback_used=True,
            reason_prefix=reason_prefix,
        )
        self._log_decision(user_input, decision, started_at)
        return decision

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
        if self.provider in OPENAI_PROVIDER_NAMES:
            return True

        if self.provider not in {"auto", "ai"}:
            return False

        callback = self.ai_response_callback
        callback_name = callback.__class__.__name__ if callback is not None else ""
        return callback_name == "OpenAIMemoryRouterProvider"

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
        if start < 0:
            return ""

        depth = 0
        in_string = False
        escape = False
        for index in range(start, len(text)):
            char = text[index]
            if escape:
                escape = False
                continue
            if char == "\\":
                escape = True
                continue
            if char == '"':
                in_string = not in_string
                continue
            if in_string:
                continue
            if char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0:
                    return text[start:index + 1]
        return ""

    def _has_any(self, normalized_text, terms):
        return any(str(term).lower() in normalized_text for term in terms)

    def _has_any_compact(self, normalized_text, terms):
        compact = re.sub(r"\s+", "", normalized_text)
        return any(
            str(term).lower() in normalized_text
            or re.sub(r"\s+", "", str(term).lower()) in compact
            for term in terms
        )

    def _is_activity_recall_request(self, normalized_text):
        has_subject = self._has_any_compact(
            normalized_text,
            self.ACTIVITY_RECALL_SUBJECT_TERMS,
        )
        has_past_activity = self._has_any_compact(
            normalized_text,
            self.ACTIVITY_RECALL_PAST_TERMS,
        )
        has_memory_word = self._has_any_compact(
            normalized_text,
            self.ACTIVITY_RECALL_MEMORY_TERMS,
        )
        has_request_word = self._has_any_compact(
            normalized_text,
            self.ACTIVITY_RECALL_REQUEST_TERMS,
        )

        return (
            (has_subject and has_past_activity)
            or (has_subject and has_memory_word)
            or (has_past_activity and has_request_word)
        )

    def _int_or_default(self, value, default):
        try:
            return int(value)
        except Exception:
            return default

    def _log_decision(self, user_input, decision, started_at):
        logger = logging.getLogger(MEMORY_LOGGER_NAME)
        try:
            elapsed_ms = int((time.perf_counter() - started_at) * 1000)
            queries = [self._log_snippet(item, 80) for item in decision.queries]
            scopes = [self._log_snippet(item, 40) for item in decision.memory_scope]
            logger.info(
                "[MemoryRouterDecision] "
                f"provider={self.provider} "
                f"intent={decision.intent} "
                f"need_memory={decision.need_memory} "
                f"reason='{self._log_snippet(decision.reason, 80)}' "
                f"query_count={len(decision.queries)} "
                f"memory_scope={scopes} "
                f"max_items={decision.max_items} "
                f"fallback_used={decision.fallback_used} "
                f"elapsed_ms={elapsed_ms}"
            )
            logger.debug(
                "[MemoryRouterDecisionDebug] "
                f"provider={self.provider} "
                f"queries={queries} "
                f"user_input='{self._log_snippet(user_input, 160)}'"
            )
        except Exception:
            return

    def _log_router_error(self, error):
        logger = logging.getLogger(MEMORY_LOGGER_NAME)
        try:
            logger.info(
                "[MemoryRouterError] "
                f"provider={self.provider} "
                f"error_type={type(error).__name__} "
                f"fallback_to_keyword={self.fallback_to_keyword}"
            )
        except Exception:
            return

    def _log_provider_unavailable(self, error):
        logger = logging.getLogger(MEMORY_LOGGER_NAME)
        try:
            logger.info(
                "[MemoryRouterProviderUnavailable] "
                f"provider={self.provider} "
                f"error_type={type(error).__name__} "
                "cached_for_session=True"
            )
        except Exception:
            return

    def _log_snippet(self, text, limit):
        text = self._clean_text(text)
        if len(text) <= limit:
            return text
        return text[: max(0, limit - 3)] + "..."

    def _clean_text(self, text):
        return " ".join(str(text or "").strip().split())

    def _clean_text_multiline(self, text):
        return str(text or "").strip()
