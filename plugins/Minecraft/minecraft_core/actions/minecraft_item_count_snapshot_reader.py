#20260725_kpopmodder: Added this reader to capture inventory count snapshots for action verification.
from __future__ import annotations

from typing import Any, Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from .minecraft_inventory_count_reader import MinecraftInventoryCountReader


class MinecraftItemCountSnapshotReader:
    def __init__(
        self,
        client_provider: MinecraftBridgeClientProvider,
        count_reader: MinecraftInventoryCountReader | None = None,
    ):
        self.client_provider = client_provider
        self.count_reader = count_reader or MinecraftInventoryCountReader()

    def read(self, item: str) -> Dict[str, Any]:
        try:
            inventory = self.client_provider.client.inventory()
        except Exception as error:
            return {
                "ok": False,
                "error": "inventory_unavailable",
                "message": str(error),
            }
        if not isinstance(inventory, dict):
            return {
                "ok": False,
                "error": "invalid_inventory_response",
                "message": "Minecraft inventory response must be an object.",
            }
        if not inventory.get("ok"):
            return {
                "ok": False,
                "error": inventory.get("error", "inventory_unavailable"),
                "message": inventory.get("message", "Minecraft inventory is unavailable."),
                "inventory": inventory,
            }
        return {
            "ok": True,
            "item": item,
            "count": self.count_reader.count(inventory, item),
            "inventory": inventory,
        }
