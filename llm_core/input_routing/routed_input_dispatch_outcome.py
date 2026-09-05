#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class RoutedInputDispatchOutcome:
    handled: bool
    suppress_response: bool
    response: object | None

    def __post_init__(self) -> None:
        if type(self.handled) is not bool:
            raise TypeError("handled must be an exact bool")
        if type(self.suppress_response) is not bool:
            raise TypeError("suppress_response must be an exact bool")
        if not self.handled and (self.suppress_response or self.response is not None):
            raise ValueError("an unhandled outcome cannot contain a routed response")
        if self.suppress_response and self.response is not None:
            raise ValueError("a suppressed outcome cannot contain a routed response")


__all__ = ("RoutedInputDispatchOutcome",)
