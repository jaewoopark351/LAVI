#20260905_kpopmodder: Owns provider-listener synchronization after UI changes.
from __future__ import annotations


class InputProviderListenerSynchronizer:
    def __init__(self, callback) -> None:
        if not callable(callback):
            raise TypeError("sync_provider_listeners_callback must be callable")
        self._callback = callback

    def synchronize(self) -> None:
        self._callback()


__all__ = ("InputProviderListenerSynchronizer",)
