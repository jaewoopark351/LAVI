#20260907_kpopmodder: Isolate legacy bare-renderer presentation compatibility.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.terminal import (
    CraftingFeedbackTerminalPresenter,
)


class CraftingFeedbackLegacyTerminalPresenter:
    def __init__(
        self,
        *,
        response_renderer=None,
        presentation_detail_projector=None,
    ) -> None:
        if response_renderer is None:
            from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
                CommandLifecycleResponseRenderer,
            )

            response_renderer = CommandLifecycleResponseRenderer()
        self._presenter = CraftingFeedbackTerminalPresenter(
            response_renderer=response_renderer,
            presentation_detail_projector=presentation_detail_projector,
        )

    def present(self, fact: object):
        return self._presenter.present(fact)


__all__ = ("CraftingFeedbackLegacyTerminalPresenter",)
