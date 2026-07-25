#20260725_kpopmodder: Added get-item bridge action handler.
from __future__ import annotations

from typing import Any, Dict

from ..bridge.minecraft_bridge_client_provider import MinecraftBridgeClientProvider
from ..minecraft_config import MinecraftConfig
from .minecraft_item_count_action_validator import MinecraftItemCountActionValidator
from .minecraft_verified_item_action_runner import MinecraftVerifiedItemActionRunner
from .minecraft_write_action_permission_guard import MinecraftWriteActionPermissionGuard


class MinecraftGetItemAction:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        client_provider: MinecraftBridgeClientProvider,
        permission_guard: MinecraftWriteActionPermissionGuard | None = None,
        validator: MinecraftItemCountActionValidator | None = None,
        verified_runner: MinecraftVerifiedItemActionRunner | None = None,
    ):
        self.client_provider = client_provider
        self.permission_guard = permission_guard or MinecraftWriteActionPermissionGuard(
            config_manager
        )
        self.validator = validator or MinecraftItemCountActionValidator()
        self.verified_runner = verified_runner or MinecraftVerifiedItemActionRunner(
            config_manager,
            client_provider,
        )

    def run(self, item: Any, count: Any = 1) -> Dict[str, object]:
        blocked = self.permission_guard.blocked_response("get_item")
        if blocked is not None:
            return blocked

        request = self.validator.validate("get_item", item, count)
        if not request.get("ok"):
            return request

        return self.verified_runner.run(
            action="get_item",
            item=request["item"],
            count=request["count"],
            submit=lambda: self.client_provider.client.get_item(
                request["item"],
                request["count"],
            ),
        )
