#20260905_kpopmodder: Preserves the provider binding-request registry API.
from __future__ import annotations

from .composition import InputProviderBindingRequestComponentGraph


class InputProviderBindingRequestRegistry:
    def __init__(self, bind_callback) -> None:
        self._components = InputProviderBindingRequestComponentGraph(
            bind_callback=bind_callback
        )
        self._bind_callback = bind_callback
        self._request_store = self._components.store
        self._request_resolver = self._components.resolver
        self._listener_factories = self._request_store.listener_factories
        self._resolved_listeners = self._request_store.resolved_listeners

    def bind(self, provider_id, listener_factory):
        if type(provider_id) is not str or not provider_id:
            raise ValueError("provider_id must be a non-empty exact str")
        if not callable(listener_factory):
            raise TypeError("listener_factory must be callable")
        self._request_store.remember(provider_id, listener_factory)
        return self._resolve(provider_id)

    def reconcile(self) -> None:
        for provider_id in self._request_store.provider_ids():
            self._resolve(provider_id)

    def clear(self) -> None:
        self._request_store.clear()

    def _resolve(self, provider_id):
        return self._request_resolver.resolve(provider_id)


__all__ = ("InputProviderBindingRequestRegistry",)
