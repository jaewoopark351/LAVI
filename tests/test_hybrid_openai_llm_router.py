import unittest
from dataclasses import dataclass
from types import SimpleNamespace

from plugins.Hybrid_OpenAI_LLM.hybrid_openai_core.engine import (
    RouterFirstHybridEngine,
)
from plugins.Hybrid_OpenAI_LLM.hybrid_openai_core.routing import (
    CommandOverrideRouter,
    OpenAIRouteProvider,
    RouteDecision,
)


@dataclass
class FakeMemoryDecision:
    need_memory: bool = False


class FakeRouteProvider:
    def __init__(self, route="local_light"):
        self.route_value = route
        self.called = False

    def route(self, message, history=None, system_prompt=""):
        self.called = True
        return RouteDecision(route=self.route_value, reason="fake_route")


class FakeMemoryRouterProvider:
    def __init__(self, need_memory=False):
        self.need_memory = need_memory
        self.called = False

    def route(self, message):
        self.called = True
        return FakeMemoryDecision(need_memory=self.need_memory)


class FakeChatProvider:
    def __init__(self, name, fail=False, enabled=True):
        self.name = name
        self.fail = fail
        self.enabled = enabled
        self.called = False

    def stream(self, message, history, system_prompt):
        self.called = True
        if self.fail:
            raise RuntimeError(f"{self.name} failure")
        yield f"{self.name}:{message}"


class HybridOpenAILLMRouterTests(unittest.TestCase):
    def build_engine(
        self,
        route="local_light",
        need_memory=False,
        openai_fail=False,
        local_enabled=False,
    ):
        self.logs = []
        self.route_provider = FakeRouteProvider(route=route)
        self.memory_provider = FakeMemoryRouterProvider(need_memory=need_memory)
        self.openai_provider = FakeChatProvider("openai", fail=openai_fail)
        self.local_provider = FakeChatProvider("local", enabled=local_enabled)
        return RouterFirstHybridEngine(
            command_router=CommandOverrideRouter(),
            route_provider=self.route_provider,
            memory_router_provider=self.memory_provider,
            openai_provider=self.openai_provider,
            local_provider=self.local_provider,
            log_print=self.logs.append,
        )

    def collect(self, engine, message):
        return list(engine.stream(message, [], ""))

    def test_openai_command_skips_local_provider(self):
        engine = self.build_engine(route="local_light")
        message = CommandOverrideRouter.COMMAND_TERMS[0] + " explain this"

        output = self.collect(engine, message)

        self.assertEqual([f"openai:{message}"], output)
        self.assertTrue(self.openai_provider.called)
        self.assertFalse(self.local_provider.called)
        self.assertFalse(self.memory_provider.called)
        self.assertFalse(self.route_provider.called)

    def test_gpt_command_forces_openai(self):
        engine = self.build_engine(route="local_light")
        gpt_term = next(
            term for term in CommandOverrideRouter.COMMAND_TERMS
            if "gpt" in term.lower()
        )
        message = gpt_term + " current state?"

        output = self.collect(engine, message)

        self.assertEqual([f"openai:{message}"], output)
        self.assertTrue(self.openai_provider.called)
        self.assertFalse(self.local_provider.called)
        self.assertFalse(self.memory_provider.called)
        self.assertFalse(self.route_provider.called)

    def test_simple_chat_uses_openai_when_local_disabled(self):
        engine = self.build_engine(route="local_light", local_enabled=False)

        output = self.collect(engine, "hello")

        self.assertEqual(["openai:hello"], output)
        self.assertTrue(self.openai_provider.called)
        self.assertFalse(self.local_provider.called)
        self.assertFalse(self.memory_provider.called)
        self.assertFalse(self.route_provider.called)
        self.assertIn("openai_only_local_disabled", self.logs[0])

    def test_local_enabled_can_still_route_local_light(self):
        engine = self.build_engine(route="local_light", local_enabled=True)

        output = self.collect(engine, "hello")

        self.assertEqual(["local:hello"], output)
        self.assertFalse(self.openai_provider.called)
        self.assertTrue(self.local_provider.called)
        self.assertTrue(self.memory_provider.called)
        self.assertTrue(self.route_provider.called)

    def test_debugging_routes_openai_chat_when_local_enabled(self):
        engine = self.build_engine(route="openai_chat", local_enabled=True)

        output = self.collect(engine, "debug this code")

        self.assertEqual(["openai:debug this code"], output)
        self.assertTrue(self.openai_provider.called)
        self.assertFalse(self.local_provider.called)
        self.assertTrue(self.memory_provider.called)
        self.assertTrue(self.route_provider.called)

    def test_memory_question_forces_openai_chat(self):
        engine = self.build_engine(
            route="local_light",
            need_memory=True,
            local_enabled=True,
        )

        output = self.collect(engine, "remember Neuro?")

        self.assertEqual(["openai:remember Neuro?"], output)
        self.assertTrue(self.openai_provider.called)
        self.assertFalse(self.local_provider.called)
        self.assertTrue(self.memory_provider.called)
        self.assertFalse(self.route_provider.called)

    def test_openai_failure_does_not_fallback_when_local_disabled(self):
        engine = self.build_engine(route="openai_chat", openai_fail=True)

        with self.assertRaises(RuntimeError):
            self.collect(engine, "debug this code")

        self.assertTrue(self.openai_provider.called)
        self.assertFalse(self.local_provider.called)
        self.assertTrue(
            any("local_light disabled" in log for log in self.logs)
        )

    def test_route_provider_fallback_allows_simple_chat_only(self):
        logs = []
        settings = SimpleNamespace(
            route_model_name="gpt-4o-mini",
            route_temperature=0.0,
            route_timeout_sec=5,
            openai_api_key="",
        )
        provider = OpenAIRouteProvider(settings, logs.append)

        decision = provider.rule_fallback("hello")

        self.assertEqual("local_light", decision.route)

    def test_route_provider_fallback_sends_debugging_to_openai(self):
        logs = []
        settings = SimpleNamespace(
            route_model_name="gpt-4o-mini",
            route_temperature=0.0,
            route_timeout_sec=5,
            openai_api_key="",
        )
        provider = OpenAIRouteProvider(settings, logs.append)

        decision = provider.rule_fallback("debug this code")

        self.assertEqual("openai_chat", decision.route)


if __name__ == "__main__":
    unittest.main()
