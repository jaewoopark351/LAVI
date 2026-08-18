#20260818_kpopmodder: Build one Fabric ChatClef server status snapshot.
from __future__ import annotations

from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import (
    BridgeLifecycleState,
)


class FabricChatClefStatusSnapshotBuilder:
    def __init__(self, *, connection_ownership, command_lock, session_registry):
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock
        self._session_registry = session_registry

    def build(
        self,
        *,
        enabled: bool,
        last_error: str | None,
        is_running: bool,
        endpoint: str,
        bound_host: str,
        bound_port: int,
    ) -> StatusSnapshotDTO:
        with self._command_lock:
            connected = self._connection_ownership.is_connected()
            commands = self._connection_ownership.snapshot()
        if not enabled:
            state = BridgeLifecycleState.DISABLED
            detail = "Fabric ChatClef bridge is disabled."
        elif last_error:
            state = BridgeLifecycleState.FAILED
            detail = "Fabric ChatClef WebSocket server failed."
        elif connected:
            state = BridgeLifecycleState.CONNECTED
            detail = "Fabric ChatClef bridge client is connected."
        elif is_running:
            state = BridgeLifecycleState.DISCONNECTED
            detail = "Fabric ChatClef WebSocket server is waiting for client."
        else:
            state = BridgeLifecycleState.STOPPED
            detail = "Fabric ChatClef WebSocket server is stopped."
        return StatusSnapshotDTO(
            backend_id="fabric_chatclef",
            enabled=enabled,
            connected=connected,
            lifecycle_state=state,
            detail=detail,
            details={
                "endpoint": endpoint,
                "host": bound_host,
                "port": bound_port,
                "sessions": self._session_registry.snapshot(),
                "commands": commands,
            },
            last_error_code=BridgeErrorCode.INTERNAL_ERROR if last_error else None,
            last_error_message=last_error,
        )
