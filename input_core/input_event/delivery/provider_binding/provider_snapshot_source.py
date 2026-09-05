#20260905_kpopmodder: Owns snapshots from the loader-provided provider inventory callback.
from __future__ import annotations


class ProviderSnapshotSource:
    """Return an isolated snapshot of the currently loaded input providers."""

    def __init__(self, provider_list_callback) -> None:
        self._provider_list_callback = provider_list_callback

    def snapshot(self) -> list:
        return list(self._provider_list_callback())


__all__ = ("ProviderSnapshotSource",)
