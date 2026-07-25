#20260725_kpopmodder: Added read-only Minecraft bridge action service.
from __future__ import annotations

from typing import Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider


class MinecraftReadActionService:
    def __init__(self, client_provider: MinecraftBridgeClientProvider):
        self.client_provider = client_provider

    def health(self) -> Dict[str, object]:
        return self.client_provider.client.health()

    def status(self) -> Dict[str, object]:
        return self.client_provider.client.status()

    def inventory(self) -> Dict[str, object]:
        return self.client_provider.client.inventory()

    def current_action(self) -> Dict[str, object]:
        return self.client_provider.client.current_action()
