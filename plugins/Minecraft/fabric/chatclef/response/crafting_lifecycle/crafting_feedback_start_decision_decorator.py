#20260907_kpopmodder: Decorate only a successfully claimed crafting start response.
from __future__ import annotations

from dataclasses import replace


class CraftingFeedbackStartDecisionDecorator:
    def __init__(self, response_renderer) -> None:
        self._response_renderer = response_renderer

    def decorate(
        self,
        decision: object,
        *,
        start_claimed: bool,
        publication_acknowledgement: object = None,
    ):
        if start_claimed is not True:
            return decision
        return replace(
            decision,
            response_text=self._response_renderer.render_start(),
            response_publication_acknowledgement=(
                publication_acknowledgement
            ),
        )


__all__ = ("CraftingFeedbackStartDecisionDecorator",)
