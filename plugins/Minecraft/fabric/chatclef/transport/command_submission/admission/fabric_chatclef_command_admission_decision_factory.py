#20260905_kpopmodder: Convert admission-stage facts into committed admission decisions.
from __future__ import annotations

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode

from .fabric_chatclef_command_admission_decision import (
    FabricChatClefCommandAdmissionDecision,
)


class FabricChatClefCommandAdmissionDecisionFactory:
    @staticmethod
    def rejected(inspection) -> FabricChatClefCommandAdmissionDecision:
        return FabricChatClefCommandAdmissionDecision(
            request=inspection.request,
            error_code=inspection.error_code or BridgeErrorCode.INTERNAL_ERROR,
            message=inspection.message,
            event=inspection.event,
            details=(
                None if inspection.details is None else dict(inspection.details)
            ),
        )

    @staticmethod
    def begin_failed(request, *, commands) -> FabricChatClefCommandAdmissionDecision:
        return FabricChatClefCommandAdmissionDecision(
            request=request,
            error_code=BridgeErrorCode.NOT_CONNECTED,
            message="Fabric ChatClef bridge client is not connected.",
            event="command_rejected_begin_command_failed",
            details={"commands": commands},
        )

    @staticmethod
    def accepted(
        request,
        *,
        command_context,
        accepted_at_ms: int,
        commands,
    ) -> FabricChatClefCommandAdmissionDecision:
        return FabricChatClefCommandAdmissionDecision(
            request=request,
            command_context=command_context,
            event="command_accepted",
            details={
                "session_id": command_context.session_id,
                "connection_generation": command_context.generation,
                "command_message_id": command_context.command_message_id,
                "accepted_at_ms": accepted_at_ms,
                "commands": commands,
            },
        )
