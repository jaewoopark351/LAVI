#20260818_kpopmodder: Build Fabric ChatClef command submission result DTOs.
from __future__ import annotations

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)


class FabricChatClefCommandResultFactory:
    def rejected(self, request, error_code, message) -> CommandResultDTO:
        return CommandResultDTO(
            request_id=request.request_id,
            ok=False,
            status=CommandResultStatus.REJECTED,
            error_code=error_code,
            message=message,
            data={},
        )

    def unknown(self, request, command_context, error) -> CommandResultDTO:
        return CommandResultDTO(
            request_id=request.request_id,
            ok=False,
            status=CommandResultStatus.UNKNOWN,
            error_code=BridgeErrorCode.INTERNAL_ERROR,
            message=(
                "Fabric ChatClef command send outcome is unknown; active command "
                "ownership is retained until matching terminal evidence or connection "
                f"reconciliation: {type(error).__name__}: {error}"
            ),
            data={
                "session_id": command_context.session_id,
                "connection_generation": command_context.generation,
                "command_message_id": command_context.command_message_id,
                "submission_outcome": "submission_outcome_unknown",
                "reconciliation_required": True,
                "command_ownership_retained": True,
            },
        )

    def accepted(self, request, command_context) -> CommandResultDTO:
        return CommandResultDTO(
            request_id=request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            error_code=None,
            message="Fabric ChatClef command sent to Java bridge.",
            data={
                "session_id": command_context.session_id,
                "connection_generation": command_context.generation,
                "command_message_id": command_context.command_message_id,
            },
        )
