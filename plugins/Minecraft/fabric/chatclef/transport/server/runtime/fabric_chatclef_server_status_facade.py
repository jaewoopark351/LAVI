#20260905_kpopmodder: Expose only local and wire Fabric ChatClef server status views.
from __future__ import annotations

from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO


class FabricChatClefServerStatusFacade:
    def __init__(self, *, status_builder, lifecycle) -> None:
        self._status_builder = status_builder
        self._lifecycle = lifecycle

    def local_snapshot(self, *, enabled: bool) -> StatusSnapshotDTO:
        return self._build(enabled=enabled, commands_view="local")

    def wire_snapshot(self, *, enabled: bool) -> StatusSnapshotDTO:
        return self._build(enabled=enabled, commands_view="wire")

    def _build(self, *, enabled: bool, commands_view: str) -> StatusSnapshotDTO:
        return self._status_builder.build(
            enabled=enabled,
            last_error=self._lifecycle.last_error,
            is_running=self._lifecycle.is_running,
            endpoint=self._lifecycle.endpoint,
            bound_host=self._lifecycle.bound_host,
            bound_port=self._lifecycle.bound_port,
            commands_view=commands_view,
        )
