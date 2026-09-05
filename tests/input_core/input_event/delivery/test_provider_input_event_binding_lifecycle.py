#20260905_kpopmodder: Verifies provider callback binding lifecycle ownership.
from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest import mock

from input_core.input_event.delivery import ProviderInputEventBindingLifecycle


class ProviderInputEventBindingLifecycleTests(unittest.TestCase):
    def test_override_binding_is_idempotent_and_preserves_listener_order(self):
        emitted = []
        unrelated = _ignore
        plugin = _Plugin([unrelated])
        provider = _Provider("VoiceInput", plugin)
        providers = [provider]
        lifecycle = ProviderInputEventBindingLifecycle(
            provider_list_callback=lambda: providers,
            output_callback=emitted.append,
        )

        lifecycle.sync()
        adapter = plugin.input_event_listeners[-1]
        trusted_events = []
        factory_calls = []

        def factory(bound_adapter):
            factory_calls.append(bound_adapter)

            def listener(payload):
                trusted_events.append(bound_adapter.adapt(payload))

            return listener

        first = lifecycle.bind("VoiceInput", factory)
        second = lifecycle.bind(
            "VoiceInput",
            lambda _adapter: self.fail("a second factory must not run"),
        )
        lifecycle.sync()

        self.assertIs(first, second)
        self.assertEqual([adapter], factory_calls)
        self.assertEqual([unrelated, first], plugin.input_event_listeners)
        first("멈춰")
        self.assertEqual([], emitted)
        self.assertEqual(1, len(trusted_events))
        self.assertEqual("voice_input_final", trusted_events[0].source)
        self.assertEqual("VoiceInput", trusted_events[0].provider_id)
        self.assertEqual("final_transcript", trusted_events[0].event_kind)
        self.assertIs(True, trusted_events[0].final)

    def test_sync_rebinds_override_after_plugin_and_provider_reload(self):
        emitted = []
        first_plugin = _Plugin()
        first_provider = _Provider("VoiceInput", first_plugin)
        providers = [first_provider]
        lifecycle = ProviderInputEventBindingLifecycle(
            provider_list_callback=lambda: providers,
            output_callback=emitted.append,
        )
        built = []

        def factory(adapter):
            def listener(payload):
                emitted.append(adapter.adapt(payload))

            built.append((adapter, listener))
            return listener

        first_listener = lifecycle.bind("VoiceInput", factory)
        replacement_plugin = _Plugin()
        first_provider.plugin = replacement_plugin
        lifecycle.sync()

        self.assertEqual([], first_plugin.input_event_listeners)
        self.assertEqual([first_listener], replacement_plugin.input_event_listeners)

        replacement_provider = _Provider("VoiceInput", _Plugin())
        providers[:] = [replacement_provider]
        lifecycle.sync()
        replacement_listener = replacement_provider.plugin.input_event_listeners[0]

        self.assertEqual([], replacement_plugin.input_event_listeners)
        self.assertIs(first_listener, replacement_listener)
        self.assertEqual(1, len(built))
        replacement_listener("멈춰줘")
        self.assertEqual("voice_input_final", emitted[-1].source)
        self.assertEqual("final_transcript", emitted[-1].event_kind)
        self.assertIs(True, emitted[-1].final)

    def test_disabled_provider_detaches_without_losing_stable_override(self):
        emitted = []
        unrelated = _ignore
        plugin = _Plugin([unrelated, emitted.append])
        provider = _Provider("VoiceInput", plugin, disabled=True)
        lifecycle = ProviderInputEventBindingLifecycle(
            provider_list_callback=lambda: [provider],
            output_callback=emitted.append,
        )

        lifecycle.sync()
        self.assertEqual([unrelated], plugin.input_event_listeners)
        listener = lifecycle.bind(
            "VoiceInput",
            lambda adapter: lambda payload: adapter.adapt(payload),
        )
        self.assertEqual([unrelated], plugin.input_event_listeners)

        provider.disabled = False
        lifecycle.sync()
        self.assertEqual([unrelated, listener], plugin.input_event_listeners)

        provider.disabled = True
        lifecycle.sync()
        self.assertEqual([unrelated], plugin.input_event_listeners)

        provider.disabled = False
        lifecycle.sync()
        self.assertIs(listener, plugin.input_event_listeners[-1])

    def test_shutdown_detaches_current_and_stale_plugins_once(self):
        emitted = []
        first_plugin = _Plugin()
        provider = _Provider("VoiceInput", first_plugin)
        lifecycle = ProviderInputEventBindingLifecycle(
            provider_list_callback=lambda: [provider],
            output_callback=emitted.append,
        )
        listener = lifecycle.bind("VoiceInput", lambda adapter: adapter)
        replacement_plugin = _Plugin([emitted.append])
        provider.plugin = replacement_plugin

        lifecycle.shutdown()
        lifecycle.shutdown()
        lifecycle.sync()
        rebound = lifecycle.bind("VoiceInput", lambda adapter: adapter)

        self.assertEqual([], first_plugin.input_event_listeners)
        self.assertEqual([], replacement_plugin.input_event_listeners)
        self.assertIsNone(rebound)
        self.assertIsNotNone(listener)

    def test_legacy_adapter_path_is_stable_and_descriptor_bound(self):
        emitted = []
        unrelated = _ignore
        plugin = _Plugin([unrelated])
        provider = _Provider("TwitchChatFetch", plugin)
        lifecycle = ProviderInputEventBindingLifecycle(
            provider_list_callback=lambda: [provider],
            output_callback=emitted.append,
        )

        lifecycle.sync()
        first_adapter = plugin.input_event_listeners[-1]
        lifecycle.sync()
        second_adapter = plugin.input_event_listeners[-1]
        second_adapter("안녕하세요")

        self.assertIs(first_adapter, second_adapter)
        self.assertEqual([unrelated, first_adapter], plugin.input_event_listeners)
        self.assertEqual(1, len(emitted))
        self.assertEqual("안녕하세요", emitted[0].text)
        self.assertEqual("TwitchChatFetch", emitted[0].source)
        self.assertEqual("provider_output", emitted[0].event_kind)
        self.assertIs(True, emitted[0].final)

    def test_missing_provider_detaches_but_preserves_override_for_return(self):
        first_plugin = _Plugin()
        first_provider = _Provider("VoiceInput", first_plugin)
        providers = [first_provider]
        lifecycle = ProviderInputEventBindingLifecycle(
            provider_list_callback=lambda: providers,
            output_callback=_ignore,
        )
        factory_calls = []

        def factory(adapter):
            factory_calls.append(adapter)
            return adapter.adapt

        listener = lifecycle.bind("VoiceInput", factory)
        providers.clear()
        lifecycle.sync()

        replacement_plugin = _Plugin()
        providers.append(_Provider("VoiceInput", replacement_plugin))
        lifecycle.sync()

        self.assertEqual([], first_plugin.input_event_listeners)
        self.assertEqual([listener], replacement_plugin.input_event_listeners)
        self.assertEqual(1, len(factory_calls))

    def test_closed_lifecycle_keeps_validation_but_stops_provider_enumeration(self):
        provider_list_callback = mock.Mock(return_value=[])
        lifecycle = ProviderInputEventBindingLifecycle(
            provider_list_callback=provider_list_callback,
            output_callback=_ignore,
        )
        lifecycle.shutdown()
        provider_list_callback.reset_mock()

        with self.assertRaisesRegex(ValueError, "non-empty exact str"):
            lifecycle.bind("", _ignore)
        with self.assertRaisesRegex(TypeError, "listener_factory must be callable"):
            lifecycle.bind("VoiceInput", None)

        self.assertIsNone(lifecycle.bind("VoiceInput", lambda adapter: adapter))
        lifecycle.sync()
        lifecycle.shutdown()
        provider_list_callback.assert_not_called()


class _Plugin:
    def __init__(self, listeners=None):
        self.input_event_listeners = list(listeners or ())


class _Provider:
    def __init__(self, provider_id, plugin, *, disabled=False):
        self.handle = SimpleNamespace(
            descriptor=SimpleNamespace(id=provider_id),
        )
        self.plugin = plugin
        self.disabled = disabled


def _ignore(_payload):
    return None


if __name__ == "__main__":
    unittest.main()
