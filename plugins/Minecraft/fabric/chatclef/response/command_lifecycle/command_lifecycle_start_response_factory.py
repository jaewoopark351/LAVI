#20260907_kpopmodder: Build a typed START response from an immutable descriptor.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailProjector,
)

from .command_lifecycle_start_response import CommandLifecycleStartResponse


class CommandLifecycleStartResponseFactory:
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

    def build(self, descriptor: object) -> CommandLifecycleStartResponse:
        return CommandLifecycleStartResponse(
            text=self._response_renderer.render_start(descriptor),
            event_id=getattr(descriptor, "event_id", ""),
            command_name=getattr(descriptor, "command_name", ""),
            presentation_detail_log=self._presentation_details.project(
                descriptor
            ),
        )


__all__ = ("CommandLifecycleStartResponseFactory",)
