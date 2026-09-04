#20260905_kpopmodder: Verifies the local Chat event-to-LLM streaming entrypoint.
import inspect
import unittest

from input_core.input_event.adapters import LocalChatInputEventAdapter
from llm_core.chat_input import LocalChatPredictionEntrypoint
from llm_core.llm_component import LLM


class LocalChatPredictionEntrypointTests(unittest.TestCase):
    def test_llm_handled_route_streams_ack_without_calling_normal_pipeline(self):
        llm = LLM.__new__(LLM)
        llm.input_router = _RecordingRouter(handled=True, response_text="accepted")
        llm.response_pipeline = _FailIfCalledPipeline()
        entrypoint = self._entrypoint(llm.predict_wrapper)

        output = list(entrypoint.predict("command", [], "system"))

        self.assertEqual(["accepted"], output)
        self.assertEqual(1, len(llm.input_router.received))
        self.assertEqual("lavi_chat_ui", llm.input_router.received[0].source)
        self.assertEqual("d" * 32, llm.input_router.received[0].event_id)

    def test_llm_unhandled_route_preserves_payload_and_streaming_order(self):
        payload = "ordinary chat"
        history = [{"role": "assistant", "content": "before"}]
        llm = LLM.__new__(LLM)
        llm.input_router = _RecordingRouter(handled=False, response_text="")
        llm.response_pipeline = _RecordingPipeline()
        llm.build_effective_system_prompt = lambda value: f"effective:{value}"
        entrypoint = self._entrypoint(llm.predict_wrapper)

        output = list(entrypoint.predict(payload, history, "system"))

        self.assertEqual(["partial", "complete"], output)
        self.assertEqual(1, len(llm.input_router.received))
        self.assertIs(payload, llm.response_pipeline.message)
        self.assertIs(history, llm.response_pipeline.history)
        self.assertEqual("effective:system", llm.response_pipeline.system_prompt)

    def test_llm_lazy_composition_exposes_bound_generator_method(self):
        llm = LLM.__new__(LLM)

        entrypoint = llm._get_local_chat_prediction_entrypoint()
        interface_factory = llm._get_local_chat_interface_factory()

        self.assertIs(entrypoint, llm.local_chat_prediction_entrypoint)
        self.assertIs(
            llm.local_chat_input_adapter,
            entrypoint._input_event_adapter,
        )
        self.assertIs(llm, entrypoint._predict_callback.__self__)
        self.assertIs(entrypoint, interface_factory._prediction_callback.__self__)
        self.assertTrue(inspect.isgeneratorfunction(entrypoint.predict))

    def test_entrypoint_rejects_invalid_dependencies_at_composition_boundary(self):
        cases = (
            (
                object(),
                lambda _event, _history, _prompt: iter(("ok",)),
                "input_event_adapter.adapt must be callable",
            ),
            (
                LocalChatInputEventAdapter(),
                None,
                "predict_callback must be callable",
            ),
        )

        for input_event_adapter, predict_callback, expected_message in cases:
            with self.subTest(expected_message=expected_message):
                with self.assertRaisesRegex(TypeError, expected_message):
                    LocalChatPredictionEntrypoint(
                        input_event_adapter=input_event_adapter,
                        predict_callback=predict_callback,
                    )

    def test_closing_outer_stream_closes_prediction_generator(self):
        closed = []

        def predict(_event, _history, _system_prompt):
            try:
                yield "partial"
                yield "complete"
            finally:
                closed.append(True)

        stream = self._entrypoint(predict).predict("hello", [], "system")

        self.assertEqual("partial", next(stream))
        stream.close()

        self.assertEqual([True], closed)

    def _entrypoint(self, predict_callback):
        return LocalChatPredictionEntrypoint(
            input_event_adapter=LocalChatInputEventAdapter(
                event_id_factory=lambda: "d" * 32,
            ),
            predict_callback=predict_callback,
        )


class _RecordingRouter:
    def __init__(self, *, handled, response_text):
        self._handled = handled
        self._response_text = response_text
        self.received = []

    def route(self, event):
        self.received.append(event)
        return _RouteDecision(
            handled=self._handled,
            response_text=self._response_text,
        )


class _RouteDecision:
    def __init__(self, *, handled, response_text):
        self.handled = handled
        self.reason = "test"
        self.response_text = response_text


class _FailIfCalledPipeline:
    def predict(self, _message, _history, _system_prompt):
        raise AssertionError("normal pipeline must not run for a handled route")


class _RecordingPipeline:
    def predict(self, message, history, system_prompt):
        self.message = message
        self.history = history
        self.system_prompt = system_prompt
        yield "partial"
        yield "complete"


__all__ = ["LocalChatPredictionEntrypointTests"]


if __name__ == "__main__":
    unittest.main()
