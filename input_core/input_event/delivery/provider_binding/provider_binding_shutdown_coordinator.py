#20260905_kpopmodder: Coordinates terminal cleanup for one provider-binding component.
from __future__ import annotations


class ProviderBindingShutdownCoordinator:
    """Detach owned callbacks and retire provider-binding state exactly once."""

    def __init__(
        self,
        *,
        snapshot_source,
        adapter_registry,
        override_registry,
        attachment_owner,
        callback_resolver,
        lifecycle_state,
    ) -> None:
        self._snapshot_source = snapshot_source
        self._adapter_registry = adapter_registry
        self._override_registry = override_registry
        self._attachment_owner = attachment_owner
        self._callback_resolver = callback_resolver
        self._lifecycle_state = lifecycle_state

    def shutdown(self) -> None:
        if not self._lifecycle_state.close_once():
            return

        providers = self._snapshot_source.snapshot()
        for provider in providers:
            plugin = getattr(provider, "plugin", None)
            if plugin is not None:
                self._attachment_owner.detach(
                    plugin,
                    self._callback_resolver.callbacks_for(provider),
                )
        for provider, plugin in self._attachment_owner.attached_items():
            self._attachment_owner.detach(
                plugin,
                self._callback_resolver.callbacks_for(provider),
            )

        self._attachment_owner.clear()
        self._override_registry.clear()
        self._adapter_registry.clear()


__all__ = ("ProviderBindingShutdownCoordinator",)
