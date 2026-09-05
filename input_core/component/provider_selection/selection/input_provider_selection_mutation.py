#20260905_kpopmodder: Owns mutation of the active input-provider selection.
from __future__ import annotations


class InputProviderSelectionMutation:
    def __init__(self, callback) -> None:
        if not callable(callback):
            raise TypeError("select_provider_callback must be callable")
        self._callback = callback

    def select(self, provider_name):
        return self._callback(provider_name)


__all__ = ("InputProviderSelectionMutation",)
