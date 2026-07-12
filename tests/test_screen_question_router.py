#20260628_kpopmodder: Regression coverage for AI/rule screen-question routing.
import ast
import json
import unittest
from pathlib import Path

from llm_core.interaction_context import LLMInteractionContext
from llm_core.screen_question_router import (
    ScreenQuestionDecision,
    ScreenQuestionRouter,
)


class FakeScreenRouterProvider:
    def __init__(self, response):
        self.response = response
        self.calls = []

    def __call__(self, system_prompt, user_input, timeout_sec=None):
        self.calls.append(
            {
                "system_prompt": system_prompt,
                "user_input": user_input,
                "timeout_sec": timeout_sec,
            }
        )
        return self.response


class ScreenQuestionRouterTests(unittest.TestCase):
    def test_rule_router_detects_screen_question_when_observation_exists(self):
        router = ScreenQuestionRouter(provider="rule")

        decision = router.route(
            "지금 화면에 뭐 떠 있어?",
            has_latest_screen_observation=True,
        )

        self.assertTrue(decision.need_screen)
        self.assertEqual("screen_question", decision.intent)

    def test_rule_router_does_not_request_empty_screen_context(self):
        router = ScreenQuestionRouter(provider="rule")

        decision = router.route(
            "지금 화면에 뭐 떠 있어?",
            has_latest_screen_observation=False,
        )

        self.assertFalse(decision.need_screen)
        self.assertEqual("rule_no_screen_observation", decision.reason)

    def test_openai_router_receives_user_text_without_screen_observation(self):
        provider = FakeScreenRouterProvider(
            json.dumps(
                {
                    "intent": "screen_question",
                    "need_screen": True,
                    "reason": "asks_current_screen",
                    "confidence": 0.9,
                }
            )
        )
        router = ScreenQuestionRouter(
            provider="openai",
            timeout_sec=2,
            ai_response_callback=provider,
        )

        decision = router.route(
            "현재 화면 설명해줘",
            has_latest_screen_observation=True,
        )

        self.assertTrue(decision.need_screen)
        self.assertEqual(1, len(provider.calls))
        payload = json.loads(provider.calls[0]["user_input"])
        self.assertEqual("현재 화면 설명해줘", payload["user_input"])
        self.assertTrue(payload["has_latest_screen_observation"])
        self.assertNotIn("SECRET_SCREEN_TEXT", provider.calls[0]["user_input"])
        self.assertEqual(2, provider.calls[0]["timeout_sec"])

    def test_openai_false_decision_is_not_overridden_by_keyword_match(self):
        provider = FakeScreenRouterProvider(
            json.dumps(
                {
                    "intent": "none",
                    "need_screen": False,
                    "reason": "asks_memory_not_screen",
                    "confidence": 0.8,
                }
            )
        )
        router = ScreenQuestionRouter(
            provider="openai",
            ai_response_callback=provider,
        )

        decision = router.route(
            "화면 말고 기억 검색해줘",
            has_latest_screen_observation=True,
        )

        self.assertFalse(decision.need_screen)
        self.assertEqual("asks_memory_not_screen", decision.reason)
        self.assertFalse(decision.fallback_used)

    def test_openai_invalid_json_falls_back_to_keyword_router(self):
        provider = FakeScreenRouterProvider("not json")
        router = ScreenQuestionRouter(
            provider="openai",
            fallback_to_keyword=True,
            ai_response_callback=provider,
        )

        decision = router.route(
            "지금 화면 뭐야?",
            has_latest_screen_observation=True,
        )

        self.assertTrue(decision.need_screen)
        self.assertTrue(decision.fallback_used)
        self.assertEqual("ai_router_failed_screen_question", decision.reason)

    def test_ai_true_does_not_request_missing_screen_observation(self):
        provider = FakeScreenRouterProvider(
            json.dumps(
                {
                    "intent": "screen_question",
                    "need_screen": True,
                    "reason": "asks_current_screen",
                    "confidence": 0.9,
                }
            )
        )
        router = ScreenQuestionRouter(
            provider="openai",
            ai_response_callback=provider,
        )

        decision = router.route(
            "현재 화면 설명해줘",
            has_latest_screen_observation=False,
        )

        self.assertFalse(decision.need_screen)
        self.assertEqual("rule_no_screen_observation", decision.reason)
        self.assertEqual(0, len(provider.calls))


class ScreenQuestionInteractionContextTests(unittest.TestCase):
    def test_router_decision_controls_screen_context_injection(self):
        context = LLMInteractionContext()
        context.add_screen_observation(
            "SECRET_SCREEN_TEXT: Visual Studio Code is visible.",
            source="test",
        )
        normalized = context.normalize_input("현재 화면 뭐야?")

        model_input = context.build_model_input(
            normalized,
            screen_question_decision=ScreenQuestionDecision(
                intent="none",
                need_screen=False,
                reason="ai_no_screen_question",
            ),
        )
        self.assertEqual("현재 화면 뭐야?", model_input)

        model_input = context.build_model_input(
            normalized,
            screen_question_decision=ScreenQuestionDecision(
                intent="screen_question",
                need_screen=True,
                reason="ai_screen_question",
            ),
        )
        self.assertIn("[최근 화면 관찰 기록]", model_input)
        self.assertIn("SECRET_SCREEN_TEXT", model_input)

    def test_legacy_keyword_path_still_works_without_router_decision(self):
        context = LLMInteractionContext()
        context.add_screen_observation(
            "SECRET_SCREEN_TEXT: Visual Studio Code is visible.",
            source="test",
        )
        normalized = context.normalize_input("현재 화면 뭐야?")

        model_input = context.build_model_input(normalized)

        self.assertIn("[최근 화면 관찰 기록]", model_input)
        self.assertIn("SECRET_SCREEN_TEXT", model_input)


class ScreenQuestionMainWiringTests(unittest.TestCase):
    def test_screen_question_bootstrap_defaults_to_rule_and_openai_is_opt_in(self):
        project_root = Path(__file__).resolve().parents[1]
        bootstrap_path = project_root / "app_core" / "screen_router_bootstrap.py"
        bootstrap_module = ast.parse(bootstrap_path.read_text(encoding="utf-8"))
        config_text = (project_root / "config.ini.example").read_text(
            encoding="utf-8",
        )

        def find_assignment(module, name):
            for node in ast.walk(module):
                if not isinstance(node, ast.Assign):
                    continue
                for target in node.targets:
                    if getattr(target, "id", "") == name:
                        return node.value
            return None

        def constant_values(node):
            if not isinstance(node, ast.Call):
                return []
            return [
                arg.value
                for arg in node.args
                if isinstance(arg, ast.Constant)
            ]

        provider_assignment = find_assignment(
            bootstrap_module,
            "screen_question_router_provider",
        )
        self.assertIn("provider", constant_values(provider_assignment))
        self.assertIn(
            "screen_question_router_provider",
            constant_values(provider_assignment),
        )
        self.assertIn("rule", constant_values(provider_assignment))

        openai_default = find_assignment(
            bootstrap_module,
            "openai_screen_question_router_provider",
        )
        self.assertIsInstance(openai_default, ast.Constant)
        self.assertIsNone(openai_default.value)

        screen_router_calls = [
            node
            for node in ast.walk(bootstrap_module)
            if isinstance(node, ast.Call)
            and getattr(node.func, "id", "") == "ScreenQuestionRouter"
        ]
        self.assertTrue(screen_router_calls)
        provider_keywords = [
            getattr(keyword.value, "id", "")
            for call in screen_router_calls
            for keyword in call.keywords
            if keyword.arg == "provider"
        ]
        self.assertIn("screen_question_router_provider", provider_keywords)

        self.assertIn("[ScreenQuestionRouter]", config_text)
        self.assertIn("provider = rule", config_text)
        self.assertIn("fallback_to_keyword = true", config_text)

    def test_main_connects_screen_question_router_bootstrap_to_llm(self):
        #20260630_kpopmodder: main.py now consumes the app_core screen router bootstrap.
        project_root = Path(__file__).resolve().parents[1]
        main_path = project_root / "main.py"
        module = ast.parse(main_path.read_text(encoding="utf-8"))

        screen_router_bootstrap_calls = [
            node
            for node in ast.walk(module)
            if isinstance(node, ast.Call)
            and getattr(node.func, "id", "") == "build_screen_question_router"
        ]
        self.assertTrue(screen_router_bootstrap_calls)

        llm_calls = [
            node
            for node in ast.walk(module)
            if isinstance(node, ast.Call)
            and getattr(node.func, "id", "") == "LLM"
        ]
        self.assertTrue(llm_calls)
        llm_router_keywords = [
            getattr(keyword.value, "id", "")
            for call in llm_calls
            for keyword in call.keywords
            if keyword.arg == "screen_question_router"
        ]
        self.assertIn("screen_question_router", llm_router_keywords)
