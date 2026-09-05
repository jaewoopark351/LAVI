#20260716_kpopmodder: Regression tests for simultaneous input provider listener routing.
import sys
import unittest
from pathlib import Path
from types import SimpleNamespace
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
            "input_gathering": [
                _ProviderHandle(provider)
                for provider in providers
            ],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value=config or {},
            ):
                from input_core.input_component import Input

                component = Input()
                component.create_all_provider_ui()
        return component

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
        original_callbacks = {
            provider: provider.input_event_listeners[0]
            for provider in (voice, twitch, youtube)
        }

        for provider in (voice, twitch, youtube):
            self.assertEqual(1, len(provider.input_event_listeners))

        selected_name = input_component.on_dropdown_change("TwitchChatFetch")
        self.assertEqual("TwitchChatFetch", selected_name)
        self.assertIs(input_component.current_plugin, twitch)

        input_component.on_dropdown_change("TwitchChatFetch")
        input_component._sync_provider_listeners()

        for provider in (voice, twitch, youtube):
            self.assertEqual(1, len(provider.input_event_listeners))
            self.assertIs(
                original_callbacks[provider],
                provider.input_event_listeners[0],
            )

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
            [event.text for event in received],
        )
        self.assertEqual(
            ["voice_input_final", "TwitchChatFetch", "YoutubeChatFetch"],
            [event.source for event in received],
        )
        self.assertEqual([True, True, True], [event.final for event in received])

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
        self.assertEqual(1, len(voice.input_event_listeners))
        self.assertEqual(0, len(broken.input_event_listeners))

    def test_shutdown_removes_provider_listeners_once(self):
        from plugin_system.interfaces import InputPluginInterface

        class VoiceInput(InputPluginInterface):
            pass

        class TwitchChatFetch(InputPluginInterface):
            pass

        voice = VoiceInput()
        twitch = TwitchChatFetch()
        input_component = self._build_input([voice, twitch])
        callbacks = [
            voice.input_event_listeners[0],
            twitch.input_event_listeners[0],
        ]

        input_component.shutdown()
        input_component.shutdown()

        self.assertNotIn(callbacks[0], voice.input_event_listeners)
        self.assertNotIn(callbacks[1], twitch.input_event_listeners)

    def test_exact_voice_provider_can_bind_trusted_edge_without_plain_llm_duplicate(self):
        from app_core.composition_core.component_wiring import (
            TrustedVoiceInputWiring,
        )
        from plugin_system.interfaces import InputPluginInterface

        class VoiceInput(InputPluginInterface):
            pass

        class TwitchChatFetch(InputPluginInterface):
            pass

        voice = VoiceInput()
        twitch = TwitchChatFetch()
        input_component = self._build_input([voice, twitch])
        llm = _TrustedVoiceLlm()
        observed = []
        input_component.add_output_event_listener(llm.receive_input)
        input_component.add_output_event_listener(observed.append)

        wiring = TrustedVoiceInputWiring()
        first = wiring.wire(input_component=input_component, llm=llm)
        second = wiring.wire(input_component=input_component, llm=llm)
        voice.process_input("voice message")
        twitch.process_input("twitch message")

        self.assertIs(first, second)
        self.assertEqual(["voice message"], [event.text for event in llm.trusted])
        self.assertEqual(["twitch message"], [event.text for event in llm.received])
        self.assertEqual(
            ["voice message", "twitch message"],
            [event.text for event in observed],
        )
        self.assertEqual(1, len(voice.input_event_listeners))

    def test_trusted_voice_binding_reconciles_when_provider_appears_late(self):
        from app_core.composition_core.component_wiring import (
            TrustedVoiceInputWiring,
        )
        from plugin_system.interfaces import InputPluginInterface
        from plugin_system.selection_core.provider import Provider

        class TwitchChatFetch(InputPluginInterface):
            pass

        class VoiceInput(InputPluginInterface):
            pass

        input_component = self._build_input([TwitchChatFetch()])
        llm = _TrustedVoiceLlm()
        input_component.add_output_event_listener(llm.receive_input)

        self.assertIsNone(
            TrustedVoiceInputWiring().wire(
                input_component=input_component,
                llm=llm,
            )
        )

        voice = VoiceInput()
        provider = Provider()
        provider.handle = _ProviderHandle(voice)
        provider.name = "VoiceInput"
        input_component.provider_list.append(provider)
        input_component.create_all_provider_ui()
        voice.process_input("late voice")

        self.assertEqual(["late voice"], [event.text for event in llm.trusted])
        self.assertEqual([], llm.received)
        self.assertEqual(1, len(voice.input_event_listeners))


class _ProviderHandle:
    def __init__(self, plugin):
        self.name = plugin.__class__.__name__
        self.descriptor = SimpleNamespace(id=self.name)
        self.runtime_contract = None
        self.error = ""
        self._plugin = plugin

    def construct(self, _plugin_type):
        return self._plugin


class _TrustedVoiceLlm:
    def __init__(self):
        self.received = []
        self.trusted = []

    def receive_input(self, event):
        self.received.append(event)

    def create_trusted_voice_input_final_enqueue_coordinator(
        self,
        adapter,
        *,
        pre_accept_observers=(),
        post_accept_observers=(),
    ):
        def callback(payload):
            event = adapter.adapt(payload)
            for observer in pre_accept_observers:
                observer(event)
            self.trusted.append(event)
            for observer in post_accept_observers:
                observer(event)
            return object()

        return callback


if __name__ == "__main__":
    unittest.main()
