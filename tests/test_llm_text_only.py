#20260630_kpopmodder: Added focused tests for text-only LLM generation.
import os
import sys
import unittest
from types import SimpleNamespace

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from LLM import LLM


class FakeResponsePipeline:
    def is_generator_plugin(self, plugin):
        return bool(getattr(plugin, "is_generator", False))


class FakeSnapshotGeneratorPlugin:
    is_generator = True

    def __init__(self):
        self.calls = []

    def predict(self, message, history, system_prompt):
        self.calls.append((message, history, system_prompt))
        yield "partial"
        yield "partial final"


class FakeDeltaGeneratorPlugin:
    is_generator = True

    def __init__(self):
        self.calls = []

    def predict(self, message, history, system_prompt):
        self.calls.append((message, history, system_prompt))
        yield "partial"
        yield " final"


class FakeTextPlugin:
    is_generator = False

    def __init__(self, output):
        self.output = output
        self.calls = []
        self.init_count = 0

    def init(self):
        self.init_count += 1

    def predict(self, message, history, system_prompt):
        self.calls.append((message, history, system_prompt))
        return self.output


class LLMTextOnlyTests(unittest.TestCase):
    def make_llm_without_plugins(self, current_plugin, providers=None):
        llm = LLM.__new__(LLM)
        llm.current_plugin = current_plugin
        llm.provider_list = list(providers or [])
        llm.plugin_type = object
        llm.response_pipeline = FakeResponsePipeline()
        return llm

    def test_generate_text_only_collects_snapshot_generator_output(self):
        plugin = FakeSnapshotGeneratorPlugin()
        llm = self.make_llm_without_plugins(plugin)

        output = llm.generate_text_only("move", "system")

        self.assertEqual("partial final", output)
        self.assertEqual([("move", [], "system")], plugin.calls)

    def test_generate_text_only_collects_delta_generator_output(self):#20260630_kpopmodder
        plugin = FakeDeltaGeneratorPlugin()
        llm = self.make_llm_without_plugins(plugin)

        output = llm.generate_text_only("move", "system")

        self.assertEqual("partial final", output)
        self.assertEqual([("move", [], "system")], plugin.calls)

    def test_generate_text_only_uses_preferred_provider_when_available(self):
        fallback = FakeTextPlugin("fallback")
        preferred = FakeTextPlugin("preferred")
        provider = SimpleNamespace(
            name="Hybrid_OpenAI_LLM",
            plugin=preferred,
        )
        llm = self.make_llm_without_plugins(fallback, [provider])

        output = llm.generate_text_only(
            "move",
            "system",
            preferred_provider_name="Hybrid_OpenAI_LLM",
        )

        self.assertEqual("preferred", output)
        self.assertEqual([], fallback.calls)
        self.assertEqual([("move", [], "system")], preferred.calls)

    def test_generate_text_only_lazy_initializes_preferred_provider(self):#20260630_kpopmodder
        fallback = FakeTextPlugin("fallback")
        preferred = FakeTextPlugin("preferred")
        provider = SimpleNamespace(
            name="Hybrid_OpenAI_LLM",
            plugin=preferred,
            initialized=False,
            disabled=False,
            init_error="",
        )
        llm = self.make_llm_without_plugins(fallback, [provider])

        output = llm.generate_text_only(
            "move",
            "system",
            preferred_provider_name="Hybrid_OpenAI_LLM",
        )

        self.assertEqual("preferred", output)
        self.assertTrue(provider.initialized)
        self.assertEqual(1, preferred.init_count)
        self.assertEqual([], fallback.calls)
        self.assertEqual([("move", [], "system")], preferred.calls)


if __name__ == "__main__":
    unittest.main()
