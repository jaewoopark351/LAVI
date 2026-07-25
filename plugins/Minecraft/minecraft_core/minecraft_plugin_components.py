#20260725_kpopmodder: Added plugin composition object so facade proxies stay simple.
from __future__ import annotations

from .minecraft_config import MinecraftConfig
from .minecraft_facade_service import MinecraftFacadeService
from .ui.minecraft_ui_builder import MinecraftUiBuilder
from .ui.minecraft_ui_controller import MinecraftUiController


class MinecraftPluginComponents:
    def __init__(self):
        self.config_manager = MinecraftConfig()
        self.facade_service = MinecraftFacadeService(self.config_manager)
        self.ui_controller = MinecraftUiController(
            self.config_manager,
            self.facade_service,
        )
        self.ui_builder = MinecraftUiBuilder(self.ui_controller)
