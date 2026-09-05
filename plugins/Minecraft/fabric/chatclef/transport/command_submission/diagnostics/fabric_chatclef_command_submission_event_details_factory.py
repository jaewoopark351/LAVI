#20260905_kpopmodder: Build only event-specific ordinary-command diagnostic details.
from __future__ import annotations

from typing import Any


class FabricChatClefCommandSubmissionEventDetailsFactory:
    def __init__(self, *, now_ms) -> None:
        self._now_ms = now_ms

    @staticmethod
    def delivery_error(*, error: Exception | None, commands) -> dict[str, Any]:
        return {
            "error_type": (
                type(error).__name__ if error is not None else "UnknownError"
            ),
            "error_message": str(error or "unknown error"),
            "commands": commands,
        }

    def outcome_unknown(self, *, error: Exception | None, commands) -> dict[str, Any]:
        details = self.delivery_error(error=error, commands=commands)
        details["command_ownership_retained"] = True
        return details

    def succeeded(self, *, command_context, commands_provider) -> dict[str, Any]:
        return {
            "session_id": command_context.session_id,
            "connection_generation": command_context.generation,
            "command_message_id": command_context.command_message_id,
            "sent_at_ms": self._now_ms(),
            "commands": commands_provider(),
        }
