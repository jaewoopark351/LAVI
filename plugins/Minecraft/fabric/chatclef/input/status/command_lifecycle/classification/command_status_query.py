#20260907_kpopmodder: Represent one deterministic read-only command status question.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandStatusQuery:
    requested_family: str
    addressed: bool
    target_text: str = ""

    def __post_init__(self) -> None:
        if type(self.requested_family) is not str or not self.requested_family:
            raise ValueError("command status family is required")
        if type(self.addressed) is not bool or type(self.target_text) is not str:
            raise TypeError("command status query fields must be exact")


__all__ = ("CommandStatusQuery",)
