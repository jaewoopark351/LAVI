#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_submission_result import (
    StopControlSubmissionResult,
)


class StopControlSubmissionResultFactory:
    def create(
        self,
        accepted: bool,
        reason: str,
        tracker: object | None = None,
    ) -> StopControlSubmissionResult:
        data = {}
        if tracker is not None:
            data = {
                "request_id": tracker.identity.request_id,
                "command_message_id": tracker.identity.message_id,
                "session_id": tracker.identity.session_id,
                "connection_generation": (
                    tracker.identity.server_connection_generation
                ),
                "target_scope": tracker.target_scope,
            }
        return StopControlSubmissionResult(
            accepted=accepted,
            reason=str(reason or "control_send_rejected"),
            result={
                "ok": accepted,
                "status": "accepted" if accepted else "rejected",
                "reason": reason,
                "data": data,
            },
        )


__all__ = ("StopControlSubmissionResultFactory",)
