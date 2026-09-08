#20260907_kpopmodder: Render one already-selected immediate terminal as one sentence.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailProjector,
)

from .command_lifecycle_coalesced_response import (
    CommandLifecycleCoalescedResponse,
)


class CommandLifecycleCoalescedResponseFactory:
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

    def build(self, fact: object) -> CommandLifecycleCoalescedResponse:
        descriptor = getattr(fact, "descriptor", None)
        return CommandLifecycleCoalescedResponse(
            text=self._response_renderer.render_terminal(fact),
            event_id=getattr(fact, "event_id", ""),
            command_name=getattr(descriptor, "command_name", ""),
            presentation_detail_log=self._presentation_details.project(
                descriptor
            ),
        )


__all__ = ("CommandLifecycleCoalescedResponseFactory",)
