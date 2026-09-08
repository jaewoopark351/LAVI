#20260801_kpopmodder: Own the Fabric ChatClef Python bridge lifecycle facade.
#20260905_kpopmodder: Expose the guarded STOP control facade without changing backend ownership.
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

    #20260905_kpopmodder: Keep STOP on the Fabric-only control lane.
    def submit_stop_control(
        self,
        *,
        event: object,
        eligibility_proof: object,
        receipt: object,
    ):
        return self._server.submit_stop_control(
            event=event,
            eligibility_proof=eligibility_proof,
            receipt=receipt,
        )

    def get_stop_control_claim_registry(self):
        getter = getattr(self._server, "get_stop_control_claim_registry", None)
        return getter() if callable(getter) else None

    def set_stop_terminal_response_callback(self, callback) -> None:
        self._server.set_stop_terminal_response_callback(callback)

    #20260907_kpopmodder: Expose only the Fabric crafting-feedback server facade.
    def reserve_crafting_feedback(self, grant: object) -> bool:
        return self.reserve_command_feedback(grant)

    def abandon_crafting_feedback(self, grant: object) -> bool:
        return self.abandon_command_feedback(grant)

    def claim_crafting_feedback_start(
        self,
        grant: object,
        result: object,
    ):
        return self.claim_command_feedback_start(grant, result)

    def inspect_crafting_feedback_status(self, target_item: str | None):
        return self._server.inspect_crafting_feedback_status(target_item)

    def set_crafting_terminal_response_callback(self, callback) -> None:
        self.set_command_lifecycle_terminal_response_callback(callback)

    def reserve_command_feedback(self, grant: object) -> bool:
        return self._server.reserve_command_feedback(grant)

    def abandon_command_feedback(self, grant: object) -> bool:
        return self._server.abandon_command_feedback(grant)

    def claim_command_feedback_start(self, grant: object, result: object):
        return self._server.claim_command_feedback_start(grant, result)

    def inspect_command_feedback_status(self, query: object):
        return self._server.inspect_command_feedback_status(query)

    def set_command_lifecycle_terminal_response_callback(self, callback) -> None:
        self._server.set_command_lifecycle_terminal_response_callback(callback)

    def set_command_terminal_response_callback(self, callback) -> None:
        self.set_command_lifecycle_terminal_response_callback(callback)

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
