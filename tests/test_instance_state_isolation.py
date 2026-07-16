import ast
from pathlib import Path
import unittest


class InstanceStateIsolationTests(unittest.TestCase):
    def test_input_plugin_listeners_are_instance_scoped(self):
        from plugin_system.interfaces import InputPluginInterface

        first = InputPluginInterface()
        second = InputPluginInterface()

        first.input_event_listeners.append(lambda text: text)

        self.assertEqual(1, len(first.input_event_listeners))
        self.assertEqual([], second.input_event_listeners)
        self.assertIsNot(first.input_event_listeners, second.input_event_listeners)

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
            "TTS.py": {
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
