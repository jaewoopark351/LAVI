#20260905_kpopmodder: Composes the focused provider-binding lifecycle collaborators.
from __future__ import annotations

from input_core.input_event.provenance import InputProviderSourceResolver

from .provider_adapter_registry import ProviderAdapterRegistry
from .provider_binding_arguments import validate_constructor_callbacks
from .provider_binding_lifecycle_state import ProviderBindingLifecycleState
from .provider_binding_shutdown_coordinator import (
    ProviderBindingShutdownCoordinator,
)
from .provider_binding_synchronizer import ProviderBindingSynchronizer
from .provider_descriptor_resolver import ProviderDescriptorResolver
from .provider_detachment_callback_resolver import (
    ProviderDetachmentCallbackResolver,
)
from .provider_listener_attachment_owner import (
    ProviderListenerAttachmentOwner,
)
from .provider_override_binder import ProviderOverrideBinder
from .provider_override_binding_registry import (
    ProviderOverrideBindingRegistry,
)
from .provider_snapshot_source import ProviderSnapshotSource


class ProviderBindingComponentGraph:
    """Build the private collaborators behind the compatibility facade."""

    def __init__(
        self,
        *,
        provider_list_callback,
        output_callback,
        source_resolver=None,
    ) -> None:
        validate_constructor_callbacks(provider_list_callback, output_callback)
        resolved_source_resolver = source_resolver or InputProviderSourceResolver()

        snapshot_source = ProviderSnapshotSource(provider_list_callback)
        descriptor_resolver = ProviderDescriptorResolver()
        adapter_registry = ProviderAdapterRegistry(
            output_callback=output_callback,
            source_resolver=resolved_source_resolver,
        )
        override_registry = ProviderOverrideBindingRegistry()
        attachment_owner = ProviderListenerAttachmentOwner()
        lifecycle_state = ProviderBindingLifecycleState()
        callback_resolver = ProviderDetachmentCallbackResolver(
            output_callback=output_callback,
            adapter_registry=adapter_registry,
            override_registry=override_registry,
        )
        synchronizer = ProviderBindingSynchronizer(
            snapshot_source=snapshot_source,
            descriptor_resolver=descriptor_resolver,
            adapter_registry=adapter_registry,
            override_registry=override_registry,
            attachment_owner=attachment_owner,
            callback_resolver=callback_resolver,
            lifecycle_state=lifecycle_state,
        )

        self._synchronizer = synchronizer
        self._override_binder = ProviderOverrideBinder(
            snapshot_source=snapshot_source,
            descriptor_resolver=descriptor_resolver,
            adapter_registry=adapter_registry,
            override_registry=override_registry,
            synchronizer=synchronizer,
            lifecycle_state=lifecycle_state,
        )
        self._shutdown_coordinator = ProviderBindingShutdownCoordinator(
            snapshot_source=snapshot_source,
            adapter_registry=adapter_registry,
            override_registry=override_registry,
            attachment_owner=attachment_owner,
            callback_resolver=callback_resolver,
            lifecycle_state=lifecycle_state,
        )

    @property
    def synchronizer(self):
        return self._synchronizer

    @property
    def override_binder(self):
        return self._override_binder

    @property
    def shutdown_coordinator(self):
        return self._shutdown_coordinator


__all__ = ("ProviderBindingComponentGraph",)
