# 20260905_kpopmodder: Verifies focused owners behind the legacy LLM facade.
from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest import mock

from plugin_system.interfaces import LLMPluginInterface
from llm_core.chat_session import LlmChatHistoryResetter
from llm_core.composition.generation_output import (
    LlmGenerationOutputComponentGraph,
)
from llm_core.composition.speech_content import LlmSpeechContentComponentGraph
from llm_core.composition.trusted_ingress import (
    LlmTrustedIngressComponentGraph,
)
from llm_core.composition.ui_lifecycle import LlmUiLifecycleComponentGraph
from llm_core.content import LlmSystemPromptContentRepository
from llm_core.event_dispatcher import LLMEventDispatcher
from llm_core.generation import LlmGenerationFacade
from llm_core.llm_component import LLM
from llm_core.output import LlmOutputListenerRegistry
from llm_core.routed_response import RoutedResponseUiPresentationQueue
from llm_core.speech_configuration import (
    LlmEffectiveSystemPromptBuilder,
    LlmSpeechStylePersistence,
    LlmSpeechStyleStateController,
    LlmSpeechStyleUpdateCoordinator,
)
from llm_core.ui import LlmChatUiBuilder


class LlmLegacyResponsibilitySplitTests(unittest.TestCase):
    def test_real_constructor_installs_focused_graph_and_shuts_down(self):
        plugin = _LlmPlugin()
        handle = _LlmHandle(plugin)
        fake_loader = SimpleNamespace(
            interface_to_category={LLMPluginInterface: "language_model"},
            plugins={"language_model": [handle]},
        )
        subscription = _Subscription()

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={},
            ):
                with mock.patch(
                    "llm_core.composition.speech_content."
                    "llm_speech_content_component_graph."
                    "LLMContextManager",
                    return_value=_ContentManager("initial"),
                ):
                    with mock.patch(
                        "llm_core.composition.speech_content."
                        "llm_speech_content_component_graph."
                        "config_manager.load_section",
                        return_value={},
                    ):
                        with mock.patch(
                            "llm_core.composition.ui_lifecycle."
                            "llm_ui_lifecycle_component_graph."
                            "event_manager.subscribe",
                            return_value=subscription,
                        ):
                            llm = LLM()

        self.assertIsInstance(llm.chat_ui_builder, LlmChatUiBuilder)
        self.assertIsInstance(
            llm.output_listener_registry,
            LlmOutputListenerRegistry,
        )
        self.assertIsInstance(llm.generation_facade, LlmGenerationFacade)
        self.assertIsInstance(
            llm.system_prompt_content_repository,
            LlmSystemPromptContentRepository,
        )
        installer = llm.compatibility_graph_installer
        self.assertIsInstance(
            installer._speech_content_graph,
            LlmSpeechContentComponentGraph,
        )
        self.assertIsInstance(
            installer._generation_output_graph,
            LlmGenerationOutputComponentGraph,
        )
        self.assertIsInstance(
            installer._trusted_ingress_graph,
            LlmTrustedIngressComponentGraph,
        )
        self.assertIsInstance(
            installer._ui_lifecycle_graph,
            LlmUiLifecycleComponentGraph,
        )
        for field_name in (
            "chat_history_resetter",
            "chat_ui_builder",
            "context_manager",
            "event_dispatcher",
            "generation_facade",
            "input_event_normalizer",
            "input_queue_display_updater",
            "input_queue_worker",
            "local_chat_input_adapter",
            "local_chat_interface_factory",
            "local_chat_prediction_entrypoint",
            "output_listener_registry",
            "response_pipeline",
            "routed_external_response_publisher",
            "routed_input_dispatch_coordinator",
            "runtime_lifecycle_coordinator",
            "speech_style_helper",
            "speech_style_mode",
            "streaming_chunker",
            "system_prompt_content_repository",
            "trusted_ingress_graph",
            "trusted_ingress_producer_registrar_factory",
            "trusted_local_chat_input_dispatch_coordinator",
            "trusted_user_input_ingress_claim_registry",
        ):
            self.assertTrue(hasattr(llm, field_name), field_name)

        llm.routed_response_ui_presentation_queue.enqueue("before reset")
        llm.reset_chat()
        self.assertEqual(
            (),
            llm.routed_response_ui_presentation_queue.snapshot()[1],
        )
        llm.routed_response_ui_presentation_queue.enqueue("before shutdown")
        llm.shutdown()
        self.assertTrue(subscription.unsubscribed)
        self.assertEqual(
            (),
            llm.routed_response_ui_presentation_queue.snapshot()[1],
        )

    def test_history_content_and_output_have_independent_owners(self):
        history = [["user", "answer"]]
        presentation_queue = RoutedResponseUiPresentationQueue()
        presentation_queue.enqueue("late terminal")
        resetter = LlmChatHistoryResetter(
            lambda: history,
            clear_pending_presentations_callback=presentation_queue.clear,
        )
        manager = _ContentManager("initial")
        synchronized = []
        repository = LlmSystemPromptContentRepository(
            context_manager=manager,
            update_current_callback=synchronized.append,
        )
        dispatcher = LLMEventDispatcher()
        registry = LlmOutputListenerRegistry(dispatcher)
        outputs = []
        full_outputs = []
        registry.add(outputs.append)
        registry.add(full_outputs.append, full_response=True)

        resetter.reset()
        self.assertEqual("initial", repository.load())
        repository.update("updated")
        registry.send_output("chunk")
        registry.send_full_output("complete")

        self.assertEqual([], history)
        self.assertEqual((), presentation_queue.snapshot()[1])
        self.assertEqual(["initial", "updated"], synchronized)
        self.assertEqual(["chunk"], outputs)
        self.assertEqual(["complete"], full_outputs)

    def test_speech_state_persistence_and_prompt_building_are_separate(self):
        modes = {"current": "polite"}
        saved = []
        helper = _SpeechHelper()
        state = LlmSpeechStyleStateController(
            helper_callback=lambda: helper,
            current_mode_callback=lambda: modes["current"],
            update_mode_callback=lambda mode: modes.__setitem__("current", mode),
            default_mode="polite",
        )
        persistence = LlmSpeechStylePersistence(
            load_section_callback=lambda section: {"section": section},
            save_config_callback=lambda *values: saved.append(values),
        )
        update = LlmSpeechStyleUpdateCoordinator(
            state_controller=state,
            persistence=persistence,
        )
        prompt = LlmEffectiveSystemPromptBuilder(
            helper_callback=lambda: helper,
            current_mode_callback=state.current_mode,
            default_prompt_callback=lambda: "default",
        )

        update.update("casual-label")

        self.assertEqual("casual", modes["current"])
        self.assertEqual([("LLM", "speech_style", "casual")], saved)
        self.assertEqual("base|casual", prompt.build("base"))
        self.assertEqual("default|casual", prompt.build())

    def test_generation_facade_keeps_pipeline_and_text_only_engines_distinct(self):
        pipeline = _GenerationPipeline()
        text_helper = _TextOnlyHelper()
        generation = LlmGenerationFacade(
            response_pipeline_callback=lambda: pipeline,
            text_only_helper_callback=lambda: text_helper,
        )

        generation.llm_output = "answer"
        generation.start_of_response = False

        self.assertEqual("answer", generation.llm_output)
        self.assertFalse(generation.start_of_response)
        self.assertTrue(generation.is_generator())
        self.assertEqual(
            "text-only",
            generation.generate_text_only(
                "message",
                "system",
                preferred_provider_name="provider",
            ),
        )
        self.assertEqual("collected", generation.collect_text_only_generator_output(()))

    def test_llm_new_seams_lazily_install_focused_compatibility_owners(self):
        llm = LLM.__new__(LLM)
        llm.history = [["user", "answer"]]
        llm.context_manager = _ContentManager("initial")
        llm.speech_style_mode = "polite"
        llm.event_dispatcher = LLMEventDispatcher()
        llm.response_pipeline = _GenerationPipeline()
        llm.text_only_generation_helper = _TextOnlyHelper()
        received = []

        llm.add_output_event_listener(received.append)
        llm.send_output("chunk")
        llm.reset_chat()
        llm.update_file("updated")

        self.assertEqual(["chunk"], received)
        self.assertEqual([], llm.history)
        self.assertEqual("updated", llm.system_prompt_text)
        self.assertIsInstance(llm.output_listener_registry, LlmOutputListenerRegistry)
        self.assertIsInstance(llm.chat_history_resetter, LlmChatHistoryResetter)
        self.assertIsInstance(
            llm.system_prompt_content_repository,
            LlmSystemPromptContentRepository,
        )

    def test_facade_ui_method_delegates_without_constructing_gradio(self):
        calls = []
        llm = LLM.__new__(LLM)
        llm.compatibility_graph_installer = SimpleNamespace(
            ensure_ui_builder=lambda: SimpleNamespace(
                build=lambda: calls.append("build")
            )
        )

        llm.create_ui()

        self.assertEqual(["build"], calls)


class _ContentManager:
    def __init__(self, content):
        self.system_prompt_text = content

    def load_content(self):
        return self.system_prompt_text

    def update_file(self, content):
        self.system_prompt_text = content


class _SpeechHelper:
    def normalize(self, value):
        if value in ("casual", "casual-label"):
            return "casual"
        return "polite"

    def label_for(self, mode):
        return f"label:{mode}"

    def build_prompt(self, base, mode):
        return f"{base}|{mode}"


class _GenerationPipeline:
    def __init__(self):
        self.LLM_output = ""
        self.start_of_response = True

    def is_generator(self):
        return True


class _TextOnlyHelper:
    def generate(self, *_args, **_kwargs):
        return "text-only"

    def collect_generator_output(self, _result):
        return "collected"


class _LlmPlugin(LLMPluginInterface):
    def init(self):
        return None

    def start(self):
        return None

    def stop(self):
        return None

    def shutdown(self):
        return None

    def cleanup(self):
        return None

    def predict(self, _message, _history, _system_prompt):
        return ""


class _LlmHandle:
    name = "FakeLlm"
    runtime_contract = None
    error = ""

    def __init__(self, plugin):
        self._plugin = plugin

    def construct(self, _plugin_type):
        return self._plugin


class _Subscription:
    def __init__(self):
        self.unsubscribed = False

    def unsubscribe(self):
        self.unsubscribed = True


if __name__ == "__main__":
    unittest.main()
