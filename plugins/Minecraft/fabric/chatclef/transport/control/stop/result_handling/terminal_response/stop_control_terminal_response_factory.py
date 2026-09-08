#20260905_kpopmodder: Isolate verified STOP terminal response construction.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.response.stop import (
    StopControlTerminalResponse,
)
from plugins.Minecraft.fabric.chatclef.presentation.stop_control import (
    StopControlPresentationDetailProjector,
)


class StopControlTerminalResponseFactory:
    def __init__(self, renderer: object, presentation_detail_projector=None):
        self._renderer = renderer
        self._presentation_details = (
            presentation_detail_projector
            or StopControlPresentationDetailProjector()
        )

    def create(self, *, tracker: object, decision: object | None) -> StopControlTerminalResponse:
        if decision is None:
            text = self._renderer.render_terminal(
                status="unknown",
                control_outcome="unknown",
                reason="malformed_control_result",
            )
        else:
            text = self._renderer.render_terminal(
                status=decision.status,
                control_outcome=decision.control_outcome,
                reason=decision.reason,
            )
        return StopControlTerminalResponse(
            text=text,
            event_id=tracker.event_id,
            presentation_detail_log=self._presentation_details.project(),
        )


__all__ = ("StopControlTerminalResponseFactory",)
