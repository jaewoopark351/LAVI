#20260821_kpopmodder: Represent released command identity without depending on dataclass equality.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)


@dataclass(frozen=True)
class ReconciliationIdentity:
    session_id: str
    connection_generation: int
    request_id: str
    command_message_id: str
    canonical_command: str
    source: str

    @classmethod
    def from_active(
        cls,
        command: FabricChatClefActiveCommand,
        *,
        canonical_command: str | None = None,
    ) -> "ReconciliationIdentity":
        return cls(
            session_id=command.session_id,
            connection_generation=command.generation,
            request_id=command.request_id,
            command_message_id=command.command_message_id,
            canonical_command=canonical_command or command.command,
            source=command.source,
        )

    def matches_result(
        self,
        *,
        session_id: str | None,
        correlation_id: str | None,
        request_id: str,
        data: Mapping[str, Any],
    ) -> bool:
        return (
            session_id == self.session_id
            and correlation_id == self.command_message_id
            and request_id == self.request_id
            and _connection_generation(data) == self.connection_generation
        )

    def to_dict(self) -> dict[str, object]:
        return {
            "session_id": self.session_id,
            "connection_generation": self.connection_generation,
            "request_id": self.request_id,
            "command_message_id": self.command_message_id,
            "canonical_command": self.canonical_command,
            "source": self.source,
        }


def _connection_generation(data: Mapping[str, Any]) -> int | None:
    for key in ("connection_generation", "active_generation"):
        value = data.get(key)
        if type(value) is int:
            return value
    ownership = data.get("ownership")
    if isinstance(ownership, Mapping):
        for key in ("connection_generation", "active_generation"):
            value = ownership.get(key)
            if type(value) is int:
                return value
    return None

