#20260905_kpopmodder: Inspect ordinary-command admission without mutating command ownership.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode

from .fabric_chatclef_ordinary_admission_inspection import (
    FabricChatClefOrdinaryAdmissionInspection,
)
from .korean_submission_context_validator import KoreanSubmissionContextValidator


class FabricChatClefOrdinaryAdmissionInspector:
    def __init__(
        self,
        *,
        connection_ownership,
        now_ms,
        stop_control_admission_barrier=None,
        catalogue_provider=None,
    ) -> None:
        self._connection_ownership = connection_ownership
        self._now_ms = now_ms
        self._stop_control_admission_barrier = stop_control_admission_barrier
        self._korean_context = KoreanSubmissionContextValidator(connection_ownership, catalogue_provider)

    def inspect(
        self,
        request: CommandRequestDTO,
        *,
        loop: Any,
    ) -> FabricChatClefOrdinaryAdmissionInspection:
        if (
            self._stop_control_admission_barrier is not None
            and self._stop_control_admission_barrier.closed
        ):
            return FabricChatClefOrdinaryAdmissionInspection(
                request=request,
                allowed=False,
                error_code=BridgeErrorCode.INVALID_REQUEST,
                message="A Minecraft stop control is still in flight.",
                event="command_rejected_stop_control_barrier",
                details={"stop_control_barrier": "closed"},
            )
        if (
            not self._connection_ownership.is_connected()
            or loop is None
            or not loop.is_running()
        ):
            return FabricChatClefOrdinaryAdmissionInspection(
                request=request,
                allowed=False,
                error_code=BridgeErrorCode.NOT_CONNECTED,
                message="Fabric ChatClef bridge client is not connected.",
                event="command_rejected_not_connected",
                details={
                    "loop_present": loop is not None,
                    "loop_running": loop is not None and loop.is_running(),
                    "commands": (
                        self._connection_ownership.local_admission_snapshot()
                    ),
                },
            )
        #20260915_kpopmodder: A receipt must not cross a reconnect or catalogue refresh.
        reason = self._korean_context.rejection_reason(request)
        if reason is not None:
            return FabricChatClefOrdinaryAdmissionInspection(
                request=request, allowed=False, error_code=BridgeErrorCode.INVALID_REQUEST,
                message="Minecraft 연결 또는 대상 정보가 바뀌었어. 요청을 다시 말해줘.",
                event="command_rejected_korean_context_changed",
                details={"reason": reason, "active_generation": self._connection_ownership.active_generation},
            )
        commands = self._connection_ownership.local_admission_snapshot()
        quarantine = _active_quarantine(commands)
        active_request_id = commands.get("active_request_id")
        if quarantine is not None:
            return FabricChatClefOrdinaryAdmissionInspection(
                request=request,
                allowed=False,
                error_code=BridgeErrorCode.INVALID_REQUEST,
                message=(
                    "Fabric ChatClef command admission is quarantined "
                    "until Java command retirement is verified."
                ),
                event="command_rejected_quarantined",
                details={
                    "rejected_at_ms": self._now_ms(),
                    "quarantine": quarantine,
                    "commands": commands,
                },
            )
        if active_request_id is not None:
            return FabricChatClefOrdinaryAdmissionInspection(
                request=request,
                allowed=False,
                error_code=BridgeErrorCode.INVALID_REQUEST,
                message=(
                    "Fabric ChatClef command already active: "
                    f"{active_request_id}"
                ),
                event="command_rejected_already_active",
                details={
                    "active_request_id": active_request_id,
                    "rejected_at_ms": self._now_ms(),
                    "commands": commands,
                },
            )
        return FabricChatClefOrdinaryAdmissionInspection(
            request=request,
            allowed=True,
        )


def _active_quarantine(commands: Mapping[str, Any]) -> dict[str, Any] | None:
    quarantine = commands.get("admission_quarantine")
    if not isinstance(quarantine, Mapping):
        return None
    if quarantine.get("active") is not True:
        return None
    return dict(quarantine)
