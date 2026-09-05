#20260905_kpopmodder: Resolves stored requests against currently active providers.
from __future__ import annotations

from .input_provider_binding_request_store import (
    InputProviderBindingRequestStore,
)


class InputProviderBindingRequestResolver:
    def __init__(
        self,
        *,
        bind_callback,
        store: InputProviderBindingRequestStore,
    ) -> None:
        if not callable(bind_callback):
            raise TypeError("bind_callback must be callable")
        if type(store) is not InputProviderBindingRequestStore:
            raise TypeError("store must be exact")
        self._bind_callback = bind_callback
        self._store = store

    def resolve(self, provider_id: str):
        listener = self._store.resolved_listeners.get(provider_id)
        if listener is not None:
            return listener
        listener = self._bind_callback(
            provider_id,
            self._store.listener_factories[provider_id],
        )
        if listener is not None:
            self._store.resolved_listeners[provider_id] = listener
        return listener


__all__ = ("InputProviderBindingRequestResolver",)
