#20260801_kpopmodder: Keep Fabric ChatClef websocket/request ownership out of the server I/O loop.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus


@dataclass(frozen=True)
class FabricChatClefConnectionAdmission:
    accepted: bool
    session_id: str
    generation: int
    reason: str = ""


@dataclass(frozen=True)
class FabricChatClefActiveCommand:
    websocket: Any = field(compare=False, repr=False)
    session_id: str
    generation: int
    request_id: str
    command_message_id: str


class FabricChatClefConnectionOwnership:
    TERMINAL_STATUSES = {
        CommandResultStatus.COMPLETED,
        CommandResultStatus.REJECTED,
        CommandResultStatus.FAILED,
        CommandResultStatus.CANCELLED,
        CommandResultStatus.DEADLINE_EXCEEDED,
        CommandResultStatus.UNKNOWN,
    }

    def __init__(self):
        self._generation = 0
        self._active_websocket: Any = None
        self._active_session_id: str | None = None
        self._active_command: FabricChatClefActiveCommand | None = None
        self._last_command_result: dict[str, Any] | None = None

    @property
    def active_websocket(self) -> Any:
        return self._active_websocket

    @property
    def active_session_id(self) -> str | None:
        return self._active_session_id

    @property
    def active_generation(self) -> int:
        return self._generation if self._active_websocket is not None else 0

    @property
    def active_request_id(self) -> str | None:
        return None if self._active_command is None else self._active_command.request_id

    def is_connected(self) -> bool:
        return self._active_websocket is not None and self._active_session_id is not None

    def is_active_websocket(self, websocket: Any) -> bool:
        return self._active_websocket is websocket

    def try_activate(
        self,
        *,
        websocket: Any,
        session_id: str,
    ) -> FabricChatClefConnectionAdmission:
        if self._active_websocket is not None:
            if (
                self._active_websocket is websocket
                and self._active_session_id == session_id
            ):
                return FabricChatClefConnectionAdmission(
                    accepted=True,
                    session_id=session_id,
                    generation=self._generation,
                )
            return FabricChatClefConnectionAdmission(
                accepted=False,
                session_id=session_id,
                generation=self.active_generation,
                reason=(
                    "Fabric ChatClef bridge already has an active Java websocket "
                    f"session: {self._active_session_id}"
                ),
            )

        self._generation += 1
        self._active_websocket = websocket
        self._active_session_id = session_id
        self._active_command = None
        return FabricChatClefConnectionAdmission(
            accepted=True,
            session_id=session_id,
            generation=self._generation,
        )

    def clear_if_active(self, *, websocket: Any, session_id: str | None) -> bool:
        if self._active_websocket is not websocket:
            return False
        if self._active_session_id != session_id:
            return False
        self.clear()
        return True

    def clear(self) -> None:
        self._active_websocket = None
        self._active_session_id = None
        self._active_command = None

    def begin_command(
        self,
        *,
        request_id: str,
        command_message_id: str,
    ) -> FabricChatClefActiveCommand | None:
        if self._active_websocket is None or self._active_session_id is None:
            return None
        if self._active_command is not None:
            return None
        command = FabricChatClefActiveCommand(
            websocket=self._active_websocket,
            session_id=self._active_session_id,
            generation=self._generation,
            request_id=request_id,
            command_message_id=command_message_id,
        )
        self._active_command = command
        return command

    def clear_command_if_current(self, command: FabricChatClefActiveCommand) -> bool:
        if self._active_command != command:
            return False
        self._active_command = None
        return True

    def accept_result(
        self,
        *,
        websocket: Any,
        envelope: BridgeEnvelopeDTO,
        result: CommandResultDTO,
    ) -> tuple[bool, str]:
        command = self._active_command
        if self._active_websocket is not websocket:
            return False, "result_websocket_not_active"
        if command is None:
            return False, "result_without_active_command"
        if command.generation != self._generation:
            return False, "result_generation_mismatch"
        if envelope.session_id != command.session_id:
            return False, "result_session_mismatch"
        if result.request_id != command.request_id:
            return False, "result_request_mismatch"
        if envelope.correlation_id != command.command_message_id:
            return False, "result_correlation_mismatch"

        self._last_command_result = result.to_dict()
        if result.status in self.TERMINAL_STATUSES:
            self._active_command = None
        return True, "accepted"

    def snapshot(self) -> dict[str, Any]:
        command = self._active_command
        return {
            "active_session_id": self._active_session_id,
            "active_generation": self.active_generation,
            "active_request_id": None if command is None else command.request_id,
            "active_command_message_id": (
                None if command is None else command.command_message_id
            ),
            "last_result": self._last_command_result,
        }
