import json
import threading
import unittest

from memory_core.memory_context_builder import MemoryContextBuilder
from memory_core.memory_router import MemoryRouter


class FakeStore:
    def get_working_memory(self):
        return []

    def get_session_memory(self):
        return {}

    def get_long_term_memory(self):
        return {}


class FakeRetriever:
    def __init__(self):
        self.queries = []
        self.use_derived_fallback = False

    def retrieve(
        self,
        query,
        exclude_texts=None,
        use_derived_fallback_override=None,
        max_results_override=None,
    ):
        mode = (
            self.use_derived_fallback
            if use_derived_fallback_override is None
            else use_derived_fallback_override
        )#20260627_kpopmodder: Test fake follows request-scoped fallback overrides.
        self.queries.append((query, mode))
        return [{
            "text": f"memory result for {query}",
            "created_at": "2026-06-26 10:00:00",
            "recall_mode": (
                "derived_memory"
                if mode == "prefer"
                else "raw_recent"
            ),
        }]


class MemoryRouterTests(unittest.TestCase):
    def test_router_decision_logs_to_memory_core_logger(self):#20260627_kpopmodder
        router = MemoryRouter(provider="rule")

        with self.assertLogs("LAV.memory_core", level="INFO") as captured:
            router.route("hello")

        self.assertIn("[MemoryRouterDecision]", "\n".join(captured.output))
        self.assertTrue(all(
            record.name == "LAV.memory_core"
            for record in captured.records
        ))

    def test_default_provider_is_rule(self):#20260627_kpopmodder
        router = MemoryRouter()

        self.assertEqual("rule", router.provider)

    def test_provider_value_is_stripped_and_lowered(self):#20260627_kpopmodder
        router = MemoryRouter(provider=" OpenAI ")

        self.assertEqual("openai", router.provider)

    def test_info_log_does_not_include_user_input_or_queries(self):#20260627_kpopmodder
        router = MemoryRouter(provider="rule")

        with self.assertLogs("LAV.memory_core", level="INFO") as captured:
            router.route("private LAV memory detail")

        logs = "\n".join(captured.output)
        self.assertIn("[MemoryRouterDecision]", logs)
        self.assertIn("query_count=", logs)
        self.assertNotIn("private LAV memory detail", logs)
        self.assertNotIn("user_input=", logs)
        self.assertNotIn("queries=", logs)

    def test_general_question_returns_need_memory_false(self):
        router = MemoryRouter(provider="rule")

        decision = router.route("파이썬 리스트 정렬 방법 알려줘")

        self.assertFalse(decision.need_memory)
        self.assertEqual("none", decision.intent)
        self.assertEqual([], decision.queries)

    def test_previous_context_whisper_question_needs_memory(self):
        router = MemoryRouter(provider="rule")

        decision = router.route("아까 Whisper 마이크 설정 문제 다시 설명해줘")

        self.assertTrue(decision.need_memory)
        self.assertEqual("search", decision.intent)
        self.assertTrue(decision.queries)

    def test_watched_youtube_memory_typo_needs_memory(self):#20260627_kpopmodder
        router = MemoryRouter(provider="rule")

        decision = router.route("Youtube 봤던 거 거억하는 거 전부 알려줘")

        self.assertTrue(decision.need_memory)
        self.assertEqual("search", decision.intent)
        self.assertIn("activity_recall", decision.reason)
        self.assertIn("recall_typo", decision.reason)

    def test_seen_video_all_request_needs_memory(self):#20260627_kpopmodder
        router = MemoryRouter(provider="rule")

        decision = router.route("본 영상 전부 알려줘")

        self.assertTrue(decision.need_memory)
        self.assertEqual("search", decision.intent)

    def test_mixed_youtube_recall_save_wording_prefers_search(self):#20260629_kpopmodder
        router = MemoryRouter(provider="rule")

        decision = router.route("Youtube 에 대해서 기억나는 거 전부 기억해줘")

        self.assertTrue(decision.need_memory)
        self.assertEqual("search", decision.intent)
        self.assertIn("activity_recall", decision.reason)
        self.assertEqual(["Youtube 에 대해서 기억나는 거 전부 기억해줘"], decision.queries)

    def test_plain_remember_command_does_not_force_search(self):#20260629_kpopmodder
        router = MemoryRouter(provider="rule")

        decision = router.route("내가 파란색을 좋아한다고 기억해줘")

        self.assertFalse(decision.need_memory)
        self.assertEqual("none", decision.intent)
        self.assertEqual([], decision.queries)

    def test_youtube_how_to_question_stays_general(self):#20260627_kpopmodder
        router = MemoryRouter(provider="rule")

        decision = router.route("YouTube 사용법 알려줘")

        self.assertFalse(decision.need_memory)
        self.assertEqual("none", decision.intent)

    def test_lav_screenvision_project_question_needs_memory(self):
        router = MemoryRouter(provider="rule")

        decision = router.route("우리 LAV에서 ScreenVision 기억은 어디에 저장돼?")

        self.assertTrue(decision.need_memory)
        self.assertEqual("search", decision.intent)

    def test_invalid_ai_json_falls_back_safely(self):
        router = MemoryRouter(
            provider="auto",
            ai_response_callback=lambda system, user, timeout: "네 기억 필요합니다",
        )

        decision = router.route("아까 LAV 마이크 문제 다시 알려줘")

        self.assertTrue(decision.need_memory)
        self.assertTrue(decision.fallback_used)
        self.assertEqual("search", decision.intent)

    def test_ai_exception_falls_back_safely(self):
        def failing_ai(system, user, timeout):
            raise RuntimeError("router unavailable")

        router = MemoryRouter(provider="auto", ai_response_callback=failing_ai)

        decision = router.route("아까 LAV 마이크 문제 다시 알려줘")

        self.assertTrue(decision.need_memory)
        self.assertTrue(decision.fallback_used)

    def test_openai_provider_unavailable_is_cached_for_session(self):#20260627_kpopmodder
        calls = []

        def missing_key(system, user, timeout):
            calls.append(user)
            raise RuntimeError("OpenAI API key is not configured")

        router = MemoryRouter(provider="openai", ai_response_callback=missing_key)

        first = router.route("아까 LAV 마이크 문제 다시 알려줘")
        second = router.route("아까 LAV 마이크 문제 다시 알려줘")

        self.assertEqual(1, len(calls))
        self.assertTrue(first.need_memory)
        self.assertTrue(second.need_memory)
        self.assertTrue(second.fallback_used)
        self.assertIn("ai_router_unavailable_cached", second.reason)

    def test_invalid_ai_json_does_not_cache_provider_unavailable(self):#20260627_kpopmodder
        calls = []

        def invalid_json(system, user, timeout):
            calls.append(user)
            return "not json"

        router = MemoryRouter(provider="openai", ai_response_callback=invalid_json)

        router.route("아까 LAV 마이크 문제 다시 알려줘")
        router.route("아까 LAV 마이크 문제 다시 알려줘")

        self.assertEqual(2, len(calls))

    def test_default_timeout_is_five_seconds(self):
        router = MemoryRouter(provider="rule")

        self.assertEqual(5, router.timeout_sec)

    def test_timeout_fallback_searches_explicit_memory_question(self):
        def timing_out(system, user, timeout):
            raise TimeoutError("router timeout")

        router = MemoryRouter(provider="openai", ai_response_callback=timing_out)

        decision = router.route("Do you remember what Neuro sang?")

        self.assertTrue(decision.need_memory)
        self.assertTrue(decision.fallback_used)
        self.assertEqual("search", decision.intent)

    def test_need_memory_true_with_empty_queries_uses_user_input(self):
        router = MemoryRouter(provider="rule")
        response = json.dumps({
            "need_memory": True,
            "queries": [],
            "max_items": 5,
        })

        decision = router.parse_ai_response("사용자 입력 원문", response)

        self.assertEqual(["사용자 입력 원문"], decision.queries)
        self.assertEqual("search", decision.intent)

    def test_max_items_is_clamped(self):
        router = MemoryRouter(provider="rule", max_items=5)
        response = json.dumps({
            "need_memory": True,
            "queries": ["LAV memory"],
            "max_items": 999,
        })

        decision = router.parse_ai_response("LAV memory", response)

        self.assertEqual(5, decision.max_items)

    def test_parser_accepts_json_code_fence_and_extra_text(self):
        router = MemoryRouter(provider="rule")
        response = (
            "route:\n```json\n"
            '{"intent":"search","need_memory":true,'
            '"queries":["Whisper mic"],"max_items":3}'
            "\n```\n"
        )

        decision = router.parse_ai_response("Whisper mic", response)

        self.assertTrue(decision.need_memory)
        self.assertEqual(["Whisper mic"], decision.queries)

    def test_auto_provider_uses_ai_json_when_valid(self):
        router = MemoryRouter(
            provider="auto",
            ai_response_callback=lambda system, user, timeout: json.dumps({
                "intent": "search",
                "need_memory": True,
                "reason": "ai_route",
                "queries": ["AI selected query"],
                "memory_scope": ["derived"],
                "max_items": 2,
            }),
        )

        decision = router.route("이전 문제 다시 알려줘")

        self.assertTrue(decision.need_memory)
        self.assertFalse(decision.fallback_used)
        self.assertEqual(["AI selected query"], decision.queries)
        self.assertEqual(["derived"], decision.memory_scope)

    def test_ai_skip_is_overridden_for_explicit_memory_question(self):
        router = MemoryRouter(
            provider="openai",
            ai_response_callback=lambda system, user, timeout: json.dumps({
                "intent": "none",
                "need_memory": False,
                "reason": "general question",
                "queries": [],
            }),
        )

        decision = router.route("neuro가 부른 노래 제목 알려줘")

        self.assertTrue(decision.need_memory)
        self.assertTrue(decision.fallback_used)
        self.assertEqual("search", decision.intent)
        self.assertEqual(["neuro가 부른 노래 제목 알려줘"], decision.queries)
        self.assertIn("ai_router_no_memory_override", decision.reason)

    def test_ai_save_decision_is_overridden_for_mixed_activity_recall(self):#20260629_kpopmodder
        router = MemoryRouter(
            provider="auto",
            ai_response_callback=lambda system, user, timeout: json.dumps({
                "intent": "save",
                "need_memory": False,
                "queries": [],
                "max_items": 0,
            }),
        )

        decision = router.route("Youtube 에 대해서 기억나는 거 전부 기억해줘")

        self.assertTrue(decision.need_memory)
        self.assertTrue(decision.fallback_used)
        self.assertEqual("search", decision.intent)
        self.assertIn("activity_recall", decision.reason)

    def test_openai_provider_does_not_use_current_llm_callback(self):
        router = MemoryRouter(
            provider="openai",
            ai_response_callback=lambda system, user, timeout: json.dumps({
                "intent": "search",
                "need_memory": True,
                "queries": ["OpenAI router query"],
                "max_items": 2,
            }),
        )
        router.set_current_llm_response_callback(
            lambda system, user, timeout: json.dumps({
                "intent": "search",
                "need_memory": True,
                "queries": ["Transformers router query"],
                "max_items": 2,
            })
        )

        decision = router.route("아까 Neuro 기억 알려줘")

        self.assertEqual(["OpenAI router query"], decision.queries)

    def test_current_llm_provider_uses_current_llm_callback(self):
        router = MemoryRouter(
            provider="current_llm",
            ai_response_callback=lambda system, user, timeout: json.dumps({
                "intent": "search",
                "need_memory": True,
                "queries": ["OpenAI router query"],
                "max_items": 2,
            }),
        )
        router.set_current_llm_response_callback(
            lambda system, user, timeout: json.dumps({
                "intent": "search",
                "need_memory": True,
                "queries": ["Current LLM router query"],
                "max_items": 2,
            })
        )

        decision = router.route("아까 Neuro 기억 알려줘")

        self.assertEqual(["Current LLM router query"], decision.queries)


class MemoryContextBuilderRouterTests(unittest.TestCase):
    def test_builder_skips_recalled_search_when_router_says_no(self):
        retriever = FakeRetriever()
        router = MemoryRouter(provider="rule")
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            memory_router=router,
        )

        context = builder.build_context_text(query="파이썬 리스트 정렬 방법 알려줘")

        self.assertEqual([], retriever.queries)
        self.assertEqual("", context)

    def test_builder_ignores_plain_remember_command_without_search(self):#20260629_kpopmodder
        retriever = FakeRetriever()
        router = MemoryRouter(provider="rule")
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            memory_router=router,
        )

        context = builder.build_context_text(
            query="내가 파란색을 좋아한다고 기억해줘",
        )

        self.assertEqual([], retriever.queries)
        self.assertEqual("", context)

    def test_builder_retrieves_for_mixed_recall_save_wording(self):#20260629_kpopmodder
        retriever = FakeRetriever()
        router = MemoryRouter(provider="rule")
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            memory_router=router,
        )

        context = builder.build_context_text(
            query="Youtube 에 대해서 기억나는 거 전부 기억해줘",
        )

        self.assertEqual(
            [("Youtube 에 대해서 기억나는 거 전부 기억해줘", False)],
            retriever.queries,
        )
        self.assertIn("[Relevant recalled memory]", context)
        self.assertIn(
            "memory result for Youtube 에 대해서 기억나는 거 전부 기억해줘",
            context,
        )
        self.assertNotIn("[Memory save request]", context)

    def test_builder_uses_router_query_when_memory_needed(self):
        retriever = FakeRetriever()
        router = MemoryRouter(
            provider="auto",
            ai_response_callback=lambda system, user, timeout: json.dumps({
                "intent": "search",
                "need_memory": True,
                "queries": ["Whisper microphone setting"],
                "max_items": 1,
            }),
        )
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            memory_router=router,
        )

        context = builder.build_context_text(query="아까 Whisper 문제 다시 알려줘")

        self.assertEqual(
            [("Whisper microphone setting", False)],
            retriever.queries,
        )
        self.assertIn("memory result for Whisper microphone setting", context)

    def test_builder_preserves_original_query_for_deep_recall(self):#20260626_kpopmodder
        retriever = FakeRetriever()
        router = MemoryRouter(
            provider="auto",
            ai_response_callback=lambda system, user, timeout: json.dumps({
                "intent": "search",
                "need_memory": True,
                "queries": ["Neuro song"],
                "max_items": 1,
            }),
        )
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            memory_router=router,
            max_recalled_items=1,
            max_deep_recalled_items=4,
        )

        context = builder.build_context_text(
            query="Neuro song all memories",
        )

        self.assertEqual(
            [
                ("Neuro song all memories", False),
                ("Neuro song", False),
            ],
            retriever.queries,
        )
        self.assertIn("memory result for Neuro song all memories", context)
        self.assertIn("memory result for Neuro song", context)

    def test_builder_uses_deep_limit_for_deep_topic_recall(self):#20260720_kpopmodder
        class WideRecallRetriever:
            def __init__(self):
                self.calls = []

            def retrieve(
                self,
                query,
                exclude_texts=None,
                use_derived_fallback_override=None,
                max_results_override=None,
            ):
                self.calls.append((query, max_results_override))
                limit = max(1, int(max_results_override or 1))
                return [
                    {
                        "text": f"topic memory {index}",
                        "created_at": "2026-06-27 10:00:00",
                        "recall_mode": "raw_long_search",
                    }
                    for index in range(limit)
                ]

        retriever = WideRecallRetriever()
        router = MemoryRouter(
            provider="auto",
            ai_response_callback=lambda system, user, timeout: json.dumps({
                "intent": "search",
                "need_memory": True,
                "queries": ["\ubd95\uad343rd"],
                "max_items": 1,
            }),
        )
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            memory_router=router,
            max_recalled_items=1,
            max_deep_recalled_items=4,
        )

        context = builder.build_context_text(
            query="\ubd95\uad343rd all memories",
        )

        self.assertEqual(
            [
                (
                    "\ubd95\uad343rd all memories",
                    4,
                )
            ],
            retriever.calls,
        )
        self.assertIn("Deep recall request", context)
        self.assertIn("topic memory 3", context)

    def test_builder_keeps_simple_recall_recent_only(self):#20260720_kpopmodder
        class PolicyRetriever:
            def __init__(self):
                self.calls = []

            def retrieve(
                self,
                query,
                exclude_texts=None,
                use_derived_fallback_override=None,
                max_results_override=None,
                allow_deep_raw_search=None,
                include_screen_observations=None,
            ):
                self.calls.append({
                    "query": query,
                    "max_results": max_results_override,
                    "allow_deep": allow_deep_raw_search,
                    "include_screen": include_screen_observations,
                })
                return [{
                    "text": f"memory result for {query}",
                    "created_at": "2026-07-20 00:00:00",
                    "recall_mode": "raw_recent",
                }]

        retriever = PolicyRetriever()
        router = MemoryRouter(
            provider="auto",
            ai_response_callback=lambda system, user, timeout: json.dumps({
                "intent": "search",
                "need_memory": True,
                "queries": ["StarCraft topic"],
                "max_items": 2,
            }),
        )
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            memory_router=router,
            max_recalled_items=2,
            max_deep_recalled_items=6,
        )

        context = builder.build_context_text(query="StarCraft remember?")

        self.assertEqual([
            {
                "query": "StarCraft topic",
                "max_results": 2,
                "allow_deep": False,
                "include_screen": False,
            }
        ], retriever.calls)
        self.assertIn("[Relevant recalled memory]", context)
        self.assertNotIn("Deep recall request", context)

    def test_builder_allows_screen_recall_only_for_screen_query(self):#20260720_kpopmodder
        class PolicyRetriever:
            def __init__(self):
                self.calls = []

            def retrieve(
                self,
                query,
                exclude_texts=None,
                use_derived_fallback_override=None,
                max_results_override=None,
                allow_deep_raw_search=None,
                include_screen_observations=None,
            ):
                self.calls.append({
                    "query": query,
                    "allow_deep": allow_deep_raw_search,
                    "include_screen": include_screen_observations,
                })
                return [{
                    "text": f"screen memory result for {query}",
                    "created_at": "2026-07-20 00:00:00",
                    "recall_mode": "raw_recent",
                }]

        retriever = PolicyRetriever()
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            memory_router=MemoryRouter(provider="rule"),
        )

        context = builder.build_context_text(
            query="Youtube watched video remember?",
        )

        self.assertTrue(retriever.calls)
        self.assertTrue(retriever.calls[0]["include_screen"])
        self.assertIn("[Relevant recalled memory]", context)

    def test_builder_skips_raw_recall_when_short_term_is_confident(self):#20260720_kpopmodder
        class ConfidentShortTermSelector:
            def select(self, query=None, active_history=None):
                return [{
                    "score": 0.55,
                    "user_text": "GPU power limit was lowered.",
                    "assistant_text": "Performance drops a little, heat drops more.",
                    "context_text": (
                        "User: GPU power limit was lowered.\n"
                        "Assistant: Performance drops a little, heat drops more."
                    ),
                }]

            def format_selected(self, items):
                return (
                    "[Related recent conversation]\n"
                    "- User: GPU power limit was lowered.\n"
                    "- Assistant: Performance drops a little, heat drops more."
                )

        class TrackingRetriever:
            def __init__(self):
                self.calls = []

            def retrieve(self, *args, **kwargs):
                self.calls.append((args, kwargs))
                return [{
                    "text": "stale raw memory that should not be injected",
                    "created_at": "2026-06-01 00:00:00",
                }]

        retriever = TrackingRetriever()
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            short_term_memory_selector=ConfidentShortTermSelector(),
        )

        context = builder.build_context_text(
            query="Does that hurt GPU performance a lot?",
        )

        self.assertEqual([], retriever.calls)
        self.assertIn("[Related recent conversation]", context)
        self.assertNotIn("stale raw memory", context)

    def test_builder_falls_back_to_existing_retriever_on_router_exception(self):
        class FailingRouter:
            def route(self, query):
                raise RuntimeError("broken")

        retriever = FakeRetriever()
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            memory_router=FailingRouter(),
        )

        context = builder.build_context_text(query="아까 LAV 문제 다시 알려줘")

        self.assertEqual([("아까 LAV 문제 다시 알려줘", False)], retriever.queries)
        self.assertIn("memory result for 아까 LAV 문제 다시 알려줘", context)

    def test_builder_can_prefer_derived_first(self):
        retriever = FakeRetriever()
        router = MemoryRouter(provider="rule")
        builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            memory_router=router,
            prefer_derived_first=True,
        )

        context = builder.build_context_text(query="우리 LAV ScreenVision 기억")

        self.assertEqual([("우리 LAV ScreenVision 기억", "prefer")], retriever.queries)
        self.assertIn("memory result for 우리 LAV ScreenVision 기억", context)

    def test_prefer_derived_first_does_not_pollute_concurrent_retriever_mode(self):#20260627_kpopmodder
        class ConcurrentRetriever:
            def __init__(self):
                self.use_derived_fallback = False
                self.queries = []
                self.lock = threading.Lock()
                self.prefer_entered = threading.Event()
                self.release_prefer = threading.Event()

            def retrieve(
                self,
                query,
                exclude_texts=None,
                use_derived_fallback_override=None,
                max_results_override=None,
            ):
                mode = (
                    self.use_derived_fallback
                    if use_derived_fallback_override is None
                    else use_derived_fallback_override
                )
                if query == "prefer request":
                    self.prefer_entered.set()
                    self.release_prefer.wait(timeout=2.0)
                with self.lock:
                    self.queries.append(
                        (query, mode, self.use_derived_fallback)
                    )
                return [{
                    "text": f"memory result for {query}",
                    "created_at": "2026-06-27 10:00:00",
                    "recall_mode": (
                        "derived_memory"
                        if mode == "prefer"
                        else "raw_recent"
                    ),
                }]

        retriever = ConcurrentRetriever()
        prefer_builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            prefer_derived_first=True,
        )
        raw_builder = MemoryContextBuilder(
            FakeStore(),
            memory_retriever=retriever,
            prefer_derived_first=False,
        )
        errors = []

        def run_prefer():
            try:
                prefer_builder.build_context_text(query="prefer request")
            except Exception as exc:
                errors.append(exc)

        thread = threading.Thread(target=run_prefer)
        thread.start()
        self.assertTrue(retriever.prefer_entered.wait(timeout=2.0))

        raw_context = raw_builder.build_context_text(query="raw request")

        retriever.release_prefer.set()
        thread.join(timeout=2.0)

        self.assertFalse(thread.is_alive())
        self.assertEqual([], errors)
        self.assertIn("memory result for raw request", raw_context)
        self.assertFalse(retriever.use_derived_fallback)
        query_modes = {
            query: (mode, shared_mode)
            for query, mode, shared_mode in retriever.queries
        }
        self.assertEqual(("prefer", False), query_modes["prefer request"])
        self.assertEqual((False, False), query_modes["raw request"])


if __name__ == "__main__":
    unittest.main()
