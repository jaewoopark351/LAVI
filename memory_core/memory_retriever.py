#20260622_kpopmodder: Dependency-free relevant memory retrieval for Korean and English conversations.
import logging
import math
import re
import time
from difflib import SequenceMatcher

from memory_core.derived_memory_rebuild_service import (
    DerivedMemoryRebuildService,
)#20260720_kpopmodder
from memory_core.memory_consolidator import MemoryConsolidator

MEMORY_LOGGER_NAME = "LAV.memory_core"#20260627_kpopmodder


class MemoryRetriever:#20260622_kpopmodder: 현재 질문과 관련된 과거 사건을 검색한다.
    """현재 질문과 관련된 과거 대화/화면 관찰을 찾는다.

    외부 임베딩 모델을 추가하지 않고 토큰, 한글/영문 문자 조각, 문장 유사도를
    함께 사용한다. 검색 실패는 빈 목록으로 처리해 기존 LLM 흐름을 보존한다.
    """

    STOP_WORDS = {
        "기억", "기억해", "기억나", "예전", "전에", "과거", "우리",
        "내가", "네가", "너가", "그때", "얘기", "이야기", "했지",
        "했어", "뭐였지", "뭐였어", "알려줘", "말해줘", "관련",
        "remember", "recall", "before", "previous", "previously", "past",
        "what", "when", "where", "which", "about", "tell", "said",
    }

    GENERIC_RECALL_PHRASES = (
        "기억나",
        "기억나니",
        "기억나요",
        "기억해",
        "거억나",
        "거억해",
        "떠올라",
        "옛날일",
        "옜날일",
        "예전일",
        "과거일",
        "예전기억",
        "옛날기억",
        "옜날기억",
        "과거기억",
        "rememberoldtimes",
        "rememberanythingfrombefore",
        "doyourememberthepast",
    )
    ASSISTANT_ECHO_RECALL_TERMS = (
        "\uae30\uc5b5",
        "\uc608\uc804",
        "\uc774\uc804",
        "\uc9c0\ub09c\ubc88",
        "\uadf8\ub54c",
        "\uac70\uc5b5",#20260629_kpopmodder: Treat typo recall prompts as answer echoes too.
        "remember",
        "recall",
        "previous",
        "before",
    )
    ASSISTANT_ECHO_REQUEST_TERMS = (
        "\uc54c\ub824\uc918",
        "\ubb50",
        "\ubb34\uc5c7",
        "\uc5b4\ub5a4",
        "tellme",
        "what",
        "which",
    )
    ASSISTANT_ECHO_SUBJECT_TERMS = (
        "\ubd80\ub978",
        "\ubd88\ub800",
        "\ub178\ub798",
        "\uc81c\ubaa9",
        "\ud588\ub358",
        "\ud588\uc5c8",
    )
    MEMORY_FAILURE_ANSWER_TERMS = (
        "\uae30\uc5b5\uc774 \ubcf4\uc774\uc9c0",
        "\uae30\uc5b5\uc774 \uc5c6",
        "\uae30\uc5b5\uc744 \ucc3e\uc9c0 \ubabb",
        "\uae30\uc5b5\uc774 \uc548 \ubcf4",
        "\ub0b4\uc6a9\uc774 \ubcf4\uc774\uc9c0",
        "\uad6c\uccb4\uc801\uc778 \ub0b4\uc6a9\uc774 \ubcf4\uc774\uc9c0",
        "\ub0b4\uc6a9\uc774 \ud3ec\ud568\ub418\uc5b4 \uc788\uc9c0",#20260629_kpopmodder: Skip prior denial answers before ranking recall evidence.
        "\uc138\ubd80 \uc0ac\ud56d\uc744 \uc81c\uacf5\ud560 \uc218 \uc5c6",
        "\ubb34\uc5c7\uc744 \ucc3e\uace0 \uacc4\uc2e0\uc9c0",
        "\uc5b4\ub5a4 \uc815\ubcf4\uac00 \ud544\uc694",
        "\uc5b4\ub5a4 \ub0b4\uc6a9\uc744 \ucc3e\uace0",
        "\uc870\uae08 \ub354 \uc54c\ub824\uc8fc",
        "i do not see",
        "i can't find",
        "i cannot find",
        "no specific memory",
        "no relevant memory",
    )
    SCREEN_AI_SUMMARY_TERMS = (
        "chatgpt_openai",
        "hybrid_openai_llm",
        "chatbot",
        "gradio",
        "\uc0ac\uc6a9\uc790\uac00",
        "\uc9c8\ubb38\ud588\uc2b5\ub2c8\ub2e4",
        "\ub77c\uace0 \ub2f5\ubcc0",
        "\ub2f5\ubcc0\ud588\uc2b5\ub2c8\ub2e4",
        "ai\uac00",
        "provider",
    )
    SCREEN_UI_NOISE_TERMS = (
        "obs studio",
        "codex",
        "code x",
        "vscode",
        "visual studio code",
        "lavi + ghost",
        "lavi+ghost",
        "vtuber_source_code",
        "starcraft16_config.json",
        "agents.md",
        "chatbot",
        "hybrid_openai_llm",
        "chatgpt_openai",
    )

    ACTIVITY_KEYWORDS = {
        "youtube": 5.0,
        "유튜브": 5.0,
        "영상": 3.0,
        "video": 3.0,
        "게임": 3.0,
        "game": 3.0,
        "steam": 3.0,
        "브라우저": 2.0,
        "browser": 2.0,
        "영화": 3.0,
        "방송": 3.0,
        "시청": 3.0,
        "재생": 2.0,
        "ghost hunter": 3.0,
    }
    SCREEN_EVENT_TYPES = (
        "screen_observation",
        "screen_observation_silent",
    )

    def __init__(
        self,
        memory_store,
        consolidator=None,
        max_raw_events=2000,
        max_results=4,
        minimum_score=1.15,
        derived_store=None,
        use_derived_fallback=False,
        allow_single_screen_observation_fallback=False,
        accuracy_first_raw_search=False,
        max_long_raw_events=15000,
        raw_search_time_budget_sec=1.5,
        raw_search_batch_size=500,
        raw_search_event_types=None,
    ):
        self.memory_store = memory_store
        self.consolidator = consolidator or MemoryConsolidator()
        self.max_raw_events = max(100, int(max_raw_events))
        self.max_results = max(1, int(max_results))
        self.minimum_score = float(minimum_score)
        self.derived_store = derived_store#20260626_kpopmodder: Optional raw-adjacent recall index; raw events remain fallback.
        self.use_derived_fallback = (
            "prefer"
            if use_derived_fallback == "prefer"
            else bool(use_derived_fallback)
        )#20260627_kpopmodder: Preserve opt-in derived-first mode while keeping False as the safe default.
        self.allow_single_screen_observation_fallback = bool(
            allow_single_screen_observation_fallback
        )#20260627_kpopmodder: Single ScreenVision derived rows require an explicit experiment flag.
        self.accuracy_first_raw_search = bool(
            accuracy_first_raw_search
        )#20260627_kpopmodder: Prefer complete raw DB search over fast recent-window shortcuts.
        self.max_long_raw_events = max(100, int(max_long_raw_events))
        self.raw_search_time_budget_sec = max(
            0.1,
            float(raw_search_time_budget_sec),
        )
        self.raw_search_batch_size = max(1, int(raw_search_batch_size))
        self.raw_search_event_types = tuple(
            raw_search_event_types
            or (
                "user_message",
                "assistant_message",
                "screen_observation",
                "screen_observation_silent",
            )
        )#20260703_kpopmodder: Filter recall to event types the consolidator can use.

    def retrieve(
        self,
        query,
        exclude_texts=None,
        use_derived_fallback_override=None,
        max_results_override=None,
        allow_deep_raw_search=None,
        include_screen_observations=None,
    ):#20260622_kpopmodder: 질문별 관련 회상 또는 최근 사건을 반환한다.
        query = self._clean_text(query)
        if not query or self.memory_store is None:
            return []

        #20260720_kpopmodder: Prompt builders can keep simple recall recent-only and screen-free.
        if allow_deep_raw_search is None:
            allow_deep_raw_search = True
        else:
            allow_deep_raw_search = bool(allow_deep_raw_search)
        if include_screen_observations is None:
            include_screen_observations = self._query_mentions_screen_memory(query)#20260720_kpopmodder
        else:
            include_screen_observations = bool(include_screen_observations)

        use_derived_fallback = self._resolve_derived_fallback_mode(
            use_derived_fallback_override,
        )#20260627_kpopmodder: Request-scoped derived-first mode must not mutate shared retriever state.
        result_limit = self._resolve_result_limit(
            max_results_override,
        )#20260627_kpopmodder: Explicit recall can request more evidence without mutating max_results.
        exclude_keys = self._exclude_keys(exclude_texts)#20260626_kpopmodder: Do not recall the current user input as past memory.

        if (
            self.derived_store is not None
            and use_derived_fallback == "prefer"
            and not (self.accuracy_first_raw_search and allow_deep_raw_search)
        ):
            try:#20260626_kpopmodder: Optional derived-first mode; raw recall remains fallback.
                derived_results = self._retrieve_from_derived(
                    query,
                    result_limit=result_limit,
                    include_screen_observations=include_screen_observations,
                )
                derived_results = self._filter_excluded_results(
                    derived_results,
                    exclude_keys,
                )
                if derived_results:
                    return self._finish_recall(query, derived_results)
            except Exception as exc:
                self._log_retrieval_warning(
                    "DerivedMemoryPreferFailed",
                    exc,
                    query=query,
                )
                self._schedule_derived_rebuild_after_error(
                    reason="derived_prefer_error",
                )

        if self.accuracy_first_raw_search and allow_deep_raw_search:
            #20260627_kpopmodder: Recall questions should scan all raw_events before trusting a fast recent hit.
            long_results = self._retrieve_long_raw_results(
                query,
                recent_events=[],
                result_limit=result_limit,
                include_screen_observations=include_screen_observations,
            )
            long_results = self._filter_excluded_results(
                long_results,
                exclude_keys,
            )
            if long_results:
                return self._finish_recall(query, long_results)

        recent_events = self._get_raw_events_safely(
            limit=self.max_raw_events,
        )
        recent_events = self._filter_raw_events_for_recall(
            recent_events,
            include_screen_observations=include_screen_observations,
        )
        recent_results = self._retrieve_from_raw_events(
            query,
            recent_events,
            recall_scope="raw_recent",
            result_limit=result_limit,
            include_screen_observations=include_screen_observations,
        )
        recent_results = self._filter_excluded_results(
            recent_results,
            exclude_keys,
        )

        if recent_results:
            #20260626_kpopmodder: Explicit old/all-memory requests should look beyond the recent window.
            if allow_deep_raw_search and self._should_expand_raw_search(query):
                expanded_results = self._retrieve_long_raw_results(
                    query,
                    recent_events,
                    result_limit=result_limit,
                    include_screen_observations=include_screen_observations,
                )
                expanded_results = self._filter_excluded_results(
                    expanded_results,
                    exclude_keys,
                )
                if expanded_results:
                    return self._finish_recall(query, expanded_results)

            return self._finish_recall(query, recent_results)

        #20260626_kpopmodder: Only scan long raw storage after the recent raw window misses.
        #20260720_kpopmodder: Simple recall requests keep this disabled to avoid topic drift.
        if allow_deep_raw_search:
            long_results = self._retrieve_long_raw_results(
                query,
                recent_events,
                result_limit=result_limit,
                include_screen_observations=include_screen_observations,
            )
            long_results = self._filter_excluded_results(
                long_results,
                exclude_keys,
            )
            if long_results:
                return self._finish_recall(query, long_results)

        if use_derived_fallback:
            try:#20260626_kpopmodder: Derived memory failures must not break raw-events recall.
                derived_results = self._retrieve_from_derived(
                    query,
                    result_limit=result_limit,
                    include_screen_observations=include_screen_observations,
                )
                derived_results = self._filter_excluded_results(
                    derived_results,
                    exclude_keys,
                )
                if derived_results:
                    return self._finish_recall(query, derived_results)
            except Exception as exc:
                self._log_retrieval_warning(
                    "DerivedMemoryFallbackFailed",
                    exc,
                    query=query,
                )
                self._schedule_derived_rebuild_after_error(
                    reason="derived_fallback_error",
                )

        return []

    def _resolve_derived_fallback_mode(self, override):#20260627_kpopmodder: Normalize per-request fallback overrides.
        if override is None:
            return self.use_derived_fallback

        if override == "prefer":
            return "prefer"

        return bool(override)

    def _schedule_derived_rebuild_after_error(self, reason):
        if self.derived_store is None or self.memory_store is None:
            return

        required_methods = ("clear", "upsert_memory", "get_stats")
        if not all(
            callable(getattr(self.derived_store, method_name, None))
            for method_name in required_methods
        ):
            return

        try:
            DerivedMemoryRebuildService.schedule_background_rebuild(
                self.derived_store,
                self.memory_store,
                reason=reason,
            )
        except Exception as exc:
            self._log_retrieval_warning(
                "DerivedMemoryBackgroundRebuildScheduleFailed",
                exc,
                query=reason,
            )

    def _resolve_result_limit(self, override):#20260627_kpopmodder: Keep broad recall limits request-scoped.
        if override is None:
            return self.max_results

        try:
            return max(1, int(override))
        except Exception:
            return self.max_results

    def _retrieve_long_raw_results(
        self,
        query,
        recent_events,
        result_limit=None,
        include_screen_observations=True,
    ):#20260626_kpopmodder: Deep recall can scan beyond the recent window on request.
        long_events = self._get_raw_events_safely(
            limit=None,
            event_types=self._raw_event_types_for_recall(
                include_screen_observations,
            ),
            max_events=self.max_long_raw_events,
            time_budget_sec=self.raw_search_time_budget_sec,
        )
        if not long_events or len(long_events) < len(recent_events or []):
            return []

        return self._retrieve_from_raw_events(
            query,
            long_events,
            recall_scope="raw_long_search",
            suppress_recent_deep_echo=True,
            focus_tokens=self._deep_recall_filter_tokens(query),
            result_limit=result_limit,
            include_screen_observations=include_screen_observations,
        )

    def _is_recent_deep_recall_echo(self, query, episode, newest_ts):#20260626_kpopmodder: Deep recall should prefer old source events over just-produced summaries.
        if str(episode.get("kind", "")) != "conversation":
            return False

        created_ts = self._float_or_zero(episode.get("created_ts"))
        newest_ts = self._float_or_zero(newest_ts)
        if created_ts <= 0.0 or newest_ts <= 0.0:
            return False

        #20260626_kpopmodder: Recent deep-recall answers can otherwise outrank older source rows.
        if newest_ts - created_ts > 300.0:
            return False

        user_text = self._clean_text(episode.get("user_text"))
        if not self._should_expand_raw_search(user_text):
            return False

        query_focus = self._deep_recall_focus_tokens(query)
        user_focus = self._deep_recall_focus_tokens(user_text)
        if query_focus and user_focus and query_focus.isdisjoint(user_focus):
            return False

        return True

    def _deep_recall_focus_tokens(self, text):
        return self._tokens(text) - self.STOP_WORDS - self._deep_recall_tokens()

    def _deep_recall_filter_tokens(self, text):#20260626_kpopmodder: Topic words should outrank generic old/all-memory wording.
        return self._expand_focus_aliases(
            self._filter_command_focus_tokens(
                self._deep_recall_focus_tokens(text)
            )
        )

    def _recall_command_terms(self):
        return (
            "\ucc3e\uc544\uc918",
            "\ucc3e\uc544",
            "\ucc3e\uc544\ubd10",
            "\ucc3e\uc544\uc904\ub798",
            "\uc54c\ub824\uc918",
            "\ub9d0\ud574\uc918",
            "\uae30\uc5b5",
            "\uae30\uc5b5\ub098",
            "\uae30\uc5b5\ub098\ub294",
            "\uae30\uc5b5\ub098\ub2c8",
            "\uae30\uc5b5\ud574",
            "\uae30\uc5b5\ud558",
            "\uac70",
            "\uac80\uc0c9",
            "find",
            "search",
            "look",
            "lookup",
            "memory",
            "memories",
        )

    def _filter_command_focus_tokens(self, tokens):
        command_tokens = set()
        command_keys = set()
        for term in self._recall_command_terms():
            command_tokens.update(self._tokens(term))
            key = self._comparison_key(term)
            if key:
                command_keys.add(key)

        filtered = set()
        for token in tokens or []:
            token = self._clean_text(token).lower()
            token_key = self._comparison_key(token)
            if not token or not token_key:
                continue
            if token in command_tokens:
                continue
            if any(
                len(command_key) >= 2
                and (
                    command_key in token_key
                    or token_key in command_key
                )
                for command_key in command_keys
            ):
                continue
            filtered.add(token)

        return filtered

    def _expand_focus_aliases(self, tokens):#20260627_kpopmodder: Treat common OCR/game-title variants as one recall topic.
        expanded = set()
        alias_map = {
            "\ubd95\uad34": {"\ubd09\uad34"},
            "\ubd09\uad34": {"\ubd95\uad34"},
            "honkai": {"\ubd95\uad34", "\ubd09\uad34"},
        }
        for token in tokens or []:
            token = self._clean_text(token).lower()
            if not token:
                continue
            expanded.add(token)
            token_key = self._comparison_key(token)
            expanded.update(alias_map.get(token, set()))
            expanded.update(alias_map.get(token_key, set()))
        return expanded

    def _prefer_deep_recall_focus_results(self, scored, focus_tokens):
        if not scored or not focus_tokens:
            return scored

        focused = [
            item for item in scored
            if self._recall_item_matches_focus(item, focus_tokens)
        ]
        if not focused:
            return scored

        return focused

    def _recall_item_matches_focus(self, item, focus_tokens):#20260627_kpopmodder: Prefer true topic hits over sidebar-only ScreenVision noise.
        expanded_focus = self._expand_focus_aliases(focus_tokens)
        strong_focus = self._strong_focus_tokens(expanded_focus)
        search_text = self._clean_text(
            item.get("search_text") or item.get("text")
        )
        candidate_tokens = self._expand_focus_aliases(
            self._tokens(search_text)
        )

        if strong_focus:
            if not (
                candidate_tokens & strong_focus
                or self._comparison_key_has_any(search_text, strong_focus)
            ):
                return False
            if (
                str(item.get("kind", "")) == "screen_observation"
                and not self._screen_primary_matches_focus(
                    search_text,
                    strong_focus,
                )
            ):
                return False
            return True

        return bool(candidate_tokens & expanded_focus)

    def _strong_focus_tokens(self, focus_tokens):
        strong = set()
        for token in self._expand_focus_aliases(focus_tokens):
            token = self._clean_text(token).lower()
            if not token:
                continue
            if re.fullmatch(r"[0-9a-z_]+", token) and len(token) <= 3:
                continue
            strong.add(token)
        return strong

    def _screen_primary_matches_focus(self, text, strong_focus):
        primary = self._screen_primary_text(text)
        primary_tokens = self._expand_focus_aliases(self._tokens(primary))
        return bool(
            primary_tokens & strong_focus
            or self._comparison_key_has_any(primary, strong_focus)
        )

    def _comparison_key_has_any(self, text, tokens):
        compact = self._comparison_key(text)
        if not compact:
            return False

        for token in tokens or []:
            token_key = self._comparison_key(token)
            if token_key and token_key in compact:
                return True
        return False

    def _screen_primary_text(self, text):
        text = self._clean_text(text)
        if not text:
            return ""

        splitters = (
            "\ud654\uba74 \uc624\ub978\ucabd",
            "\uc624\ub978\ucabd\uc5d0\ub294",
            "\uad00\ub828 \ucf58\ud150\uce20",
            "\uad6c\ub3c5 \ubaa9\ub85d",
            "\uac80\uc0c9 \uacb0\uacfc \ubaa9\ub85d",
            "Chatbot",
            "Gradio",
        )
        lowered = text.lower()
        cut_at = len(text)
        for marker in splitters:
            index = lowered.find(marker.lower())
            if index > 0:
                cut_at = min(cut_at, index)

        return text[: min(cut_at, 360)]

    def _deep_recall_tokens(self):
        tokens = set()
        for term in self._deep_recall_terms():
            tokens.update(self._tokens(term))
        return tokens

    def _should_expand_raw_search(self, query):#20260626_kpopmodder: Keep normal recall fast, but honor explicit old/all-memory requests.
        compact = self._comparison_key(query)
        if not compact:
            return False

        if self._query_mentions_watched_screen_memory(query):#20260720_kpopmodder
            return True

        query_tokens = self._tokens(query)
        for term in self._deep_recall_terms():
            if re.fullmatch(r"[0-9a-zA-Z_]+", term):
                if term in query_tokens:
                    return True
                continue
            if term in compact:
                return True
        return False

    def _deep_recall_terms(self):
        return (
            "\uc624\ub798\ub41c",
            "\uc624\ub798\uc804",
            "\uc804\ubd80",
            "\uc804\uccb4",
            "\ubaa8\ub450",
            "\ubaa8\ub4e0",
            "\ub2e4\ucc3e",
            "\ub2e4\uae30\uc5b5",
            "\ubcf8\uc601\uc0c1",
            "\ubcf8\ub3d9\uc601\uc0c1",
            "\ubd24\ub358\uc601\uc0c1",
            "\ubd24\ub358\ub3d9\uc601\uc0c1",
            "\ubd24\ub358\uc720\ud29c\ube0c",
            "\uc2dc\uccad\ud588\ub358",
            "\uc2dc\uccad\ud55c",
            "\uc7ac\uc0dd\ud588\ub358",
            "\uc7ac\uc0dd\ud55c",
            "old",
            "older",
            "oldest",
            "earlier",
            "earliest",
            "first",
            "all",
            "everything",
            "entire",
            "full",
            "watched",
            "seen",
        )

    def _query_mentions_screen_memory(self, query):#20260720_kpopmodder
        compact = self._comparison_key(query)
        normalized = self._clean_text(query).lower()
        if not compact and not normalized:
            return False

        terms = (
            "\ud654\uba74",
            "\uc2a4\ud06c\ub9b0",
            "\uc720\ud29c\ube0c",
            "\uc601\uc0c1",
            "\ub3d9\uc601\uc0c1",
            "\uc2dc\uccad",
            "\uc7ac\uc0dd",
            "screen",
            "youtube",
            "video",
        )
        return any(
            term.lower() in normalized or self._comparison_key(term) in compact
            for term in terms
        )

    def _query_mentions_watched_screen_memory(self, query):#20260720_kpopmodder
        compact = self._comparison_key(query)
        if not compact:
            return False

        screen_terms = (
            "\uc720\ud29c\ube0c",
            "\uc601\uc0c1",
            "\ub3d9\uc601\uc0c1",
            "youtube",
            "video",
        )
        watched_terms = (
            "\ubd24\ub358",
            "\ubcf8",
            "\uc2dc\uccad",
            "\uc7ac\uc0dd",
            "watched",
            "seen",
        )
        return (
            any(self._comparison_key(term) in compact for term in screen_terms)
            and any(self._comparison_key(term) in compact for term in watched_terms)
        )

    def _raw_event_types_for_recall(self, include_screen_observations):#20260720_kpopmodder
        if include_screen_observations:
            return self.raw_search_event_types

        return tuple(
            event_type
            for event_type in self.raw_search_event_types
            if str(event_type) not in self.SCREEN_EVENT_TYPES
        )

    def _filter_raw_events_for_recall(
        self,
        events,
        include_screen_observations=True,
    ):#20260720_kpopmodder
        if include_screen_observations:
            return list(events or [])

        return [
            event
            for event in events or []
            if str(event.get("event_type", "")) not in self.SCREEN_EVENT_TYPES
        ]

    def _get_raw_events_safely(
        self,
        limit,
        event_types=None,
        max_events=None,
        time_budget_sec=None,
    ):#20260626_kpopmodder: Raw recall lookup must fail closed instead of breaking chat.
        try:
            if (
                limit is not None
                and event_types is None
                and max_events is None
                and time_budget_sec is None
            ):
                return self.memory_store.get_raw_events(limit=limit)

            if hasattr(self.memory_store, "iter_raw_events"):
                started_at = time.monotonic()
                events = list(self.memory_store.iter_raw_events(
                    limit=limit,
                    event_types=event_types,
                    batch_size=self.raw_search_batch_size,
                    max_events=max_events,
                    time_budget_sec=time_budget_sec,
                ))
                if time_budget_sec is not None:
                    elapsed = time.monotonic() - started_at
                    if elapsed >= float(time_budget_sec):
                        self._log_retrieval_warning(
                            "RawEventsLookupBudgetReached",
                            TimeoutError("raw event lookup time budget reached"),
                            limit=max_events,
                        )
                return events

            return self.memory_store.get_raw_events(limit=limit)
        except Exception as exc:
            self._log_retrieval_warning(
                "RawEventsLookupFailed",
                exc,
                limit=max_events if limit is None else limit,
            )
            return []

    def _retrieve_from_raw_events(
        self,
        query,
        events,
        recall_scope,
        suppress_recent_deep_echo=False,
        focus_tokens=None,
        result_limit=None,
        include_screen_observations=True,
    ):#20260626_kpopmodder: Search source-of-truth raw episodes first.
        if not events:
            return []

        result_limit = self._resolve_result_limit(result_limit)
        episodes = self.consolidator.consolidate(events)
        query_key = self._comparison_key(query)

        if self._is_generic_recall_query(query):
            return self._retrieve_recent_episodes(
                episodes,
                query_key=query_key,
                result_limit=result_limit,
                include_screen_observations=include_screen_observations,
            )

        scored = []
        #20260626_kpopmodder: Keep recency as a small transparent tie-breaker, not a replacement for keyword match.
        created_values = [
            self._float_or_zero(episode.get("created_ts"))
            for episode in episodes
        ]
        oldest_ts = min(created_values) if created_values else 0.0
        newest_ts = max(created_values) if created_values else 0.0

        for index, episode in enumerate(episodes):
            if (
                not include_screen_observations
                and str(episode.get("kind", "")) == "screen_observation"
            ):
                continue
            search_text = self._clean_text(episode.get("search_text"))
            if not search_text:
                continue

            #20260622_kpopmodder: The current user query is recorded before recall runs.
            # Exclude that identical event so the AI does not "remember" the present as the past.
            if self._comparison_key(search_text) == query_key:
                continue
            if self._should_skip_recall_episode(episode):
                continue
            if (
                suppress_recent_deep_echo
                and self._is_recent_deep_recall_echo(
                    query,
                    episode,
                    newest_ts,
                )
            ):
                continue

            score_parts = self._score_breakdown(#20260626_kpopmodder: Preserve score reasons for log-only recall evidence.
                query,
                search_text,
                episode=episode,
                oldest_ts=oldest_ts,
                newest_ts=newest_ts,
            )
            score = score_parts["total_score"]
            if score < self.minimum_score:
                continue

            result = dict(episode)
            result["score"] = round(score, 4)
            result["_score_breakdown"] = score_parts
            result["_order"] = index
            result["recall_mode"] = recall_scope
            scored.append(result)

        if recall_scope == "raw_long_search":
            scored = self._prefer_deep_recall_focus_results(
                scored,
                focus_tokens,
            )

        scored.sort(
            key=lambda item: (
                item.get("score", 0.0),
                item.get("created_ts", 0.0),
                item.get("_order", 0),
            ),
            reverse=True,
        )

        results = []
        seen = set()
        for item in scored:
            key = self._comparison_key(item.get("text"))
            if not key or key in seen:
                continue
            seen.add(key)
            item.pop("_order", None)
            results.append(item)
            if len(results) >= result_limit:
                break

        return results

    def _retrieve_recent_episodes(
        self,
        episodes,
        query_key,
        result_limit=None,
        include_screen_observations=True,
    ):#20260622_kpopmodder: 일반 회상 질문에는 대화와 실제 화면 활동을 함께 찾는다.
        """대상이 없는 회상 질문에는 최근 대화와 실제 화면 활동을 반환한다."""
        result_limit = self._resolve_result_limit(
            result_limit
        )#20260629_kpopmodder: Keep max_results_override effective for generic recall.
        conversation_results = []
        screen_results = []
        seen = set()

        for episode in reversed(episodes or []):
            kind = str(episode.get("kind", ""))
            if kind not in {"conversation", "screen_observation"}:
                continue
            if kind == "screen_observation" and not include_screen_observations:
                continue

            if (
                kind == "conversation"
                and str(episode.get("assistant_source", "")).lower()
                == "memory_command"
            ):
                continue
            if self._should_skip_recall_episode(episode):
                continue

            search_text = self._clean_text(episode.get("search_text"))
            key = self._comparison_key(search_text)

            if not key or key == query_key or key in seen:
                continue

            if kind == "conversation":
                user_text = self._clean_text(episode.get("user_text"))
                if self._is_generic_recall_query(user_text):
                    continue

            if self._looks_like_noise(search_text):
                continue

            seen.add(key)
            result = dict(episode)
            result["score"] = 0.0
            result["_score_breakdown"] = {
                "total_score": 0.0,
                "keyword_score": 0.0,
                "ngram_score": 0.0,
                "sequence_score": 0.0,
                "exact_bonus": 0.0,
                "recent_score": 0.0,
                "source_score": self._source_score(episode),
                "length_penalty": 0.0,
            }
            result["recall_mode"] = "recent_events"

            if kind == "screen_observation":
                result["activity_score"] = self._activity_score(search_text)
                screen_results.append(result)
            else:
                conversation_results.append(result)

        screen_limit = max(1, result_limit // 2)
        selected_screens = self._select_distinct_screen_events(
            screen_results,
            limit=screen_limit,
        )
        conversation_limit = max(0, result_limit - len(selected_screens))
        results = (
            selected_screens
            + conversation_results[:conversation_limit]
        )

        if len(results) < result_limit:
            selected_keys = {
                self._comparison_key(item.get("search_text"))
                for item in results
            }
            remaining_screens = [
                item
                for item in screen_results
                if self._comparison_key(item.get("search_text"))
                not in selected_keys
            ]
            remaining = remaining_screens + conversation_results[
                conversation_limit:
            ]
            remaining.sort(
                key=lambda item: (
                    item.get("activity_score", 0.0),
                    item.get("created_ts", 0.0),
                ),
                reverse=True,
            )
            for item in remaining:
                key = self._comparison_key(item.get("search_text"))
                if key in selected_keys:
                    continue
                selected_keys.add(key)
                results.append(item)
                if len(results) >= result_limit:
                    break

        results.sort(
            key=lambda item: item.get("created_ts", 0.0),
            reverse=True,
        )
        return results[:result_limit]

    def _select_distinct_screen_events(self, items, limit):#20260622_kpopmodder: 비슷한 연속 화면 기억의 반복을 줄인다.
        ranked = sorted(
            items or [],
            key=lambda item: (
                item.get("activity_score", 0.0),
                item.get("created_ts", 0.0),
            ),
            reverse=True,
        )
        selected = []
        selected_topics = set()

        for item in ranked:
            text = self._clean_text(item.get("search_text"))
            topic = self._screen_topic(text)

            if topic and topic in selected_topics:
                continue
            if self._is_similar_to_selected_screen(text, selected):
                continue

            selected.append(item)
            if topic:
                selected_topics.add(topic)
            if len(selected) >= limit:
                break

        return selected

    def _activity_score(self, text):#20260622_kpopmodder: 영상과 게임 같은 실제 활동 기록을 우선한다.
        normalized = self._clean_text(text).lower()
        return sum(
            weight
            for keyword, weight in self.ACTIVITY_KEYWORDS.items()
            if keyword in normalized
        )

    def _screen_topic(self, text):
        normalized = self._clean_text(text).lower()
        topics = (
            ("youtube", ("youtube", "유튜브")),
            ("video", ("영상", "video", "영화", "방송", "시청", "재생")),
            ("game", ("게임", "game", "steam", "ghost hunter")),
            ("browser", ("브라우저", "browser")),
        )
        for topic, keywords in topics:
            if any(keyword in normalized for keyword in keywords):
                return topic
        return ""

    def _is_similar_to_selected_screen(self, text, selected_items):
        compact_text = self._comparison_key(text)
        if not compact_text:
            return True

        for item in selected_items:
            previous = self._comparison_key(item.get("search_text"))
            if not previous:
                continue
            similarity = SequenceMatcher(
                None,
                compact_text,
                previous,
            ).ratio()
            if similarity >= 0.82:
                return True

        return False

    def _exclude_keys(self, texts):
        keys = set()
        for text in texts or []:
            key = self._comparison_key(text)
            if key:
                keys.add(key)
        return keys

    def _filter_excluded_results(self, results, exclude_keys):
        if not exclude_keys:
            return results

        return [
            item
            for item in results or []
            if not self._is_excluded_recall_item(item, exclude_keys)
        ]

    def _is_excluded_recall_item(self, item, exclude_keys):
        for field_name in ("search_text", "user_text"):
            key = self._comparison_key(item.get(field_name))
            if key and key in exclude_keys:
                return True
        return False

    def _should_skip_recall_episode(self, episode):
        if self._is_assistant_memory_answer_echo(episode):
            return True

        text = self._clean_text(
            episode.get("search_text") or episode.get("text")
        )
        if self._looks_like_memory_failure_answer(text):
            return True

        if str(episode.get("kind", "")) == "screen_observation":
            return self._looks_like_ai_screen_summary(text)

        return False

    def _looks_like_memory_failure_answer(self, text):
        normalized = self._clean_text(text).lower()
        compact = self._comparison_key(text)
        if not normalized and not compact:
            return False

        return any(
            term.lower() in normalized or self._comparison_key(term) in compact
            for term in self.MEMORY_FAILURE_ANSWER_TERMS
        )

    def _looks_like_ai_screen_summary(self, text):
        normalized = self._clean_text(text).lower()
        compact = self._comparison_key(text)
        if not normalized and not compact:
            return False

        hit_count = 0
        for term in self.SCREEN_AI_SUMMARY_TERMS:
            if term.lower() in normalized or self._comparison_key(term) in compact:
                hit_count += 1
        if hit_count >= 2:
            return True

        ui_noise_count = 0
        for term in self.SCREEN_UI_NOISE_TERMS:
            if term.lower() in normalized or self._comparison_key(term) in compact:
                ui_noise_count += 1
        return ui_noise_count >= 2

    def _is_assistant_memory_answer_echo(self, episode):
        if str(episode.get("kind", "")) != "conversation":
            return False

        user_text = self._clean_text(episode.get("user_text"))
        assistant_text = self._clean_text(episode.get("assistant_text"))
        if not user_text or not assistant_text:
            return False

        #20260626_kpopmodder: Do not let an AI answer to a memory question become new recall evidence.
        compact_user = self._comparison_key(user_text)
        has_recall_signal = any(
            self._comparison_key(term) in compact_user
            for term in self.ASSISTANT_ECHO_RECALL_TERMS
        )
        if has_recall_signal:
            return True

        has_request_signal = "?" in user_text or any(
            self._comparison_key(term) in compact_user
            for term in self.ASSISTANT_ECHO_REQUEST_TERMS
        )
        has_subject_signal = any(
            self._comparison_key(term) in compact_user
            for term in self.ASSISTANT_ECHO_SUBJECT_TERMS
        )
        return has_request_signal and has_subject_signal

    def _is_generic_recall_query(self, query):#20260622_kpopmodder: 옛날일 기억나 같은 일반 회상 표현과 오타를 감지한다.
        compact = self._comparison_key(query)
        if not compact:
            return False

        has_recall_word = any(
            phrase in compact
            for phrase in self.GENERIC_RECALL_PHRASES
        )
        if not has_recall_word:
            return False

        remaining = compact
        removable = (
            "옛날일", "옜날일", "예전일", "과거일",
            "옛날", "옜날", "예전", "과거", "전에", "그때",
            "기억나니", "기억나요", "기억나", "기억해",
            "거억나", "거억해", "거억",
            "떠올라", "뭐", "무슨일", "일들", "일",
            "좀", "혹시", "아직", "너", "넌",
            "doyou", "remember", "oldtimes", "anything",
            "frombefore", "thepast", "past", "before",
        )
        for word in removable:
            remaining = remaining.replace(word, "")

        remaining = re.sub(
            r"(은|는|이|가|을|를|도|만|나|니|나요|해|줘|줄래)+$",
            "",
            remaining,
        )
        return len(remaining) <= 1

    def _looks_like_noise(self, text):
        compact = self._comparison_key(text)
        if len(compact) < 4:
            return True

        unique_ratio = len(set(compact)) / len(compact)
        if len(compact) >= 20 and unique_ratio < 0.12:
            return True

        return False

    def _score(self, query, candidate):
        return self._score_breakdown(query, candidate)["total_score"]

    #20260626_kpopmodder: Split recall score into auditable parts for logs.
    def _score_breakdown(
        self,
        query,
        candidate,
        episode=None,
        oldest_ts=0.0,
        newest_ts=0.0,
    ):
        query_tokens = self._expand_focus_aliases(self._tokens(query))
        candidate_tokens = self._expand_focus_aliases(self._tokens(candidate))
        meaningful_query = (
            self._deep_recall_filter_tokens(query)
            or (query_tokens - self.STOP_WORDS)
        )

        if meaningful_query:
            overlap = meaningful_query & candidate_tokens
            token_score = 3.0 * len(overlap) / len(meaningful_query)
        else:
            overlap = query_tokens & candidate_tokens
            token_score = 1.5 * len(overlap) / max(1, len(query_tokens))

        query_ngrams = self._character_ngrams(query)
        candidate_ngrams = self._character_ngrams(candidate)
        ngram_overlap = query_ngrams & candidate_ngrams
        ngram_score = 0.0
        if query_ngrams:
            ngram_score = 1.8 * len(ngram_overlap) / len(query_ngrams)

        compact_query = self._comparison_key(query)
        compact_candidate = self._comparison_key(candidate)
        sequence_score = SequenceMatcher(
            None,
            compact_query,
            compact_candidate,
        ).ratio()

        exact_bonus = 0.0
        if compact_query and compact_query in compact_candidate:
            exact_bonus = 1.5

        length_penalty = 0.0
        if len(compact_candidate) > 1200:
            length_penalty = math.log10(len(compact_candidate) / 1200) * 0.2

        recent_score = self._recent_score(
            episode,
            oldest_ts=oldest_ts,
            newest_ts=newest_ts,
        )
        source_score = self._source_score(episode)
        topic_score = self._topic_focus_score(
            query,
            candidate,
            episode=episode,
        )
        total_score = (
            token_score
            + ngram_score
            + sequence_score
            + exact_bonus
            + recent_score
            + source_score
            + topic_score
            - length_penalty
        )

        return {
            "total_score": total_score,
            "keyword_score": token_score,
            "ngram_score": ngram_score,
            "sequence_score": sequence_score,
            "exact_bonus": exact_bonus,
            "recent_score": recent_score,
            "source_score": source_score,
            "topic_score": topic_score,
            "length_penalty": length_penalty,
        }

    def _topic_focus_score(self, query, candidate, episode=None):#20260627_kpopmodder: Boost strong topic matches without trusting sidebar-only screen text.
        strong_focus = self._strong_focus_tokens(
            self._deep_recall_filter_tokens(query)
        )
        if not strong_focus:
            return 0.0

        candidate_tokens = self._expand_focus_aliases(self._tokens(candidate))
        if not (
            candidate_tokens & strong_focus
            or self._comparison_key_has_any(candidate, strong_focus)
        ):
            return 0.0

        if (
            episode
            and str(episode.get("kind", "")) == "screen_observation"
        ):
            if self._screen_primary_matches_focus(candidate, strong_focus):
                return 1.2
            return 0.0

        return 1.0

    def _recent_score(self, episode, oldest_ts=0.0, newest_ts=0.0):#20260626_kpopmodder: Small recency boost for ordering only.
        if not episode:
            return 0.0

        created_ts = self._float_or_zero(episode.get("created_ts"))
        oldest_ts = self._float_or_zero(oldest_ts)
        newest_ts = self._float_or_zero(newest_ts)
        if created_ts <= 0.0 or newest_ts <= oldest_ts:
            return 0.0

        ratio = (created_ts - oldest_ts) / (newest_ts - oldest_ts)
        ratio = min(1.0, max(0.0, ratio))
        return 0.12 * ratio

    def _source_score(self, episode):#20260626_kpopmodder: Prefer user/conversation memories slightly over screen-only context.
        if not episode:
            return 0.0

        kind = str(episode.get("kind", ""))
        if kind == "conversation":
            return 0.08
        if kind == "user_message":
            return 0.06
        if kind == "screen_observation":
            return 0.02
        return 0.0

    def _tokens(self, text):
        return {
            token
            for token in re.findall(
                r"[가-힣]{2,}|[a-zA-Z0-9_]{2,}",
                self._clean_text(text).lower(),
            )
        }

    def _character_ngrams(self, text):
        compact = self._comparison_key(text)
        if len(compact) < 2:
            return set()

        grams = set()
        for size in (2, 3):
            if len(compact) < size:
                continue
            grams.update(
                compact[index:index + size]
                for index in range(len(compact) - size + 1)
            )
        return grams

    def _comparison_key(self, text):
        return re.sub(
            r"[^0-9a-zA-Z가-힣]+",
            "",
            self._clean_text(text).lower(),
        )

    def _clean_text(self, text):
        return " ".join(str(text or "").strip().split())

    def _float_or_zero(self, value):
        try:
            return float(value)
        except Exception:
            return 0.0

    def _int_or_zero(self, value):
        try:
            return int(value)
        except Exception:
            return 0

    def _finish_recall(self, query, results):#20260626_kpopmodder: Log evidence, then strip trace fields before prompt injection.
        self._log_recall_evidence(query, results)
        return [self._public_recall_item(item) for item in results]

    def _log_recall_evidence(self, query, results):#20260626_kpopmodder: Recall evidence is visible in logs only, not chat.
        if not results:
            return

        logger = logging.getLogger(MEMORY_LOGGER_NAME)
        try:
            mode = str(results[0].get("recall_mode", "") or "unknown")
            source_store = (
                results[0].get("_source_store")
                or self._source_store_for_mode(mode)
            )
            raw_ids = self._collect_raw_event_ids(results)
            derived_ids = self._collect_derived_memory_ids(results)
            logger.info(
                "[MemoryRecall] "
                f"query='{self._log_snippet(query, 80)}' "
                f"mode={mode} source_store={source_store} "
                f"count={len(results)} raw_event_ids={raw_ids} "
                f"derived_memory_ids={derived_ids}"
            )

            top = results[0]
            score_parts = top.get("_score_breakdown") or {}
            derived_counts = self._derived_counts_log_fragment(top)
            logger.info(
                "[MemoryRecallTop] "
                f"mode={mode} kind={top.get('kind', '')} "
                f"created_at='{self._log_snippet(top.get('created_at'), 40)}' "
                f"source='{self._episode_source(top)}' "
                f"raw_event_ids={top.get('raw_event_ids', [])} "
                f"derived_memory_id={top.get('derived_memory_id', '')} "
                f"{derived_counts}"
                f"text='{self._log_snippet(top.get('search_text') or top.get('text'), 160)}'"
            )
            if score_parts:
                logger.info(
                    "[MemoryRecallScore] "
                    f"mode={mode} total={top.get('score', 0.0)} "
                    f"keyword={score_parts.get('keyword_score', 0.0):.4f} "
                    f"ngram={score_parts.get('ngram_score', 0.0):.4f} "
                    f"sequence={score_parts.get('sequence_score', 0.0):.4f} "
                    f"exact={score_parts.get('exact_bonus', 0.0):.4f} "
                    f"recent={score_parts.get('recent_score', 0.0):.4f} "
                    f"source={score_parts.get('source_score', 0.0):.4f} "
                    f"topic={score_parts.get('topic_score', 0.0):.4f} "
                    f"length_penalty={score_parts.get('length_penalty', 0.0):.4f}"
                )
        except Exception:
            return

    def _log_retrieval_warning(self, marker, error, query=None, limit=None):#20260627_kpopmodder: Fail closed, but leave recall failure evidence in logs.
        logger = logging.getLogger(MEMORY_LOGGER_NAME)
        try:
            parts = [
                f"[MemoryRetriever{marker}]",
                f"error_type={type(error).__name__}",
            ]
            if limit is not None:
                parts.append(f"limit={limit}")
            logger.warning(" ".join(parts))
            logger.debug(
                "[MemoryRetrieverFailureDebug] "
                f"marker={marker} "
                f"query='{self._log_snippet(query, 80)}' "
                f"error={error!r}"
            )
        except Exception:
            return

    def _source_store_for_mode(self, mode):#20260626_kpopmodder: Make derived recall clearly non-authoritative in logs.
        if mode == "derived_memory":
            return "derived_memory.sqlite3(reference_index_not_source_of_truth)"
        if mode == "raw_long_search":
            return "raw_events.sqlite3/raw_events.jsonl"
        return "raw_events.sqlite3"

    def _collect_raw_event_ids(self, results):
        ids = []
        seen = set()
        for item in results or []:
            for raw_event_id in item.get("raw_event_ids", []) or []:
                if raw_event_id in seen:
                    continue
                seen.add(raw_event_id)
                ids.append(raw_event_id)
        return ids

    def _collect_derived_memory_ids(self, results):
        ids = []
        for item in results or []:
            memory_id = item.get("derived_memory_id")
            if memory_id is not None:
                ids.append(memory_id)
        return ids

    def _episode_source(self, item):
        return (
            item.get("source")
            or item.get("user_source")
            or item.get("assistant_source")
            or ""
        )

    def _log_snippet(self, value, limit):
        text = self._clean_text(value)
        text = text.replace("'", "\\'")
        if len(text) <= limit:
            return text
        return text[: max(0, limit - 15)] + "...[truncated]"

    def _derived_counts_log_fragment(self, item):
        if item.get("derived_memory_id") is None:
            return ""

        return (
            f"source_event_count={item.get('source_event_count', '')} "
            f"duplicate_count={item.get('duplicate_count', '')} "
        )

    def _public_recall_item(self, item):#20260626_kpopmodder: Do not leak raw ids/hashes into LLM prompt text.
        public_item = dict(item)
        for key in (
            "_score_breakdown",
            "_order",
            "_source_store",
            "raw_event_ids",
            "raw_line_hashes",
            "derived_memory_id",
        ):
            public_item.pop(key, None)
        return public_item

    def _retrieve_from_derived(
        self,
        query,
        result_limit=None,
        include_screen_observations=True,
    ):#20260626_kpopmodder: Return derived rows in the existing episode dict shape.
        if self.derived_store is None:
            return []

        result_limit = self._resolve_result_limit(result_limit)
        if self._is_generic_recall_query(query):
            rows = self.derived_store.get_recent(limit=result_limit)
            recall_mode = "recent_events"
        else:
            rows = self.derived_store.search(
                query,
                limit=result_limit,
            )
            recall_mode = "derived_memory"

        results = []
        query_key = self._comparison_key(query)
        seen = set()

        for row in rows or []:
            if not self._is_eligible_derived_row(
                row,
                include_screen_observations=include_screen_observations,
            ):
                continue

            episode = self._derived_row_to_episode(row, recall_mode)
            search_key = self._comparison_key(episode.get("search_text"))
            text_key = self._comparison_key(episode.get("text"))

            if not search_key or search_key == query_key:
                continue
            if text_key in seen:
                continue

            seen.add(text_key)
            results.append(episode)
            if len(results) >= result_limit:
                break

        return results

    def _is_eligible_derived_row(
        self,
        row,
        include_screen_observations=True,
    ):#20260720_kpopmodder: Conversation rows may be used; screen rows stay gated.
        kind = str(row.get("kind", ""))
        if kind != "screen_observation":
            return kind in {"conversation", "user_message", "derived_memory"}

        if not include_screen_observations:
            return False

        meaningful_text = (
            self._clean_text(row.get("search_text"))
            or self._clean_text(row.get("summary"))
        )
        if not meaningful_text:
            return False#20260627_kpopmodder: Empty derived rows are not useful fallback evidence.

        if self.allow_single_screen_observation_fallback:
            return True

        source_event_count = self._int_or_zero(row.get("source_event_count"))
        duplicate_count = self._int_or_zero(row.get("duplicate_count"))
        metadata = row.get("metadata", {})
        if not isinstance(metadata, dict):
            metadata = {}
        base_normalized_key_count = self._int_or_zero(
            metadata.get("base_normalized_key_count")
        )
        return (
            source_event_count >= 2
            or duplicate_count >= 1
            or base_normalized_key_count >= 2
        )

    def _derived_row_to_episode(self, row, recall_mode):#20260626_kpopmodder: Convert optional reference-index rows without claiming raw authority.
        kind = str(row.get("kind", "") or "derived_memory")
        summary = self._clean_text(row.get("summary"))
        search_text = self._clean_text(row.get("search_text")) or summary
        created_at = (
            row.get("last_created_at")
            or row.get("first_created_at")
            or ""
        )
        created_ts = (
            row.get("last_created_ts")
            or row.get("first_created_ts")
            or 0.0
        )

        return {
            "kind": kind,
            "text": search_text or summary,
            "search_text": search_text,
            "created_at": created_at,
            "created_ts": created_ts,
            "score": row.get("score", row.get("confidence", 0.0)),
            "recall_mode": recall_mode,
            "_source_store": "derived_memory.sqlite3(reference_index_not_source_of_truth)",
            "derived_memory_id": row.get("id"),
            "title": row.get("title", ""),
            "topic_key": row.get("topic_key", ""),
            "source_event_count": row.get("source_event_count", 1),
            "duplicate_count": row.get("duplicate_count", 0),
            "raw_event_ids": self._metadata_list(
                row,
                "source_event_ids",
                fallback_key="raw_event_ids",
            ),
            "raw_line_hashes": self._metadata_list(row, "raw_line_hashes"),
        }

    def _metadata_list(self, row, key, fallback_key=None):
        metadata = row.get("metadata", {})
        if not isinstance(metadata, dict):
            metadata = {}

        value = metadata.get(key)
        if value is None and fallback_key:
            value = metadata.get(fallback_key)

        if isinstance(value, (list, tuple)):
            return list(value)
        if value is None:
            return []
        return [value]
