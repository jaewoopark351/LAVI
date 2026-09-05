#20260905_kpopmodder: Owns descriptor-bound adapter instances for live provider identities.
from __future__ import annotations

from input_core.input_event.adapters import ProviderBoundInputEventAdapter


class ProviderAdapterRegistry:
    """Create and retain one stable adapter for each provider object."""

    def __init__(self, *, output_callback, source_resolver) -> None:
        self._output_callback = output_callback
        self._source_resolver = source_resolver
        self._adapters = {}

    def adapter_for(self, provider):
        adapter = self._adapters.get(provider)
        if adapter is None:
            adapter = ProviderBoundInputEventAdapter(
                provider=provider,
                output_callback=self._output_callback,
                source_resolver=self._source_resolver,
            )
            self._adapters[provider] = adapter
        return adapter

    def get(self, provider):
        return self._adapters.get(provider)

    def providers(self) -> tuple:
        return tuple(self._adapters)

    def retire(self, provider) -> None:
        self._adapters.pop(provider, None)

    def clear(self) -> None:
        self._adapters.clear()


__all__ = ("ProviderAdapterRegistry",)
