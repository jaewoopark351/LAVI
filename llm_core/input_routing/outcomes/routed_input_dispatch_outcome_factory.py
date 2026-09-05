#20260905_kpopmodder: Construct canonical routed-input dispatch outcomes.
from __future__ import annotations

from ..routed_input_dispatch_outcome import RoutedInputDispatchOutcome


class RoutedInputDispatchOutcomeFactory:
    def unhandled(self) -> RoutedInputDispatchOutcome:
        return RoutedInputDispatchOutcome(
            handled=False,
            suppress_response=False,
            response=None,
        )

    def suppressed(self) -> RoutedInputDispatchOutcome:
        return RoutedInputDispatchOutcome(
            handled=True,
            suppress_response=True,
            response=None,
        )

    def handled(self, response: object) -> RoutedInputDispatchOutcome:
        return RoutedInputDispatchOutcome(
            handled=True,
            suppress_response=False,
            response=response,
        )


__all__ = ("RoutedInputDispatchOutcomeFactory",)
