#20260907_kpopmodder: Wrap only acknowledgements that expose the closed coalesce port.
from __future__ import annotations

from .command_feedback_ready_decision_acknowledgement import (
    CommandFeedbackReadyDecisionAcknowledgement,
)


class CommandFeedbackReadyDecisionAcknowledgementFactory:
    def __init__(
        self,
        *,
        initial_response_selector,
        coalesced_response_factory,
    ) -> None:
        self._initial_response_selector = initial_response_selector
        self._coalesced_response_factory = coalesced_response_factory

    def wrap(self, acknowledgement: object):
        if not all(
            callable(getattr(acknowledgement, method_name, None))
            for method_name in (
                "wait_until_ready",
                "acknowledge",
                "select_coalesced_terminal",
            )
        ):
            return acknowledgement
        return CommandFeedbackReadyDecisionAcknowledgement(
            acknowledgement=acknowledgement,
            initial_response_selector=self._initial_response_selector,
            coalesced_response_factory=self._coalesced_response_factory,
        )


__all__ = ("CommandFeedbackReadyDecisionAcknowledgementFactory",)
