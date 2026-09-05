#20260905_kpopmodder: Owns creation and stable registration of one provider listener override.
from __future__ import annotations

from .provider_binding_arguments import (
    validate_bind_request,
    validate_provider_listener,
)


class ProviderOverrideBinder:
    """Bind a caller-created listener once for an exact provider descriptor."""

    def __init__(
        self,
        *,
        snapshot_source,
        descriptor_resolver,
        adapter_registry,
        override_registry,
        synchronizer,
        lifecycle_state,
    ) -> None:
        self._snapshot_source = snapshot_source
        self._descriptor_resolver = descriptor_resolver
        self._adapter_registry = adapter_registry
        self._override_registry = override_registry
        self._synchronizer = synchronizer
        self._lifecycle_state = lifecycle_state

    def bind(self, provider_id, listener_factory):
        validate_bind_request(provider_id, listener_factory)
        if self._lifecycle_state.closed:
            return None

        providers = self._snapshot_source.snapshot()
        provider = self._descriptor_resolver.find(providers, provider_id)
        if provider is None:
            return None
        if self._override_registry.contains(provider_id):
            self._synchronizer.sync()
            return self._override_registry.listener_for(provider_id)

        adapter = self._adapter_registry.adapter_for(provider)
        listener = listener_factory(adapter)
        validate_provider_listener(listener)
        self._override_registry.register(
            provider_id,
            provider,
            adapter,
            listener,
        )
        self._synchronizer.sync()
        return listener


__all__ = ("ProviderOverrideBinder",)
