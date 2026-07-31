#20260801_kpopmodder: Add a fail-closed Fabric ChatClef adapter skeleton for Phase 1.
from __future__ import annotations

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import BridgeLifecycleState
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.config.fabric_chatclef_config import (
    FabricChatClefConfig,
)


class FabricChatClefAdapter:
    BACKEND_ID = "fabric_chatclef"
    PHASE_1_DETAIL = "Fabric ChatClef bridge is not implemented in Phase 1."

    def __init__(self, config: FabricChatClefConfig | None = None):
        self._config = config or FabricChatClefConfig()

    @property
    def backend_id(self) -> str:
        return self.BACKEND_ID

    def start(self) -> None:
        return None

    def stop(self) -> None:
        return None

    def submit_command(self, request: CommandRequestDTO) -> CommandResultDTO:
        command_request = CommandRequestDTO.from_mapping(request)
        return CommandResultDTO(
            request_id=command_request.request_id,
            ok=False,
            status=CommandResultStatus.REJECTED,
            error_code=BridgeErrorCode.NOT_IMPLEMENTED,
            message=self.PHASE_1_DETAIL,
            data={},
        )

    def get_status(self) -> StatusSnapshotDTO:
        return StatusSnapshotDTO(
            backend_id=self.BACKEND_ID,
            enabled=self._config.enabled,
            connected=False,
            lifecycle_state=BridgeLifecycleState.NOT_IMPLEMENTED,
            detail=self.PHASE_1_DETAIL,
            details={},
            last_error_code=None,
            last_error_message=None,
        )
