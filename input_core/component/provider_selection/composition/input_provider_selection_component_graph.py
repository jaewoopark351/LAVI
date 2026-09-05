#20260905_kpopmodder: Composes provider selection, UI, and synchronization effects.
from __future__ import annotations

from ..selection import (
    InputProviderListenerSynchronizer,
    InputProviderSelectionMutation,
    InputProviderUiRefresh,
)


class InputProviderSelectionComponentGraph:
    def __init__(
        self,
        *,
        create_all_provider_ui_callback,
        select_provider_callback,
        sync_provider_listeners_callback,
    ) -> None:
        self.ui_refresh = InputProviderUiRefresh(
            create_all_provider_ui_callback
        )
        self.selection_mutation = InputProviderSelectionMutation(
            select_provider_callback
        )
        self.listener_synchronizer = InputProviderListenerSynchronizer(
            sync_provider_listeners_callback
        )


__all__ = ("InputProviderSelectionComponentGraph",)
