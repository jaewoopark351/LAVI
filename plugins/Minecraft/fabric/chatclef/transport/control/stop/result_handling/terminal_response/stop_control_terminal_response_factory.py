#20260905_kpopmodder: Isolate verified STOP terminal response construction.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.response.stop import (
    StopControlTerminalResponse,
)


class StopControlTerminalResponseFactory:
    def __init__(self, renderer: object):
        self._renderer = renderer

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
        return StopControlTerminalResponse(text=text, event_id=tracker.event_id)


__all__ = ("StopControlTerminalResponseFactory",)
