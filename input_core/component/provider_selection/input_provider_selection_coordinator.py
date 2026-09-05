#20260905_kpopmodder: Sequences focused provider-selection effects.
from __future__ import annotations

from .composition import InputProviderSelectionComponentGraph


class InputProviderSelectionCoordinator:
    def __init__(
        self,
        *,
        create_all_provider_ui_callback,
        select_provider_callback,
        sync_provider_listeners_callback,
    ) -> None:
        self._components = InputProviderSelectionComponentGraph(
            create_all_provider_ui_callback=create_all_provider_ui_callback,
            select_provider_callback=select_provider_callback,
            sync_provider_listeners_callback=sync_provider_listeners_callback,
        )
        self._create_all_provider_ui_callback = (
            create_all_provider_ui_callback
        )
        self._select_provider_callback = select_provider_callback
        self._sync_provider_listeners_callback = (
            sync_provider_listeners_callback
        )

    def create_all_provider_ui(self) -> None:
        self._components.ui_refresh.refresh()
        self._components.listener_synchronizer.synchronize()

    def select_provider(self, provider_name):
        selected_name = self._components.selection_mutation.select(provider_name)
        self._components.listener_synchronizer.synchronize()
        return selected_name


__all__ = ("InputProviderSelectionCoordinator",)
