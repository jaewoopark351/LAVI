#20260725_kpopmodder: Added write-action permission guard so new Minecraft actions do not inline config checks.
from __future__ import annotations

from typing import Dict

from ..minecraft_config import MinecraftConfig


class MinecraftWriteActionPermissionGuard:
    def __init__(self, config_manager: MinecraftConfig):
        self.config_manager = config_manager

    def blocked_response(self, action: str) -> Dict[str, object] | None:
        if self.config_manager.get_bool("enabled", True) and self.config_manager.get_bool(
            "allow_actions",
            True,
        ):
            return None
        return {
            "ok": False,
            "action": action,
            "error": "actions_disabled",
            "message": "Minecraft actions are disabled in config.",
        }
