#20260905_kpopmodder: Commit and release only ordinary-command ownership under its lock.
from __future__ import annotations

from typing import Any


class FabricChatClefCommandOwnershipCommitter:
    def __init__(self, *, connection_ownership, command_lock) -> None:
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock

    def begin_locked(self, *, request, message_id: str):
        return self._connection_ownership.begin_command(
            request_id=request.request_id,
            command_message_id=message_id,
            command=request.command,
            source=request.source,
        )

    def snapshot_locked(self) -> dict[str, Any]:
        return self._connection_ownership.local_admission_snapshot()

    def release_if_not_scheduled(self, command_context: Any) -> dict[str, Any]:
        with self._command_lock:
            self._connection_ownership.clear_command_if_current(command_context)
            return self.snapshot_locked()

    def snapshot(self) -> dict[str, Any]:
        with self._command_lock:
            return self.snapshot_locked()
