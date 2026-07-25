#20260725_kpopmodder: Added bridge client provider to isolate Minecraft client lifecycle from action services.
from __future__ import annotations

from .chatclef_bridge_client import ChatClefBridgeClient
from ..minecraft_config import MinecraftConfig


class MinecraftBridgeClientProvider:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        client_factory=None,
    ):
        self.config_manager = config_manager
        self.client_factory = client_factory or ChatClefBridgeClient
        self.client = self._build_client()

    def reload(self):
        self.client = self._build_client()
        return self.client

    def _build_client(self):
        return self.client_factory(
            base_url=self.config_manager.bridge_base_url(),
            timeout_sec=self.config_manager.request_timeout_sec(),
        )
