#20260801_kpopmodder: Compose the Fabric ChatClef adapter without GUI or Java runtime wiring.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter import (
    FabricChatClefAdapter,
)
from plugins.Minecraft.fabric.chatclef.config import (
    FabricChatClefConfig,
    FabricChatClefConfigLoader,
)
from plugins.Minecraft.fabric.chatclef.ui import FabricChatClefPanel


class MinecraftFabricChatClefPlugin:
    def __init__(
        self,
        config: FabricChatClefConfig | Mapping[str, Any] | None = None,
        config_loader: FabricChatClefConfigLoader | None = None,
    ):
        self._config_loader = config_loader or FabricChatClefConfigLoader()
        self._config = self._normalize_config(config)
        self._adapter: FabricChatClefAdapter | None = None

    @property
    def config(self) -> FabricChatClefConfig:
        return self._config

    def create_adapter(self) -> FabricChatClefAdapter:
        if self._adapter is None:
            self._adapter = FabricChatClefAdapter(config=self._config)
        return self._adapter

    def get_status(self) -> dict[str, Any]:
        status = self.create_adapter().get_status()
        return {
            "enabled": self._config.enabled,
            "backend_id": status.backend_id,
            "endpoint": self._config.endpoint,
            "bridge": status.to_dict(),
        }

    def create_ui(self, extension: Any = None) -> None:
        FabricChatClefPanel(plugin=self, extension=extension).create_ui()

    def _normalize_config(
        self,
        config: FabricChatClefConfig | Mapping[str, Any] | None,
    ) -> FabricChatClefConfig:
        if isinstance(config, FabricChatClefConfig):
            return config
        return self._config_loader.from_mapping(config)
