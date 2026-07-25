#20260725_kpopmodder: Added public config snapshot builder for Minecraft bridge status output.
from __future__ import annotations

from typing import Dict

from ..minecraft_config import MinecraftConfig


class MinecraftPublicConfigSnapshot:
    def __init__(self, config_manager: MinecraftConfig):
        self.config_manager = config_manager

    def build(self) -> Dict[str, object]:
        return {
            "enabled": self.config_manager.get_bool("enabled", True),
            "allow_actions": self.config_manager.get_bool("allow_actions", True),
            "bridge_base_url": self.config_manager.bridge_base_url(),
            "timeout_sec": self.config_manager.request_timeout_sec(),
            "config_path": self.config_manager.config_path,
        }
