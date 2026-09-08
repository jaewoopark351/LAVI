#20260907_kpopmodder: Preserve crafting result APIs over split acceptance and presentation owners.
from __future__ import annotations

from .result.acceptance import CraftingFeedbackResultAcceptance


class CraftingFeedbackResultCoordinator:
    def __init__(
        self,
        *,
        tracker,
        effect_verifier,
        response_renderer,
        presentation_detail_projector=None,
        stop_terminal_arbitrator=None,
        evidence_failure_reporter=None,
    ) -> None:
        self._acceptance = CraftingFeedbackResultAcceptance(
            tracker=tracker,
            effect_verifier=effect_verifier,
            stop_terminal_arbitrator=stop_terminal_arbitrator,
            evidence_failure_reporter=evidence_failure_reporter,
        )
        if callable(getattr(response_renderer, "present", None)):
            self._terminal_presenter = response_renderer
        else:
            # Imported only for legacy callers that still pass a bare renderer.
            from .compatibility import CraftingFeedbackLegacyTerminalPresenter

            self._terminal_presenter = CraftingFeedbackLegacyTerminalPresenter(
                response_renderer=response_renderer,
                presentation_detail_projector=presentation_detail_projector,
            )

    def accept_fact(self, **values):
        return self._acceptance.accept_fact(**values)

    def render_terminal_fact(self, fact: object):
        return self._terminal_presenter.present(fact)

    def accept(self, **values):
        fact = self.accept_fact(**values)
        return None if fact is None else self.render_terminal_fact(fact)

__all__ = ("CraftingFeedbackResultCoordinator",)
