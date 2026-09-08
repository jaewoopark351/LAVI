#20260908_kpopmodder: Added this module to freeze one terminal-evidence decision and its optional projection.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandTerminalEvidenceEvaluation:
    verified: bool
    projection: object = None

    def __post_init__(self) -> None:
        if type(self.verified) is not bool:
            raise TypeError("command terminal evidence verified must be an exact bool")
        if not self.verified and self.projection is not None:
            raise ValueError("unverified command evidence cannot carry a projection")


__all__ = ("CommandTerminalEvidenceEvaluation",)
