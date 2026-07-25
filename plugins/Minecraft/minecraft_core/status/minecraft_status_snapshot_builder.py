#20260725_kpopmodder: Added status snapshot builder so bridge calls and response shape are separate.
from __future__ import annotations

from typing import Dict

from ..minecraft_config import MinecraftConfig
from .minecraft_public_config_snapshot import MinecraftPublicConfigSnapshot


class MinecraftStatusSnapshotBuilder:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        public_config_snapshot: MinecraftPublicConfigSnapshot,
    ):
        self.config_manager = config_manager
        self.public_config_snapshot = public_config_snapshot

    def build(self, bridge_status: Dict[str, object]) -> Dict[str, object]:
        return {
            "ok": bool(bridge_status.get("ok", False)),
            "name": "minecraft",
            "enabled": self.config_manager.get_bool("enabled", True),
            "allow_actions": self.config_manager.get_bool("allow_actions", True),
            "config_message": self.config_manager.config_message(),
            "config": self.public_config_snapshot.build(),
            "bridge": bridge_status,
        }
