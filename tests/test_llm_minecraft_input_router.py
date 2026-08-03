#20260803_kpopmodder: Ensure direct Gradio chat input can be intercepted before LLM generation.
import unittest
from types import SimpleNamespace

from llm_core.llm_component import LLM


class LLMMinecraftInputRouterTests(unittest.TestCase):
    def test_predict_wrapper_returns_router_response_when_handled(self):
        llm = LLM.__new__(LLM)
        llm.input_router = _Router(
            SimpleNamespace(
                handled=True,
                reason="minecraft_command_routed",
                response_text="[Minecraft] command sent: get gold_ingot 1",
            )
        )

        output = list(llm.predict_wrapper("\uae08\uad34 1\uac1c \uad6c\ud574", [], ""))

        self.assertEqual(["[Minecraft] command sent: get gold_ingot 1"], output)

    def test_predict_wrapper_uses_normal_pipeline_when_not_handled(self):
        llm = LLM.__new__(LLM)
        llm.input_router = _Router(
            SimpleNamespace(
                handled=False,
                reason="no_minecraft_trigger",
                response_text="",
            )
        )
        llm.response_pipeline = _Pipeline()
        llm.build_effective_system_prompt = lambda prompt: f"effective:{prompt}"

        output = list(
            llm.predict_wrapper("\uc624\ub298 \ubb50 \uba39\uc9c0?", [], "system")
        )

        self.assertEqual(["llm response"], output)
        self.assertEqual(
            [("\uc624\ub298 \ubb50 \uba39\uc9c0?", [], "effective:system")],
            llm.response_pipeline.calls,
        )


class _Router:
    def __init__(self, decision):
        self.decision = decision

    def route(self, _message):
        return self.decision


class _Pipeline:
    def __init__(self):
        self.calls = []

    def predict(self, message, history, system_prompt):
        self.calls.append((message, history, system_prompt))
        yield "llm response"


if __name__ == "__main__":
    unittest.main()
