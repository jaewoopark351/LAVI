#20260725_kpopmodder: Added craft bridge action handler for creating craftable Minecraft items.
from __future__ import annotations

from typing import Any, Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from ..minecraft_config import MinecraftConfig
from .minecraft_item_count_action_validator import MinecraftItemCountActionValidator
from .minecraft_write_action_permission_guard import MinecraftWriteActionPermissionGuard


class MinecraftCraftAction:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        client_provider: MinecraftBridgeClientProvider,
        permission_guard: MinecraftWriteActionPermissionGuard | None = None,
        validator: MinecraftItemCountActionValidator | None = None,
    ):
        self.client_provider = client_provider
        self.permission_guard = permission_guard or MinecraftWriteActionPermissionGuard(
            config_manager
        )
        self.validator = validator or MinecraftItemCountActionValidator()

    def run(self, item: Any, count: Any = 1) -> Dict[str, object]:
        blocked = self.permission_guard.blocked_response("craft")
        if blocked is not None:
            return blocked

        request = self.validator.validate("craft", item, count)
        if not request.get("ok"):
            return request

        return self.client_provider.client.craft(
            request["item"],
            request["count"],
        )
