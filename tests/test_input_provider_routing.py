#20260716_kpopmodder: Regression tests for simultaneous input provider listener routing.
import sys
import unittest
from pathlib import Path
from unittest import mock


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


class InputProviderRoutingTests(unittest.TestCase):
    def _build_input(self, providers, config=None):
        from plugin_system.interfaces import InputPluginInterface

        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            InputPluginInterface: "input_gathering",
        }
        fake_loader.plugins = {
            "input_gathering": providers,
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value=config or {},
            ):
                from input_core.input_component import Input

                return Input()

    def test_dropdown_switch_keeps_one_listener_per_loaded_input_provider(self):
        from plugin_system.interfaces import InputPluginInterface

        class VoiceInput(InputPluginInterface):
            pass

        class TwitchChatFetch(InputPluginInterface):
            pass

        class YoutubeChatFetch(InputPluginInterface):
            pass

        voice = VoiceInput()
        twitch = TwitchChatFetch()
        youtube = YoutubeChatFetch()

        input_component = self._build_input([voice, twitch, youtube])

        for provider in (voice, twitch, youtube):
            self.assertEqual(1, provider.input_event_listeners.count(input_component.send_output))

        selected_name = input_component.on_dropdown_change("TwitchChatFetch")
        self.assertEqual("TwitchChatFetch", selected_name)
        self.assertIs(input_component.current_plugin, twitch)

        input_component.on_dropdown_change("TwitchChatFetch")
        input_component._sync_provider_listeners()

        for provider in (voice, twitch, youtube):
            self.assertEqual(1, provider.input_event_listeners.count(input_component.send_output))

    def test_voice_twitch_youtube_simultaneous_inputs_all_reach_output(self):
        from plugin_system.interfaces import InputPluginInterface

        class VoiceInput(InputPluginInterface):
            pass

        class TwitchChatFetch(InputPluginInterface):
            pass

        class YoutubeChatFetch(InputPluginInterface):
            pass

        voice = VoiceInput()
        twitch = TwitchChatFetch()
        youtube = YoutubeChatFetch()
        input_component = self._build_input([voice, twitch, youtube])
        received = []
        input_component.add_output_event_listener(received.append)

        voice.process_input("voice message")
        twitch.process_input("twitch message")
        youtube.process_input("youtube message")

        self.assertEqual(
            ["voice message", "twitch message", "youtube message"],
            received,
        )

    def test_failed_dropdown_load_returns_actual_current_provider_and_detaches_failed(self):
        from plugin_system.interfaces import InputPluginInterface

        class VoiceInput(InputPluginInterface):
            pass

        class BrokenInput(InputPluginInterface):
            def init(self):
                raise RuntimeError("broken input")

        voice = VoiceInput()
        broken = BrokenInput()
        input_component = self._build_input(
            [voice, broken],
            config={"default_input_gathering_provider": "VoiceInput"},
        )

        selected_name = input_component.on_dropdown_change("BrokenInput")

        self.assertEqual("VoiceInput", selected_name)
        self.assertIs(input_component.current_plugin, voice)
        self.assertEqual(1, voice.input_event_listeners.count(input_component.send_output))
        self.assertEqual(0, broken.input_event_listeners.count(input_component.send_output))

    def test_shutdown_removes_provider_listeners_once(self):
        from plugin_system.interfaces import InputPluginInterface

        class VoiceInput(InputPluginInterface):
            pass

        class TwitchChatFetch(InputPluginInterface):
            pass

        voice = VoiceInput()
        twitch = TwitchChatFetch()
        input_component = self._build_input([voice, twitch])

        input_component.shutdown()
        input_component.shutdown()

        self.assertNotIn(input_component.send_output, voice.input_event_listeners)
        self.assertNotIn(input_component.send_output, twitch.input_event_listeners)


if __name__ == "__main__":
    unittest.main()
