#20260907_kpopmodder: Convert one immutable terminal fact into its generalized envelope.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailProjector,
)

from .command_lifecycle_terminal_response import CommandLifecycleTerminalResponse


class CommandLifecycleTerminalResponseFactory:
    def __init__(
        self,
        *,
        response_renderer,
        presentation_detail_projector=None,
    ) -> None:
        self._response_renderer = response_renderer
        self._presentation_details = (
            presentation_detail_projector
            or CommandLifecyclePresentationDetailProjector()
        )

    def build(self, fact: object) -> CommandLifecycleTerminalResponse:
        descriptor = getattr(fact, "descriptor", None)
        return CommandLifecycleTerminalResponse(
            text=self._response_renderer.render_terminal(fact),
            event_id=getattr(fact, "event_id", ""),
            command_name=getattr(descriptor, "command_name", ""),
            presentation_detail_log=self._presentation_details.project(
                descriptor
            ),
        )


__all__ = ("CommandLifecycleTerminalResponseFactory",)
