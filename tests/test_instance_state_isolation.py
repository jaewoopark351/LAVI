import ast
from pathlib import Path
import unittest
from unittest import mock


class InstanceStateIsolationTests(unittest.TestCase):
    def test_input_plugin_listeners_are_instance_scoped(self):
        from plugin_system.interfaces import InputPluginInterface

        first = InputPluginInterface()
        second = InputPluginInterface()

        first.input_event_listeners.append(lambda text: text)

        self.assertEqual(1, len(first.input_event_listeners))
        self.assertEqual([], second.input_event_listeners)
        self.assertIsNot(first.input_event_listeners, second.input_event_listeners)

    def test_vtuber_avatar_data_is_instance_scoped(self):
        from plugin_system.interfaces import VtuberPluginInterface
        from plugin_system.interfaces_core.avatar_data import AvatarData

        first = VtuberPluginInterface()
        second = VtuberPluginInterface()

        first.avatar_data.mouth_open = 0.7

        self.assertIs(VtuberPluginInterface.AvatarData, AvatarData)
        self.assertIsInstance(VtuberPluginInterface.AvatarData(), AvatarData)
        self.assertIsNot(first.avatar_data, second.avatar_data)
        self.assertEqual(0.7, first.avatar_data.mouth_open)
        self.assertEqual(0, second.avatar_data.mouth_open)

    def test_input_plugin_continues_after_listener_exception(self):
        from plugin_system.interfaces import InputPluginInterface

        plugin = InputPluginInterface()
        seen = []

        def broken_listener(_text):
            seen.append("broken")
            raise RuntimeError("listener boom")

        def stable_listener(text):
            seen.append(f"stable:{text}")

        plugin.input_event_listeners = [broken_listener, stable_listener]

        with mock.patch(
            "plugin_system.interfaces_core.input_plugin_interface.log_print"
        ) as log_mock:
            plugin.process_input("hello")

        self.assertEqual(["broken", "stable:hello"], seen)
        self.assertIn("listener boom", str(log_mock.mock_calls))
        self.assertIn("broken_listener", str(log_mock.mock_calls))

    def test_input_plugin_iterates_listener_snapshot(self):
        from plugin_system.interfaces import InputPluginInterface

        plugin = InputPluginInterface()
        seen = []

        def clearing_listener(_text):
            seen.append("clearing")
            plugin.input_event_listeners.clear()

        def stable_listener(_text):
            seen.append("stable")

        plugin.input_event_listeners = [clearing_listener, stable_listener]

        plugin.process_input("hello")

        self.assertEqual(["clearing", "stable"], seen)
        self.assertEqual([], plugin.input_event_listeners)

    def test_input_plugin_propagates_process_interrupt_exceptions(self):
        from plugin_system.interfaces import InputPluginInterface

        for exception_type in (KeyboardInterrupt, SystemExit):
            with self.subTest(exception_type=exception_type.__name__):
                plugin = InputPluginInterface()

                def interrupting_listener(_text):
                    raise exception_type()

                plugin.input_event_listeners = [interrupting_listener]

                with self.assertRaises(exception_type):
                    plugin.process_input("hello")

    def test_queue_listener_and_textbox_state_are_not_class_assignments(self):
        project_root = Path(__file__).resolve().parents[1]
        checked_classes = {
            "translation_core/translate_component.py": {
                "Translate": {
                    "input_queue",
                    "output_event_listeners",
                    "input_process_thread",
                    "log_live_textbox",
                    "queue_live_textbox",
                },
            },
            "tts_core/tts_component.py": {
                "TTS": {
                    "log_live_textbox",
                    "process_queue_live_textbox",
                },
            },
            "plugins/TwitchChatFetch/twitchChatFetch.py": {
                "TwitchChatFetch": {
                    "liveTextbox",
                    "console_textbox",
                    "queue_textbox",
                    "chatlog",
                    "chat_process_thread",
                },
            },
            "plugins/YoutubeChatFetch/youtubeChatFetch.py": {
                "YoutubeChatFetch": {
                    "liveTextbox",
                    "console_textbox",
                    "queue_textbox",
                    "chatlog",
                    "chat_process_thread",
                },
            },
            "plugins/Chess/Chess.py": {
                "Chess": {
                    "console_textbox",
                    "game_state_textbox",
                },
            },
            "plugins/Idle_think/IdleThink.py": {
                "IdleThink": {
                    "console_textbox",
                },
            },
        }

        for relative_path, class_names in checked_classes.items():
            tree = ast.parse((project_root / relative_path).read_text(encoding="utf-8"))
            for node in tree.body:
                if not isinstance(node, ast.ClassDef) or node.name not in class_names:
                    continue
                forbidden_names = class_names[node.name]
                assigned_names = {
                    target.id
                    for statement in node.body
                    if isinstance(statement, ast.Assign)
                    for target in statement.targets
                    if isinstance(target, ast.Name)
                }
                self.assertFalse(
                    forbidden_names & assigned_names,
                    f"{relative_path}:{node.name} has class state: "
                    f"{sorted(forbidden_names & assigned_names)}",
                )


if __name__ == "__main__":
    unittest.main()
