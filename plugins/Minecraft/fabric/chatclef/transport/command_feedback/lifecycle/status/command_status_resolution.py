#20260907_kpopmodder: Carry one pure status-query match result without route ownership.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandStatusResolution:
    claimed: bool
    family_matched: bool
    target_matched: bool

    def __post_init__(self) -> None:
        if any(
            type(value) is not bool
            for value in (self.claimed, self.family_matched, self.target_matched)
        ):
            raise TypeError("command status resolution flags must be exact bools")


__all__ = ("CommandStatusResolution",)
