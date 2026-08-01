#20260801_kpopmodder: Own the Fabric ChatClef Python bridge lifecycle facade.
from __future__ import annotations

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.config.fabric_chatclef_config import (
    FabricChatClefConfig,
)
from plugins.Minecraft.fabric.chatclef.diagnostics import FabricChatClefDiagnostics
from plugins.Minecraft.fabric.chatclef.session import FabricChatClefSessionRegistry
from plugins.Minecraft.fabric.chatclef.transport import FabricChatClefWebSocketServer


class FabricChatClefAdapter:
    BACKEND_ID = "fabric_chatclef"
    DISABLED_DETAIL = "Fabric ChatClef bridge is disabled."

    def __init__(
        self,
        config: FabricChatClefConfig | None = None,
        session_registry: FabricChatClefSessionRegistry | None = None,
        diagnostics: FabricChatClefDiagnostics | None = None,
        server: FabricChatClefWebSocketServer | None = None,
    ):
        self._config = config or FabricChatClefConfig()
        self._session_registry = session_registry or FabricChatClefSessionRegistry()
        self._diagnostics = diagnostics or FabricChatClefDiagnostics()
        self._server = server or FabricChatClefWebSocketServer(
            self._config,
            self._session_registry,
            self._diagnostics,
        )

    @property
    def backend_id(self) -> str:
        return self.BACKEND_ID

    def start(self) -> None:
        if not self._config.enabled:
            self._diagnostics.info("server not started because bridge is disabled")
            return
        self._server.start()

    def stop(self) -> None:
        self._server.stop()

    def submit_command(self, request: CommandRequestDTO) -> CommandResultDTO:
        command_request = CommandRequestDTO.from_mapping(request)
        if not self._config.enabled:
            return self._reject(
                command_request,
                BridgeErrorCode.BRIDGE_DISABLED,
                self.DISABLED_DETAIL,
            )
        if self._session_registry.active_session() is None:
            return self._reject(
                command_request,
                BridgeErrorCode.NOT_CONNECTED,
                "Fabric ChatClef bridge client is not connected.",
            )
        return self._server.submit_command(command_request)

    def get_status(self) -> StatusSnapshotDTO:
        return self._server.status_snapshot(enabled=self._config.enabled)

    def _reject(
        self,
        request: CommandRequestDTO,
        error_code: BridgeErrorCode,
        message: str,
    ) -> CommandResultDTO:
        return CommandResultDTO(
            request_id=request.request_id,
            ok=False,
            status=CommandResultStatus.REJECTED,
            error_code=error_code,
            message=message,
            data={},
        )
