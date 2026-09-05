#20260905_kpopmodder: Resolve route decisions before optional publication.
from __future__ import annotations

from .routed_input_decision_resolution import RoutedInputDecisionResolution


class RoutedInputDecisionResolver:
    def __init__(self, outcome_factory):
        self._outcome_factory = outcome_factory

    def resolve(self, decision) -> RoutedInputDecisionResolution:
        if not getattr(decision, "handled", False):
            return self._terminal(self._outcome_factory.unhandled())
        if getattr(decision, "suppress_response", False) is True:
            return self._terminal(self._outcome_factory.suppressed())

        response_text = str(
            getattr(decision, "response_text", "") or ""
        ).strip()
        if not response_text:
            response_text = "[Input routed]"
        if getattr(decision, "publish_external_response", False) is not True:
            return self._terminal(
                self._outcome_factory.handled(response_text)
            )
        return RoutedInputDecisionResolution(
            terminal_outcome=None,
            response_text=response_text,
            requires_publication=True,
        )

    @staticmethod
    def _terminal(outcome: object) -> RoutedInputDecisionResolution:
        return RoutedInputDecisionResolution(
            terminal_outcome=outcome,
            response_text="",
            requires_publication=False,
        )


__all__ = ("RoutedInputDecisionResolver",)
