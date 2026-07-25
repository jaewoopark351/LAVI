#20260725_kpopmodder: Added get-and-equip bridge action handler for collect-then-select behavior.
from __future__ import annotations

from typing import Any, Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from ..minecraft_config import MinecraftConfig


class MinecraftGetAndEquipAction:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        client_provider: MinecraftBridgeClientProvider,
    ):
        self.config_manager = config_manager
        self.client_provider = client_provider

    def run(self, item: Any, count: Any = 1) -> Dict[str, object]:
        if not self._actions_allowed():
            return {
                "ok": False,
                "action": "get_and_equip",
                "error": "actions_disabled",
                "message": "Minecraft actions are disabled in config.",
            }
        item_name = str(item or "").strip()
        if not item_name:
            return {
                "ok": False,
                "action": "get_and_equip",
                "error": "missing_item",
                "message": "item is required.",
            }
        return self.client_provider.client.get_and_equip(
            item_name,
            self._coerce_count(count),
        )

    def _actions_allowed(self) -> bool:
        return (
            self.config_manager.get_bool("enabled", True)
            and self.config_manager.get_bool("allow_actions", True)
        )

    def _coerce_count(self, value: Any) -> int:
        try:
            count = int(float(value))
        except (TypeError, ValueError):
            count = 1
        return max(1, count)
