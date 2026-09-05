#20260905_kpopmodder: Stores pending and resolved provider binding requests.
from __future__ import annotations


class InputProviderBindingRequestStore:
    def __init__(self) -> None:
        self.listener_factories = {}
        self.resolved_listeners = {}

    def remember(self, provider_id: str, listener_factory) -> None:
        if provider_id not in self.listener_factories:
            self.listener_factories[provider_id] = listener_factory

    def provider_ids(self) -> tuple[str, ...]:
        return tuple(self.listener_factories)

    def clear(self) -> None:
        self.resolved_listeners.clear()
        self.listener_factories.clear()


__all__ = ("InputProviderBindingRequestStore",)
