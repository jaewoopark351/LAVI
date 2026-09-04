#20260905_kpopmodder: Verifies the exact Gradio registration and streaming path for local Chat.
import asyncio
import inspect
import unittest

import gradio as gr

from input_core.input_event.adapters import LocalChatInputEventAdapter
from llm_core.chat_input import (
    LocalChatInterfaceFactory,
    LocalChatPredictionEntrypoint,
)


class LocalChatInterfaceFactoryTests(unittest.TestCase):
    def test_factory_registers_bound_generator_method(self):
        entrypoint = self._entrypoint(
            lambda _event, _history, _prompt: iter(("ok",))
        )

        async def assert_gradio_contract():
            chat = self._chat_interface(entrypoint)
            self.assertTrue(chat.is_generator)
            self.assertIs(entrypoint, chat.fn.__self__)

        self.assertTrue(inspect.isgeneratorfunction(entrypoint.predict))
        asyncio.run(assert_gradio_contract())

    def test_gradio_stream_path_emits_every_chunk_and_builds_one_event(self):
        events = []
        callback_calls = []

        def predict(event, history, system_prompt):
            events.append(event)
            callback_calls.append((history, system_prompt))
            yield "first"
            yield "second"

        entrypoint = self._entrypoint(predict)

        async def collect_stream():
            chat = self._chat_interface(entrypoint)
            return [
                output
                async for output in chat._stream_fn("hello", [], "system")
            ]

        outputs = asyncio.run(collect_stream())

        self.assertEqual(["first", "second"], [output[0] for output in outputs])
        self.assertEqual(1, len(events))
        self.assertEqual("hello", events[0].text)
        self.assertEqual("d" * 32, events[0].event_id)
        self.assertEqual("lavi_chat_ui", events[0].source)
        self.assertEqual([([], "system")], callback_calls)
        self.assertEqual(
            ["user", "assistant"],
            [message["role"] for message in outputs[-1][1]],
        )
        self.assertEqual("second", outputs[-1][1][-1]["content"])

    def test_factory_rejects_non_generator_prediction_method(self):
        class NonStreamingEntrypoint:
            def predict(self, _message, _history, _system_prompt):
                return "not a stream"

        with self.assertRaisesRegex(
            TypeError,
            "prediction_entrypoint.predict must be a generator function",
        ):
            LocalChatInterfaceFactory(
                prediction_entrypoint=NonStreamingEntrypoint(),
            ).create(
                system_prompt=object(),
                examples=[],
                autofocus=False,
            )

    def _entrypoint(self, predict_callback):
        return LocalChatPredictionEntrypoint(
            input_event_adapter=LocalChatInputEventAdapter(
                event_id_factory=lambda: "d" * 32,
            ),
            predict_callback=predict_callback,
        )

    def _chat_interface(self, entrypoint):
        return LocalChatInterfaceFactory(
            prediction_entrypoint=entrypoint,
        ).create(
            system_prompt=gr.Textbox(render=False),
            examples=[],
            autofocus=False,
        )


__all__ = ["LocalChatInterfaceFactoryTests"]


if __name__ == "__main__":
    unittest.main()
