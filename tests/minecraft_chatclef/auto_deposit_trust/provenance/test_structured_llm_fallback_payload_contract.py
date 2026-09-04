#20260905_kpopmodder: Verifies non-routed structured input reaches the existing LLM pipeline unchanged.
import unittest
from types import SimpleNamespace

from input_core.input_event.adapters import DirectCallbackInputEventAdapter
from llm_core.llm_component import LLM


class StructuredLlmFallbackPayloadContractTests(unittest.TestCase):
    def test_predict_wrapper_passes_exact_screen_vision_object_to_normal_pipeline(self):
        payload = {
            "kind": "screen_observation",
            "source": "screen_vision_auto_watch",
            "observation": "a chest",
            "text": "describe",
            "display_text": "automatic observation",
            "remember_history": False,
            "metadata": {"nested": [1, 2, 3]},
            "payload": {"opaque": object()},
        }
        events = []
        DirectCallbackInputEventAdapter(
            output_callback=events.append,
            source="screen_vision",
            provider_id="ScreenVision",
            event_kind="screen_observation",
            event_id_factory=lambda: "f" * 32,
        )(payload)
        llm = LLM.__new__(LLM)
        llm.input_router = _NotHandledRouter()
        llm.response_pipeline = _RecordingPipeline()
        llm.build_effective_system_prompt = lambda value: value

        output = list(llm.predict_wrapper(events[0], [], "system"))

        self.assertEqual(["normal response"], output)
        self.assertIs(events[0], llm.input_router.received)
        self.assertIs(payload, llm.response_pipeline.received)


class _NotHandledRouter:
    def route(self, event):
        self.received = event
        return SimpleNamespace(handled=False, reason="none", response_text="")


class _RecordingPipeline:
    def predict(self, message, _history, _system_prompt):
        self.received = message
        yield "normal response"


__all__ = ["StructuredLlmFallbackPayloadContractTests"]


if __name__ == "__main__":
    unittest.main()
