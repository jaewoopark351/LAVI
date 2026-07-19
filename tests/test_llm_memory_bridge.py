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


class ActiveHistoryContextBuilder:
    def __init__(self, context_text="\n[common memory]", memory_store=None):
        self.context_text = context_text
        self.memory_store = memory_store
        self.calls = []

    def build_context_text(self, query=None, active_history=None):
        self.calls.append({
            "query": query,
            "active_history": active_history,
            "event_count": len(self.memory_store.events)
            if self.memory_store is not None
            else None,
        })
        return self.context_text


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

    def test_passes_active_history_to_memory_context_builder(self):
        context_builder = ActiveHistoryContextBuilder()
        bridge = LLMMemoryBridge(
            memory_context_builder=context_builder,
        )
        active_history = [["GPU limit?", "It may reduce heat."]]

        result = bridge.build_augmented_system_prompt(
            "base",
            query="Does that hurt performance?",
            active_history=active_history,
        )

        self.assertEqual("base\n[common memory]", result)
        self.assertEqual(active_history, context_builder.calls[0]["active_history"])

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

    def test_pipeline_builds_memory_before_recording_current_user_event(self):
        memory_store = FakeMemoryStore()
        context_builder = ActiveHistoryContextBuilder(memory_store=memory_store)
        plugin = CapturingPlugin()
        active_history = [["GPU 전력 제한 낮추자", "온도는 줄고 성능은 조금 줄어."]]

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

        list(pipeline.predict(
            "그거 성능 많이 떨어져?",
            history=active_history,
            system_prompt="base",
        ))

        self.assertEqual(0, context_builder.calls[0]["event_count"])
        self.assertEqual(active_history, context_builder.calls[0]["active_history"])
        self.assertEqual(
            ["user_message", "assistant_message"],
            [event["event_type"] for event in memory_store.events],
        )

    def test_pipeline_uses_recent_model_history_when_active_history_is_empty(self):#20260720_kpopmodder
        memory_store = FakeMemoryStore()
        context_builder = ActiveHistoryContextBuilder(memory_store=memory_store)
        plugin = CapturingPlugin()
        stored_history = []

        pipeline = LLMResponsePipeline(
            current_plugin_callback=lambda: plugin,
            send_output_callback=lambda output: None,
            send_full_output_callback=lambda output: None,
            history_callback=lambda: stored_history,
            remember_history_callback=lambda: True,
            live_textbox=FakeLiveTextbox(),
            streaming_chunker=FakeStreamingChunker(),
            memory_context_builder=context_builder,
        )

        list(pipeline.predict(
            "first topic",
            history=[],
            system_prompt="base",
        ))
        stored_history.clear()
        list(pipeline.predict(
            "follow up",
            history=[],
            system_prompt="base",
        ))

        self.assertEqual(
            [["first topic", "recalled answer"]],
            plugin.calls[1][1],
        )
        self.assertEqual(
            [["first topic", "recalled answer"]],
            context_builder.calls[1]["active_history"],
        )

    def test_pipeline_continues_without_memory_when_context_builder_fails(self):
        class FailingActiveContextBuilder:
            def build_context_text(self, query=None, active_history=None):
                raise RuntimeError("short-term selector failed")

        plugin = CapturingPlugin()
        pipeline = LLMResponsePipeline(
            current_plugin_callback=lambda: plugin,
            send_output_callback=lambda output: None,
            send_full_output_callback=lambda output: None,
            history_callback=lambda: [],
            remember_history_callback=lambda: False,
            live_textbox=FakeLiveTextbox(),
            streaming_chunker=FakeStreamingChunker(),
            memory_context_builder=FailingActiveContextBuilder(),
        )

        result = list(pipeline.predict(
            "hello",
            history=[],
            system_prompt="base",
        ))

        self.assertEqual(["recalled answer"], result)
        self.assertEqual("base", plugin.calls[0][2])

    def test_common_memory_context_reaches_different_llm_plugins(self):
        class OtherCapturingPlugin(CapturingPlugin):
            pass

        first_plugin = CapturingPlugin()
        second_plugin = OtherCapturingPlugin()

        self._run_pipeline_with_plugin(first_plugin)
        self._run_pipeline_with_plugin(second_plugin)

        self.assertIn("[common memory]", first_plugin.calls[0][2])
        self.assertIn("[common memory]", second_plugin.calls[0][2])
        self.assertEqual(first_plugin.calls[0][2], second_plugin.calls[0][2])

    def _run_pipeline_with_plugin(self, plugin):
        memory_store = FakeMemoryStore()
        context_builder = ActiveHistoryContextBuilder(memory_store=memory_store)
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
        list(pipeline.predict(
            "same query",
            history=[],
            system_prompt="base",
        ))


if __name__ == "__main__":
    unittest.main()
