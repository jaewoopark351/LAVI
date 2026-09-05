#20260905_kpopmodder: Owns Input tab construction and provider UI callback binding.
from __future__ import annotations

import gradio as gr


class InputComponentUiBuilder:
    def __init__(
        self,
        *,
        create_provider_selection_ui_callback,
        create_all_provider_ui_callback,
        sync_provider_listeners_callback,
    ) -> None:
        callbacks = (
            create_provider_selection_ui_callback,
            create_all_provider_ui_callback,
            sync_provider_listeners_callback,
        )
        if not all(callable(callback) for callback in callbacks):
            raise TypeError("Input UI callbacks must be callable")
        self._create_provider_selection_ui_callback = (
            create_provider_selection_ui_callback
        )
        self._create_all_provider_ui_callback = (
            create_all_provider_ui_callback
        )
        self._sync_provider_listeners_callback = (
            sync_provider_listeners_callback
        )

    def build(self) -> None:
        with gr.Tab("Input"):
            with gr.Blocks():
                self._create_provider_selection_ui_callback()
            self._create_all_provider_ui_callback()
            self._sync_provider_listeners_callback()


__all__ = ("InputComponentUiBuilder",)
