#20260905_kpopmodder: Verifies focused provider adaptation and selection owners.
from __future__ import annotations

from types import SimpleNamespace

from input_core.component.provider_selection import (
    InputProviderBindingRequestRegistry,
    InputProviderSelectionCoordinator,
)
from input_core.component.provider_selection.binding_requests import (
    InputProviderBindingRequestResolver,
    InputProviderBindingRequestStore,
)
from input_core.component.provider_selection.selection import (
    InputProviderListenerSynchronizer,
    InputProviderSelectionMutation,
    InputProviderUiRefresh,
)
from input_core.input_event.adapters import ProviderBoundInputEventAdapter
from input_core.input_event.adapters.provider_bound import (
    ProviderBoundInputEventFactory,
    ProviderBoundInputEventForwarder,
)


def test_provider_bound_adapter_separates_adaptation_from_forwarding():
    received = []
    provider = SimpleNamespace(
        handle=SimpleNamespace(descriptor=SimpleNamespace(id="VoiceInput"))
    )
    adapter = ProviderBoundInputEventAdapter(
        provider=provider,
        output_callback=received.append,
        event_id_factory=lambda: "b" * 32,
    )

    event = adapter("마이크 입력")

    assert event is None
    assert type(adapter._event_factory) is ProviderBoundInputEventFactory
    assert type(adapter._forwarder) is ProviderBoundInputEventForwarder
    assert received[0].text == "마이크 입력"


def test_selection_facade_composes_three_independent_effects():
    calls = []
    coordinator = InputProviderSelectionCoordinator(
        create_all_provider_ui_callback=lambda: calls.append("ui"),
        select_provider_callback=lambda name: calls.append(name) or name,
        sync_provider_listeners_callback=lambda: calls.append("sync"),
    )

    coordinator.create_all_provider_ui()
    assert coordinator.select_provider("VoiceInput") == "VoiceInput"

    assert type(coordinator._components.ui_refresh) is InputProviderUiRefresh
    assert (
        type(coordinator._components.selection_mutation)
        is InputProviderSelectionMutation
    )
    assert (
        type(coordinator._components.listener_synchronizer)
        is InputProviderListenerSynchronizer
    )
    assert calls == ["ui", "sync", "VoiceInput", "sync"]


def test_binding_request_facade_keeps_store_and_resolution_separate():
    available = {}
    registry = InputProviderBindingRequestRegistry(
        lambda provider_id, factory: (
            factory() if available.get(provider_id) else None
        )
    )

    assert registry.bind("VoiceInput", lambda: "listener") is None
    available["VoiceInput"] = True
    registry.reconcile()

    assert type(registry._request_store) is InputProviderBindingRequestStore
    assert (
        type(registry._request_resolver)
        is InputProviderBindingRequestResolver
    )
    assert registry.bind("VoiceInput", lambda: "replacement") == "listener"
