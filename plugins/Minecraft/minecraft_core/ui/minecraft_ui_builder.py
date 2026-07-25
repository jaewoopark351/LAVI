#20260725_kpopmodder: Added Gradio UI builder for the Minecraft bridge tab.
from __future__ import annotations

from .minecraft_ui_controller import MinecraftUiController
from .minecraft_ui_event_binder import MinecraftUiEventBinder
from .minecraft_ui_layout import MinecraftUiLayout


class MinecraftUiBuilder:
    def __init__(
        self,
        controller: MinecraftUiController,
        layout: MinecraftUiLayout | None = None,
        event_binder: MinecraftUiEventBinder | None = None,
    ):
        self.controller = controller
        self.layout = layout or MinecraftUiLayout()
        self.event_binder = event_binder or MinecraftUiEventBinder(controller)

    def create_ui(self) -> None:
        components = self.layout.create(self.controller.initial_status_text())
        self.event_binder.bind(components)
