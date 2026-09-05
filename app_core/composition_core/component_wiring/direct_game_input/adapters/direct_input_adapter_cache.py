#20260905_kpopmodder: Own stable callback-identity direct input adapters.
from __future__ import annotations

from input_core.input_event.adapters import DirectCallbackInputEventAdapter


class DirectInputAdapterCache:
    def __init__(self):
        self._adapters = {}

    def get_or_create(
        self,
        *,
        source,
        provider_id,
        event_kind,
        output_callback,
    ):
        callback_owner = getattr(output_callback, "__self__", None)
        callback_function = getattr(output_callback, "__func__", output_callback)
        key = (source, id(callback_owner), callback_function)
        adapter = self._adapters.get(key)
        if adapter is None:
            adapter = DirectCallbackInputEventAdapter(
                output_callback=output_callback,
                source=source,
                provider_id=provider_id,
                event_kind=event_kind,
                final=True,
            )
            self._adapters[key] = adapter
        return adapter


__all__ = ("DirectInputAdapterCache",)
