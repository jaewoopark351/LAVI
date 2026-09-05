#20260905_kpopmodder: Owns stable request-specific listener overrides and their current providers.
from __future__ import annotations


class ProviderOverrideBindingRegistry:
    """Store stable override listeners separately from live owner selection."""

    def __init__(self) -> None:
        self._adapters = {}
        self._owners = {}
        self._listeners = {}

    def contains(self, provider_id) -> bool:
        return provider_id in self._listeners

    def provider_ids(self) -> tuple:
        return tuple(self._listeners)

    def listener_for(self, provider_id):
        return self._listeners[provider_id]

    def listener_callbacks(self) -> tuple:
        return tuple(self._listeners.values())

    def adapter_for(self, provider_id):
        return self._adapters.get(provider_id)

    def owner_for(self, provider_id):
        return self._owners.get(provider_id)

    def owner_items(self) -> tuple:
        return tuple(self._owners.items())

    def register(self, provider_id, provider, adapter, listener) -> None:
        self._adapters[provider_id] = adapter
        self._owners[provider_id] = provider
        self._listeners[provider_id] = listener

    def replace_owners(self, selected_owners) -> None:
        self._owners = dict(selected_owners)

    def clear(self) -> None:
        self._listeners.clear()
        self._owners.clear()
        self._adapters.clear()


__all__ = ("ProviderOverrideBindingRegistry",)
