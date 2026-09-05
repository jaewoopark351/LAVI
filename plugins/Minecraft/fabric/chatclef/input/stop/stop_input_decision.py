#20260905_kpopmodder: Represent one side-effect-free Korean STOP input decision.
from __future__ import annotations

from dataclasses import dataclass

from .stop_input_decision_kind import StopInputDecisionKind


@dataclass(frozen=True)
class StopInputDecision:
    kind: StopInputDecisionKind
    phrase_rule_id: str = ""
    normalized_phrase: str = ""
    terminal_suffix: str = ""
    reason: str = ""

    @property
    def valid(self) -> bool:
        return self.kind is StopInputDecisionKind.VALID_EXACT_STOP

    @property
    def guarded_rejection(self) -> bool:
        return self.kind is StopInputDecisionKind.GUARDED_STOP_LIKE_REJECTION


__all__ = ("StopInputDecision",)
