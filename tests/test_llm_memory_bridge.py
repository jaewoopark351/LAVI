import unittest

from llm_core.memory_bridge import LLMMemoryBridge
from llm_core.response_pipeline import LLMResponsePipeline


class FakeMemoryStore:
    def __init__(self):
        self.events = []

    def add_raw_event(self, event_type, value, source, metadata):
        self.events.append({
            "event_type": event_type,
            "value": value,
            "source": source,
            "metadata": metadata,
        })


class FakeContextBuilder:
    def __init__(self, context_text="", memory_store=None):
        self.context_text = context_text
        self.memory_store = memory_store

    def build_context_text(self):
        return self.context_text


class QueryAwareContextBuilder:
    def __init__(self):
        self.queries = []

    def build_context_text(self, query=None):
        self.queries.append(query)
        return "\n[recalled memory]"


class FakeCommandHandler:
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
        raise AssertionError("memory command must not call the LLM plugin")


class CapturingPlugin:
    def __init__(self):
        self.calls = []

    def predict(self, message, history, system_prompt):
        self.calls.append((message, history, system_prompt))
        return "recalled answer"


class LLMMemoryBridgeTests(unittest.TestCase):
    def test_builds_prompt_and_records_raw_event(self):
        memory_store = FakeMemoryStore()
        context_builder = FakeContextBuilder(
            context_text="\n[memory]",
            memory_store=memory_store,
        )
        bridge = LLMMemoryBridge(
            memory_context_builder=context_builder,
        )

        self.assertEqual(
            "base\n[memory]",
            bridge.build_augmented_system_prompt("base"),
        )

        bridge.record_raw_event(
            event_type="assistant_message",
            value="hello",
            source="test",
            metadata={"kind": "user"},
        )

        self.assertEqual(1, len(memory_store.events))
        self.assertEqual("hello", memory_store.events[0]["value"])

    def test_missing_or_failing_memory_components_fall_back_safely(self):
        class FailingContextBuilder:
            def build_context_text(self):
                raise RuntimeError("context failure")

        class FailingCommandHandler:
            def try_handle(self, message):
                raise RuntimeError("command failure")

        bridge = LLMMemoryBridge(
            memory_context_builder=FailingContextBuilder(),
            memory_command_handler=FailingCommandHandler(),
        )

        self.assertEqual(
            "base",
            bridge.build_augmented_system_prompt("base"),
        )
        self.assertIsNone(bridge.try_handle_command("remember this"))

    def test_passes_current_user_query_to_memory_context_builder(self):
        context_builder = QueryAwareContextBuilder()
        bridge = LLMMemoryBridge(
            memory_context_builder=context_builder,
        )

        result = bridge.build_augmented_system_prompt(
            "base",
            query="What did I name my cat?",
        )

        self.assertEqual(
            ["What did I name my cat?"],
            context_builder.queries,
        )
        self.assertEqual("base\n[recalled memory]", result)

    def test_memory_command_keeps_pipeline_short_circuit_behavior(self):
        memory_store = FakeMemoryStore()
        context_builder = FakeContextBuilder(memory_store=memory_store)
        command_handler = FakeCommandHandler(response="saved")
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
            memory_command_handler=command_handler,
        )

        result = list(pipeline.predict(
            "remember this",
            history=[],
            system_prompt="base",
        ))

        self.assertEqual(["saved"], result)
        self.assertEqual(["saved"], [output["text"] for output in streamed_outputs])
        self.assertEqual(1, streamed_outputs[0]["response_generation"])
        self.assertEqual(["saved"], full_outputs)
        self.assertEqual([["remember this", "saved"]], history)
        self.assertEqual(["remember this"], command_handler.messages)
        self.assertEqual(
            ["user_message", "assistant_message"],
            [event["event_type"] for event in memory_store.events],
        )

    def test_pipeline_uses_current_message_as_recall_query(self):
        memory_store = FakeMemoryStore()
        context_builder = QueryAwareContextBuilder()
        context_builder.memory_store = memory_store
        plugin = CapturingPlugin()

        pipeline = LLMResponsePipeline(
            current_plugin_callback=lambda: plugin,
            send_output_callback=lambda output: None,
            send_full_output_callback=lambda output: None,
            history_callback=lambda: [],
            remember_history_callback=lambda: False,
            live_textbox=FakeLiveTextbox(),
            streaming_chunker=FakeStreamingChunker(),
            memory_context_builder=context_builder,
        )

        result = list(pipeline.predict(
            "What did I name my cat?",
            history=[],
            system_prompt="base",
        ))

        self.assertEqual(["recalled answer"], result)
        self.assertEqual(
            ["What did I name my cat?"],
            context_builder.queries,
        )
        self.assertIn("[recalled memory]", plugin.calls[0][2])


if __name__ == "__main__":
    unittest.main()
