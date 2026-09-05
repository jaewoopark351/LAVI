#20260905_kpopmodder: Verifies the legacy provider-binding facade remains delegation-only.
from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest import mock

from input_core.input_event.delivery import ProviderInputEventBindingLifecycle


class ProviderInputEventBindingLifecycleDelegationTests(unittest.TestCase):
    def test_public_operations_delegate_to_the_composed_responsibility_owners(self):
        synchronizer = mock.Mock()
        override_binder = mock.Mock()
        shutdown_coordinator = mock.Mock()
        graph = SimpleNamespace(
            synchronizer=synchronizer,
            override_binder=override_binder,
            shutdown_coordinator=shutdown_coordinator,
        )
        provider_list_callback = mock.Mock()
        output_callback = mock.Mock()
        source_resolver = object()
        listener_factory = mock.Mock()
        override_binder.bind.return_value = "stable-listener"

        with mock.patch(
            "input_core.input_event.delivery."
            "provider_input_event_binding_lifecycle."
            "ProviderBindingComponentGraph",
            return_value=graph,
        ) as graph_factory:
            lifecycle = ProviderInputEventBindingLifecycle(
                provider_list_callback=provider_list_callback,
                output_callback=output_callback,
                source_resolver=source_resolver,
            )
            lifecycle.sync()
            bound = lifecycle.bind("VoiceInput", listener_factory)
            lifecycle.shutdown()

        graph_factory.assert_called_once_with(
            provider_list_callback=provider_list_callback,
            output_callback=output_callback,
            source_resolver=source_resolver,
        )
        synchronizer.sync.assert_called_once_with()
        override_binder.bind.assert_called_once_with(
            "VoiceInput",
            listener_factory,
        )
        shutdown_coordinator.shutdown.assert_called_once_with()
        self.assertEqual("stable-listener", bound)


if __name__ == "__main__":
    unittest.main()
