#20260907_kpopmodder: Present immutable lifecycle facts as terminal response DTOs.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailProjector,
)
from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle import (
    CraftingLifecycleTerminalResponse,
)

from ..command_lifecycle_terminal_response import CommandLifecycleTerminalResponse


class CraftingFeedbackTerminalPresenter:
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

    def present(self, fact: object):
        descriptor = getattr(fact, "descriptor", None)
        values = {
            "text": self._render_text(fact),
            "event_id": getattr(fact, "event_id", ""),
            "presentation_detail_log": self._presentation_details.project(
                descriptor
            ),
        }
        if self._legacy_exact_craft(descriptor):
            return CraftingLifecycleTerminalResponse(**values)
        return CommandLifecycleTerminalResponse(
            **values,
            command_name=getattr(descriptor, "command_name", ""),
        )

    def _render_text(self, fact: object) -> str:
        try:
            return self._response_renderer.render_terminal(fact)
        except TypeError:
            return self._response_renderer.render_terminal(
                status=getattr(fact, "status", "unknown"),
                verified=getattr(fact, "verified", False) is True,
                dispatch_started=(
                    getattr(fact, "dispatch_started", False) is True
                ),
            )

    @staticmethod
    def _legacy_exact_craft(descriptor: object) -> bool:
        return bool(
            getattr(descriptor, "command", None) == "get diamond_pickaxe 1"
            and getattr(descriptor, "acquisition_verb_class", None) == "craft"
        )


__all__ = ("CraftingFeedbackTerminalPresenter",)
