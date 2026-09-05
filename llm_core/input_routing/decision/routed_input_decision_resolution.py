#20260905_kpopmodder: Carry one immutable routed-input decision resolution.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class RoutedInputDecisionResolution:
    terminal_outcome: object
    response_text: str
    requires_publication: bool


__all__ = ("RoutedInputDecisionResolution",)
