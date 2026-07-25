#20260725_kpopmodder: Added stop bridge action handler.
from __future__ import annotations

from typing import Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider


class MinecraftStopAction:
    def __init__(self, client_provider: MinecraftBridgeClientProvider):
        self.client_provider = client_provider

    def run(self) -> Dict[str, object]:
        return self.client_provider.client.stop()
