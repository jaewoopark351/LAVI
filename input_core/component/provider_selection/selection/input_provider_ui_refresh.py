#20260905_kpopmodder: Owns refresh of all provider UI surfaces.
from __future__ import annotations


class InputProviderUiRefresh:
    def __init__(self, callback) -> None:
        if not callable(callback):
            raise TypeError("create_all_provider_ui_callback must be callable")
        self._callback = callback

    def refresh(self) -> None:
        self._callback()


__all__ = ("InputProviderUiRefresh",)
