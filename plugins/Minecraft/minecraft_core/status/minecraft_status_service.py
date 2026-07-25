#20260725_kpopmodder: Added status snapshot service for Minecraft bridge configuration and runtime status.
from __future__ import annotations

from typing import Dict

from ..actions.minecraft_read_action_service import MinecraftReadActionService
from ..minecraft_config import MinecraftConfig
from .minecraft_public_config_snapshot import MinecraftPublicConfigSnapshot
from .minecraft_status_snapshot_builder import MinecraftStatusSnapshotBuilder


class MinecraftStatusService:
    def __init__(
        self,
        config_manager: MinecraftConfig,
        read_action_service: MinecraftReadActionService,
        public_config_snapshot: MinecraftPublicConfigSnapshot | None = None,
        status_snapshot_builder: MinecraftStatusSnapshotBuilder | None = None,
    ):
        self.config_manager = config_manager
        self.read_action_service = read_action_service
        self.public_config_snapshot = public_config_snapshot or MinecraftPublicConfigSnapshot(
            config_manager
        )
        self.status_snapshot_builder = status_snapshot_builder or MinecraftStatusSnapshotBuilder(
            config_manager,
            self.public_config_snapshot,
        )

    def get_status(self) -> Dict[str, object]:
        bridge_status = self.read_action_service.status()
        return self.status_snapshot_builder.build(bridge_status)

    def public_config(self) -> Dict[str, object]:
        return self.public_config_snapshot.build()
