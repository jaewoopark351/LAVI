#20260905_kpopmodder: Projects the exact callback set owned by one provider binding.
from __future__ import annotations


class ProviderDetachmentCallbackResolver:
    """Resolve callbacks that binding cleanup may remove from a plugin."""

    def __init__(
        self,
        *,
        output_callback,
        adapter_registry,
        override_registry,
    ) -> None:
        self._output_callback = output_callback
        self._adapter_registry = adapter_registry
        self._override_registry = override_registry

    def callbacks_for(self, provider) -> tuple:
        callbacks = [self._output_callback]
        adapter = self._adapter_registry.get(provider)
        if adapter is not None:
            callbacks.append(adapter)
        callbacks.extend(self._override_registry.listener_callbacks())
        return tuple(callbacks)


__all__ = ("ProviderDetachmentCallbackResolver",)
