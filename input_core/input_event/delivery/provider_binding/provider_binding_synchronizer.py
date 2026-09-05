#20260905_kpopmodder: Coordinates one provider inventory snapshot into live listener bindings.
from __future__ import annotations


class ProviderBindingSynchronizer:
    """Reconcile registries and listener attachments with loaded providers."""

    def __init__(
        self,
        *,
        snapshot_source,
        descriptor_resolver,
        adapter_registry,
        override_registry,
        attachment_owner,
        callback_resolver,
        lifecycle_state,
    ) -> None:
        self._snapshot_source = snapshot_source
        self._descriptor_resolver = descriptor_resolver
        self._adapter_registry = adapter_registry
        self._override_registry = override_registry
        self._attachment_owner = attachment_owner
        self._callback_resolver = callback_resolver
        self._lifecycle_state = lifecycle_state

    def sync(self) -> None:
        if self._lifecycle_state.closed:
            return

        providers = self._snapshot_source.snapshot()
        selected_owners = self._select_override_owners(providers)
        self._retire_replaced_override_owners(selected_owners)
        self._override_registry.replace_owners(selected_owners)
        self._retire_missing_providers(providers)

        for provider in providers:
            self._sync_provider(provider)

    def _select_override_owners(self, providers) -> dict:
        owners = {}
        for provider_id in self._override_registry.provider_ids():
            provider = self._descriptor_resolver.find(providers, provider_id)
            if provider is not None:
                owners[provider_id] = provider
        return owners

    def _retire_replaced_override_owners(self, selected_owners) -> None:
        for provider_id, previous_owner in self._override_registry.owner_items():
            if selected_owners.get(provider_id) is previous_owner:
                continue
            plugin = self._attachment_owner.plugin_for(previous_owner)
            if plugin is not None:
                self._attachment_owner.detach(
                    plugin,
                    self._callback_resolver.callbacks_for(previous_owner),
                )
            if (
                self._adapter_registry.get(previous_owner)
                is self._override_registry.adapter_for(provider_id)
            ):
                self._adapter_registry.retire(previous_owner)

    def _retire_missing_providers(self, providers) -> None:
        current = set(providers)
        known = set(self._adapter_registry.providers()) | set(
            self._attachment_owner.providers()
        )
        for provider in known - current:
            plugin = self._attachment_owner.plugin_for(provider)
            if plugin is not None:
                self._attachment_owner.detach(
                    plugin,
                    self._callback_resolver.callbacks_for(provider),
                )
                self._attachment_owner.forget(provider)
            self._adapter_registry.retire(provider)

    def _sync_provider(self, provider) -> None:
        plugin = getattr(provider, "plugin", None)
        previous_plugin = self._attachment_owner.plugin_for(provider)
        if previous_plugin is not None and previous_plugin is not plugin:
            self._attachment_owner.detach(
                previous_plugin,
                self._callback_resolver.callbacks_for(provider),
            )

        if plugin is None:
            self._attachment_owner.forget(provider)
            return

        self._attachment_owner.remember(provider, plugin)
        self._attachment_owner.listener_list(plugin)
        self._attachment_owner.detach(
            plugin,
            self._callback_resolver.callbacks_for(provider),
        )
        listener = self._listener_for(provider)
        if getattr(provider, "disabled", False):
            return
        self._attachment_owner.attach(plugin, listener)

    def _listener_for(self, provider):
        provider_id = self._descriptor_resolver.descriptor_id(provider)
        if self._override_registry.owner_for(provider_id) is provider:
            return self._override_registry.listener_for(provider_id)
        return self._adapter_registry.adapter_for(provider)


__all__ = ("ProviderBindingSynchronizer",)
