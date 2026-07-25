#20260725_kpopmodder: Covers LLM short-circuit routing for explicit game commands.
import unittest

from llm_core.response_pipeline import LLMResponsePipeline


class FakeMemoryStore:
    def __init__(self):
        self.events = []

    def add_raw_event(self, event_type, value, source, metadata):
        self.events.append(
            {
                "event_type": event_type,
                "value": value,
                "source": source,
                "metadata": metadata,
            }
        )


class FakeContextBuilder:
    def __init__(self, memory_store=None):
        self.memory_store = memory_store

    def build_context_text(self, query=None, active_history=None):
        return ""


class FakeGameCommandHandler:
    def __init__(self, response=None):
        self.response = response
        self.messages = []

    def try_handle(self, message):
        self.messages.append(message)
        return self.response


class FakeLiveTextbox:
    def __init__(self):
        self.entries = []

    def print(self, text, append_to_last=False):
        self.entries.append((text, append_to_last))


class FakeStreamingChunker:
    def get_streaming_tts_chunk(self, output, processed_idx):
        return "", processed_idx


class FailingPlugin:
    def predict(self, message, history, system_prompt):
        raise AssertionError("game command must not call the LLM plugin")


class CapturingPlugin:
    def __init__(self):
        self.calls = []

    def predict(self, message, history, system_prompt):
        self.calls.append((message, history, system_prompt))
        return "normal response"


class LLMGameCommandBridgeTests(unittest.TestCase):
    def test_game_command_short_circuits_llm_plugin(self):
        memory_store = FakeMemoryStore()
        context_builder = FakeContextBuilder(memory_store=memory_store)
        command_handler = FakeGameCommandHandler(
            response="Minecraft command accepted: get_item",
        )
        history = []
        streamed_outputs = []
        full_outputs = []

        pipeline = LLMResponsePipeline(
            current_plugin_callback=lambda: FailingPlugin(),
            send_output_callback=streamed_outputs.append,
            send_full_output_callback=full_outputs.append,
            history_callback=lambda: history,
            remember_history_callback=lambda: True,
            live_textbox=FakeLiveTextbox(),
            streaming_chunker=FakeStreamingChunker(),
            memory_context_builder=context_builder,
            game_command_handler=command_handler,
        )

        result = list(
            pipeline.predict(
                "minecraft get oak_log 1",
                history=[],
                system_prompt="base",
            )
        )

        self.assertEqual(["Minecraft command accepted: get_item"], result)
        self.assertEqual(
            ["Minecraft command accepted: get_item"],
            [output["text"] for output in streamed_outputs],
        )
        self.assertEqual(["Minecraft command accepted: get_item"], full_outputs)
        self.assertEqual(
            [["minecraft get oak_log 1", "Minecraft command accepted: get_item"]],
            history,
        )
        self.assertEqual(["minecraft get oak_log 1"], command_handler.messages)
        self.assertEqual(
            ["user_message", "assistant_message"],
            [event["event_type"] for event in memory_store.events],
        )
        self.assertEqual("game_command", memory_store.events[1]["source"])
        self.assertEqual(
            "game_command_response",
            memory_store.events[1]["metadata"]["kind"],
        )

    def test_non_game_command_continues_to_llm_plugin(self):
        plugin = CapturingPlugin()
        command_handler = FakeGameCommandHandler(response=None)

        pipeline = LLMResponsePipeline(
            current_plugin_callback=lambda: plugin,
            send_output_callback=lambda output: None,
            send_full_output_callback=lambda output: None,
            history_callback=lambda: [],
            remember_history_callback=lambda: False,
            live_textbox=FakeLiveTextbox(),
            streaming_chunker=FakeStreamingChunker(),
            memory_context_builder=FakeContextBuilder(),
            game_command_handler=command_handler,
        )

        result = list(
            pipeline.predict(
                "hello LAVI",
                history=[],
                system_prompt="base",
            )
        )

        self.assertEqual(["normal response"], result)
        self.assertEqual(["hello LAVI"], command_handler.messages)
        self.assertEqual("hello LAVI", plugin.calls[0][0])


if __name__ == "__main__":
    unittest.main()
