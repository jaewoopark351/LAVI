#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Freeze one contextual-busy route eligibility result.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class ContextualBusyResponseEvaluation:
    candidate: bool
    verified: bool

    def __post_init__(self) -> None:
        if type(self.candidate) is not bool or type(self.verified) is not bool:
            raise TypeError("contextual busy evaluation flags must be exact bools")
        if self.verified and not self.candidate:
            raise ValueError("a verified contextual busy result must be a candidate")


__all__ = ("ContextualBusyResponseEvaluation",)
