#20260905_kpopmodder: Preserves the legacy provider-binding API as a thin delegation facade.
from __future__ import annotations

from .provider_binding import ProviderBindingComponentGraph


class ProviderInputEventBindingLifecycle:
    """Expose the stable public lifecycle while focused collaborators own behavior."""

    def __init__(
        self,
        *,
        provider_list_callback,
        output_callback,
        source_resolver=None,
    ) -> None:
        self._component_graph = ProviderBindingComponentGraph(
            provider_list_callback=provider_list_callback,
            output_callback=output_callback,
            source_resolver=source_resolver,
        )

    def sync(self) -> None:
        self._component_graph.synchronizer.sync()

    def bind(self, provider_id, listener_factory):
        return self._component_graph.override_binder.bind(
            provider_id,
            listener_factory,
        )

    def shutdown(self) -> None:
        self._component_graph.shutdown_coordinator.shutdown()


__all__ = ("ProviderInputEventBindingLifecycle",)
