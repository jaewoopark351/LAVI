#20260905_kpopmodder: Build the exact v1 metadata profile for one STOP control.
from __future__ import annotations

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO

from .stop_control_identity import StopControlIdentity
from .stop_control_target_snapshot import StopControlTargetSnapshot


class StopControlRequestFactory:
    DEADLINE_WINDOW_MS = 2000

    def build(
        self,
        *,
        identity: StopControlIdentity,
        event: object,
        now_ms: int,
        target: StopControlTargetSnapshot | None,
    ) -> CommandRequestDTO:
        target_scope = "tracked_command" if target is not None else "current_global_automation"
        metadata = {
            "request_kind": "stop_control_v1",
            "operation": "stop_ai",
            "input_event": {
                "source": getattr(event, "source", ""),
                "provider_id": getattr(event, "provider_id", ""),
                "event_kind": getattr(event, "event_kind", ""),
                "final": getattr(event, "final", False),
                "event_id": getattr(event, "event_id", ""),
            },
            "server_connection_generation": identity.server_connection_generation,
            "target_scope": target_scope,
        }
        if target is not None:
            metadata.update(
                {
                    "target_request_id": target.request_id,
                    "target_command_message_id": target.command_message_id,
                    "target_session_id": target.session_id,
                    "target_server_connection_generation": target.server_connection_generation,
                }
            )
        return CommandRequestDTO(
            request_id=identity.request_id,
            command="stop",
            source=getattr(event, "source", ""),
            deadline_ms=now_ms + self.DEADLINE_WINDOW_MS,
            metadata=metadata,
        )


__all__ = ("StopControlRequestFactory",)
