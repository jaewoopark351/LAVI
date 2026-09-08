#20260905_kpopmodder: Verifies the exact Gradio registration and streaming path for local Chat.
import asyncio
import inspect
import unittest

import gradio as gr
from gradio.state_holder import SessionState

from input_core.input_event.adapters import LocalChatInputEventAdapter
from llm_core.chat_input import (
    LocalChatInterfaceFactory,
    LocalChatPredictionEntrypoint,
)
from llm_core.chat_input.gradio import GradioLocalChatStreamCompletionAdapter


class LocalChatInterfaceFactoryTests(unittest.TestCase):
    def test_factory_registers_bound_generator_method(self):
        entrypoint = self._entrypoint(
            lambda _event, _history, _prompt: iter(("ok",))
        )

        async def assert_gradio_contract():
            chat = self._chat_interface(entrypoint)
            self.assertTrue(chat.is_generator)
            self.assertIsInstance(
                chat.fn.__self__,
                GradioLocalChatStreamCompletionAdapter,
            )

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

    def test_empty_product_stream_completes_without_an_assistant_card(self):
        callback_messages = []

        def predict(event, _history, _system_prompt):
            callback_messages.append(event.text)
            return
            yield

        entrypoint = self._entrypoint(predict)

        async def collect_stream():
            chat = self._chat_interface(entrypoint)
            outputs = [
                output
                async for output in chat._stream_fn("STOP", [], "system")
            ]
            return chat, outputs

        chat, outputs = asyncio.run(collect_stream())

        self.assertEqual(["STOP"], callback_messages)
        self.assertEqual(1, len(outputs))
        self.assertEqual([], outputs[0][0])
        self.assertEqual(["user"], [item["role"] for item in outputs[0][1]])
        self.assertEqual("STOP", outputs[0][1][0]["content"])

        postprocessed = chat.chatbot.postprocess(outputs[0][1])
        round_tripped = type(postprocessed).model_validate_json(
            postprocessed.model_dump_json()
        )
        preprocessed = chat.chatbot.preprocess(round_tripped)

        self.assertEqual(["user"], [item["role"] for item in preprocessed])

    def test_empty_stream_finishes_queue_iterator_and_accepts_next_submit(self):
        callback_messages = []

        def predict(event, _history, _system_prompt):
            callback_messages.append(event.text)
            if event.text == "STOP":
                return
            yield "next response"

        entrypoint = self._entrypoint(predict)

        async def submit_twice():
            chat = self._chat_interface(entrypoint)
            submit_index = next(
                index
                for index, block_fn in chat.fns.items()
                if block_fn.api_name == "_submit_fn"
            )
            state = SessionState(chat)
            state[chat.saved_input._id] = "STOP"
            state[chat.chatbot_state._id] = []

            first = await chat.process_api(
                submit_index,
                [None, None, "system"],
                state=state,
                iterator=None,
                session_hash="completion-test",
            )
            completed = await chat.process_api(
                submit_index,
                [],
                state=state,
                iterator=first["iterator"],
                session_hash="completion-test",
            )

            state[chat.saved_input._id] = "GET"
            state[chat.chatbot_state._id] = []
            next_submit = await chat.process_api(
                submit_index,
                [None, None, "system"],
                state=state,
                iterator=None,
                session_hash="completion-test",
            )
            next_completed = await chat.process_api(
                submit_index,
                [],
                state=state,
                iterator=next_submit["iterator"],
                session_hash="completion-test",
            )
            return first, completed, next_submit, next_completed

        first, completed, next_submit, next_completed = asyncio.run(
            submit_twice()
        )

        self.assertTrue(first["is_generating"])
        self.assertIsNotNone(first["iterator"])
        self.assertIsNone(first["data"][0])
        self.assertEqual(
            ["user"],
            [item["role"] for item in first["data"][1]],
        )
        self.assertFalse(completed["is_generating"])
        self.assertIsNone(completed["iterator"])
        self.assertTrue(next_submit["is_generating"])
        self.assertIsNotNone(next_submit["iterator"])
        self.assertFalse(next_completed["is_generating"])
        self.assertIsNone(next_completed["iterator"])
        self.assertEqual(["STOP", "GET"], callback_messages)

    def test_nonempty_minecraft_status_round_trips_and_allows_next_submit(self):
        callback_messages = []

        def predict(event, _history, _system_prompt):
            callback_messages.append(event.text)
            yield gr.ChatMessage(
                content="다이아 곡괭이 만드는 중이야",
                metadata={"title": "Minecraft"},
            )

        entrypoint = self._entrypoint(predict)

        async def submit_twice():
            chat = self._chat_interface(entrypoint)
            first = [
                output
                async for output in chat._stream_fn(
                    "지금 뭐 해?",
                    [],
                    "system",
                )
            ]
            second = [
                output
                async for output in chat._stream_fn(
                    "다음 질문",
                    first[-1][1],
                    "system",
                )
            ]
            return chat, first, second

        chat, first, second = asyncio.run(submit_twice())

        self.assertEqual(
            ["지금 뭐 해?", "다음 질문"],
            callback_messages,
        )
        self.assertEqual(1, len(first))
        self.assertEqual(1, len(second))
        self.assertEqual(
            ["user", "assistant"],
            [item["role"] for item in first[-1][1]],
        )
        self.assertEqual(
            ["user", "assistant", "user", "assistant"],
            [item["role"] for item in second[-1][1]],
        )
        self.assertEqual(
            "다이아 곡괭이 만드는 중이야",
            second[-1][1][-1]["content"],
        )
        self.assertEqual(
            "Minecraft",
            second[-1][1][-1]["metadata"]["title"],
        )
        postprocessed = chat.chatbot.postprocess(second[-1][1])
        round_tripped = type(postprocessed).model_validate_json(
            postprocessed.model_dump_json()
        )
        preprocessed = chat.chatbot.preprocess(round_tripped)
        assistants = [
            item for item in preprocessed if item["role"] == "assistant"
        ]
        self.assertEqual(2, len(assistants))
        self.assertEqual(
            [
                [
                    {
                        "text": "다이아 곡괭이 만드는 중이야",
                        "type": "text",
                    }
                ],
                [
                    {
                        "text": "다이아 곡괭이 만드는 중이야",
                        "type": "text",
                    }
                ],
            ],
            [item["content"] for item in assistants],
        )
        self.assertTrue(all(item["content"] for item in preprocessed))

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
