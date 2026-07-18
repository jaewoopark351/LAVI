import ast
from pathlib import Path
import unittest
from unittest import mock


class EventShutdownTests(unittest.TestCase):
    def test_event_subscription_is_idempotent_and_unsubscribable(self):
        from core.event_manager import EventManager, EventType

        manager = EventManager()
        calls = []

        def callback():
            calls.append("interrupt")

        subscription = manager.subscribe(EventType.INTERRUPT, callback)
        manager.subscribe(EventType.INTERRUPT, callback)

        self.assertEqual(1, manager.subscriber_count(EventType.INTERRUPT))

        manager.trigger(EventType.INTERRUPT)
        self.assertEqual(["interrupt"], calls)

        self.assertTrue(subscription.unsubscribe())
        self.assertFalse(subscription.active)
        self.assertFalse(subscription.unsubscribe())
        self.assertEqual(0, manager.subscriber_count(EventType.INTERRUPT))

        manager.trigger(EventType.INTERRUPT)
        self.assertEqual(["interrupt"], calls)

    def test_unsubscribed_callback_is_skipped_during_trigger(self):
        from core.event_manager import EventManager, EventType

        manager = EventManager()
        calls = []

        def first():
            calls.append("first")
            second_subscription.unsubscribe()

        def second():
            calls.append("second")

        manager.subscribe(EventType.INTERRUPT, first)
        second_subscription = manager.subscribe(EventType.INTERRUPT, second)

        manager.trigger(EventType.INTERRUPT)

        self.assertEqual(["first"], calls)

    def test_callback_exception_is_logged_and_later_callbacks_continue(self):
        #20260718_kpopmodder: Common EventManager should isolate normal listener failures.
        from core.event_manager import EventManager, EventType

        manager = EventManager()
        calls = []

        def first():
            calls.append("first")
            raise RuntimeError("listener failed")

        def second():
            calls.append("second")

        manager.subscribe(EventType.INTERRUPT, first)
        manager.subscribe(EventType.INTERRUPT, second)

        with self.assertLogs("LAV", level="ERROR") as logs:
            manager.trigger(EventType.INTERRUPT)

        self.assertEqual(["first", "second"], calls)
        self.assertTrue(
            any("Event callback failed" in message for message in logs.output)
        )

    def test_keyboard_interrupt_from_callback_is_not_swallowed(self):
        from core.event_manager import EventManager, EventType

        manager = EventManager()
        calls = []

        def first():
            calls.append("first")
            raise KeyboardInterrupt()

        def second():
            calls.append("second")

        manager.subscribe(EventType.INTERRUPT, first)
        manager.subscribe(EventType.INTERRUPT, second)

        with self.assertRaises(KeyboardInterrupt):
            manager.trigger(EventType.INTERRUPT)

        self.assertEqual(["first"], calls)

    def test_hybrid_openai_llm_interrupt_subscription_is_shutdown_owned(self):
        from core.event_manager import EventType, event_manager
        from plugins.Hybrid_OpenAI_LLM.Hybrid_OpenAI_LLM import (
            Hybrid_OpenAI_LLM,
            HybridOpenAISettings,
        )

        class FakeLocalProvider:
            def __init__(self):
                self.interrupt_count = 0
                self.unload_count = 0

            def request_interrupt(self):
                self.interrupt_count += 1

            def unload(self):
                self.unload_count += 1

        providers = []

        def fake_build_runtime(plugin):
            provider = FakeLocalProvider()
            providers.append(provider)
            plugin.local_provider = provider

        event_manager.clear(EventType.INTERRUPT)
        try:
            with mock.patch.object(
                HybridOpenAISettings,
                "load",
                return_value=object(),
            ), mock.patch.object(
                Hybrid_OpenAI_LLM,
                "_build_runtime",
                fake_build_runtime,
            ):
                plugin = Hybrid_OpenAI_LLM()
                plugin.init()
                plugin.init()

                self.assertEqual(1, event_manager.subscriber_count(EventType.INTERRUPT))
                self.assertTrue(plugin._interrupt_subscription.active)

                event_manager.trigger(EventType.INTERRUPT)
                self.assertEqual(1, providers[-1].interrupt_count)

                plugin.shutdown()
                self.assertEqual(0, event_manager.subscriber_count(EventType.INTERRUPT))
                self.assertIsNone(plugin._interrupt_subscription)
                self.assertEqual(1, providers[-1].unload_count)

                event_manager.trigger(EventType.INTERRUPT)
                self.assertEqual(1, providers[-1].interrupt_count)
        finally:
            event_manager.clear(EventType.INTERRUPT)

    def test_llm_event_dispatcher_can_remove_and_clear_listeners(self):
        from llm_core.event_dispatcher import LLMEventDispatcher

        dispatcher = LLMEventDispatcher()
        calls = []

        def callback(output):
            calls.append(output)

        dispatcher.add_output_event_listener(callback)
        dispatcher.add_output_event_listener(callback)
        dispatcher.send_output("one")

        self.assertEqual(["one"], calls)
        self.assertEqual(1, len(dispatcher.output_event_listeners))

        self.assertTrue(dispatcher.remove_output_event_listener(callback))
        dispatcher.send_output("two")
        self.assertEqual(["one"], calls)

        dispatcher.add_output_event_listener(callback, full_response=True)
        dispatcher.clear_listeners()
        self.assertEqual([], dispatcher.output_event_listeners)
        self.assertEqual([], dispatcher.full_output_event_listeners)

    def test_runtime_components_define_shutdown_methods(self):
        project_root = Path(__file__).resolve().parents[1]
        expected_shutdown_classes = {
            "input_core/input_component.py": "Input",
            "llm_core/llm_component.py": "LLM",
            "translation_core/translate_component.py": "Translate",
            "tts_core/tts_component.py": "TTS",
            "vtuber_core/vtuber_component.py": "Vtuber",
            "plugins/ScreenVision/screenVision.py": "ScreenVision",
            "plugins/VoiceInput/voiceInput.py": "VoiceInput",
        }

        for relative_path, class_name in expected_shutdown_classes.items():
            tree = ast.parse((project_root / relative_path).read_text(encoding="utf-8"))
            matching_classes = [
                node
                for node in tree.body
                if isinstance(node, ast.ClassDef) and node.name == class_name
            ]
            self.assertEqual(
                1,
                len(matching_classes),
                f"{relative_path}:{class_name} not found",
            )

            method_names = {
                node.name
                for node in matching_classes[0].body
                if isinstance(node, ast.FunctionDef)
            }
            self.assertIn(
                "shutdown",
                method_names,
                f"{relative_path}:{class_name} should define shutdown()",
            )

    def test_screenvision_own_interrupt_guard_is_one_shot(self):
        project_root = Path(__file__).resolve().parents[1]
        tree = ast.parse(
            (project_root / "plugins/ScreenVision/screenVision.py").read_text(
                encoding="utf-8",
            )
        )
        screenvision_class = next(
            node
            for node in tree.body
            if isinstance(node, ast.ClassDef) and node.name == "ScreenVision"
        )
        handle_interrupt = next(
            node
            for node in screenvision_class.body
            if isinstance(node, ast.FunctionDef) and node.name == "handle_interrupt"
        )
        own_interrupt_guard = next(
            (
                node
                for node in ast.walk(handle_interrupt)
                if isinstance(node, ast.If)
                and "ignore_next_own_interrupt" in ast.unparse(node.test)
            ),
            None,
        )

        self.assertIsNotNone(own_interrupt_guard)

        clears_guard_before_return = False
        for node in own_interrupt_guard.body:
            if isinstance(node, ast.Assign):
                clears_guard_before_return = any(
                    isinstance(target, ast.Attribute)
                    and target.attr == "ignore_next_own_interrupt"
                    and isinstance(target.value, ast.Name)
                    and target.value.id == "self"
                    for target in node.targets
                ) and isinstance(node.value, ast.Constant) and node.value.value is False
            if isinstance(node, ast.Return):
                break

        self.assertTrue(clears_guard_before_return)


if __name__ == "__main__":
    unittest.main()
