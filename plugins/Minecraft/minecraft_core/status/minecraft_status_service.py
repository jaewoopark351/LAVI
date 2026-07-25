#20260725_kpopmodder: Added status snapshot service for Minecraft bridge configuration and runtime status.
from __future__ import annotations

from typing import Dict

from ..actions.minecraft_read_action_service import MinecraftReadActionService
from ..minecraft_config import MinecraftConfig


class MinecraftStatusService:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        read_action_service: MinecraftReadActionService,
    ):
        self.config_manager = config_manager
        self.read_action_service = read_action_service

    def get_status(self) -> Dict[str, object]:
        bridge_status = self.read_action_service.status()
        return {
            "ok": bool(bridge_status.get("ok", False)),
            "name": "minecraft",
            "enabled": self.config_manager.get_bool("enabled", True),
            "allow_actions": self.config_manager.get_bool("allow_actions", True),
            "config_message": self.config_manager.config_message(),
            "config": self.public_config(),
            "bridge": bridge_status,
        }

    def public_config(self) -> Dict[str, object]:
        return {
            "enabled": self.config_manager.get_bool("enabled", True),
            "allow_actions": self.config_manager.get_bool("allow_actions", True),
            "bridge_base_url": self.config_manager.bridge_base_url(),
            "timeout_sec": self.config_manager.request_timeout_sec(),
            "config_path": self.config_manager.config_path,
        }
