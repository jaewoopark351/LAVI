#20260908_kpopmodder: Freeze one pure contextual STATUS eligibility and match result.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class ContextualCommandStatusClaimEvaluation:
    claimed: bool
    family_matched: bool
    target_matched: bool
    ordinary_context: bool
    owner_matched: bool

    def __post_init__(self) -> None:
        if any(
            type(value) is not bool
            for value in (
                self.claimed,
                self.family_matched,
                self.target_matched,
                self.ordinary_context,
                self.owner_matched,
            )
        ):
            raise TypeError("contextual command status claim flags must be exact bools")


__all__ = ("ContextualCommandStatusClaimEvaluation",)
