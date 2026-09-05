#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_target_snapshot import (
    StopControlTargetSnapshot,
)


class StopControlTargetSnapshotFactory:
    def __init__(self, connection_ownership: object):
        self._connection_ownership = connection_ownership

    def create(self) -> StopControlTargetSnapshot | None:
        snapshot = self._connection_ownership.local_admission_snapshot()
        owner_token = getattr(
            self._connection_ownership,
            "active_command_owner",
            None,
        )
        candidate = StopControlTargetSnapshot(
            request_id=self._exact_text(snapshot.get("active_request_id")),
            command_message_id=self._exact_text(
                snapshot.get("active_command_message_id")
            ),
            session_id=self._exact_text(snapshot.get("active_session_id")),
            server_connection_generation=self._exact_positive_int(
                snapshot.get("active_generation")
            ),
            owner_token=owner_token,
        )
        return candidate if candidate.complete else None

    def _exact_text(self, value: object) -> str:
        return value if type(value) is str else ""

    def _exact_positive_int(self, value: object) -> int:
        return value if type(value) is int and value > 0 else 0


__all__ = ("StopControlTargetSnapshotFactory",)
