#20260818_kpopmodder: Own Fabric ChatClef connection and active-command admission.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode

from .fabric_chatclef_command_request_normalizer import normalize_command_request


@dataclass(frozen=True)
class FabricChatClefCommandAdmissionDecision:
    request: CommandRequestDTO
    command_context: Any = None
    error_code: BridgeErrorCode | None = None
    message: str = ""
    event: str = ""
    details: dict[str, Any] | None = None

    @property
    def accepted(self) -> bool:
        return self.command_context is not None and self.error_code is None


class FabricChatClefCommandAdmission:
    def __init__(self, *, connection_ownership, command_lock, now_ms):
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock
        self._now_ms = now_ms

    def inspect(
        self,
        request: CommandRequestDTO,
        *,
        message_id: str,
        loop: Any,
    ) -> FabricChatClefCommandAdmissionDecision:
        command_request = normalize_command_request(request)
        with self._command_lock:
            if (
                not self._connection_ownership.is_connected()
                or loop is None
                or not loop.is_running()
            ):
                return FabricChatClefCommandAdmissionDecision(
                    request=command_request,
                    error_code=BridgeErrorCode.NOT_CONNECTED,
                    message="Fabric ChatClef bridge client is not connected.",
                    event="command_rejected_not_connected",
                    details={
                        "loop_present": loop is not None,
                        "loop_running": loop is not None and loop.is_running(),
                        "commands": self._connection_ownership.snapshot(),
                    },
                )
            active_request_id = self._connection_ownership.active_request_id
            if active_request_id is not None:
                return FabricChatClefCommandAdmissionDecision(
                    request=command_request,
                    error_code=BridgeErrorCode.INVALID_REQUEST,
                    message=(
                        "Fabric ChatClef command already active: "
                        f"{active_request_id}"
                    ),
                    event="command_rejected_already_active",
                    details={
                        "active_request_id": active_request_id,
                        "rejected_at_ms": self._now_ms(),
                        "commands": self._connection_ownership.snapshot(),
                    },
                )
            context = self._connection_ownership.begin_command(
                request_id=command_request.request_id,
                command_message_id=message_id,
                command=command_request.command,
                source=command_request.source,
            )
            if context is None:
                return FabricChatClefCommandAdmissionDecision(
                    request=command_request,
                    error_code=BridgeErrorCode.NOT_CONNECTED,
                    message="Fabric ChatClef bridge client is not connected.",
                    event="command_rejected_begin_command_failed",
                    details={"commands": self._connection_ownership.snapshot()},
                )
            return FabricChatClefCommandAdmissionDecision(
                request=command_request,
                command_context=context,
                event="command_accepted",
                details={
                    "session_id": context.session_id,
                    "connection_generation": context.generation,
                    "command_message_id": context.command_message_id,
                    "accepted_at_ms": self._now_ms(),
                    "commands": self._connection_ownership.snapshot(),
                },
            )

    def release_if_not_scheduled(self, command_context: Any) -> dict[str, Any]:
        with self._command_lock:
            self._connection_ownership.clear_command_if_current(command_context)
            return self._connection_ownership.snapshot()

    def snapshot(self) -> dict[str, Any]:
        with self._command_lock:
            return self._connection_ownership.snapshot()
