#20260907_kpopmodder: Describe one deterministic command phrase family.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandPhraseProfile:
    command_name: str
    lifecycle_kind: str
    profile_id: str
    family: str
    command_label: str
    response_lifecycle_kind: str = "finite_task"

    def __post_init__(self) -> None:
        if not all(
            type(value) is str and bool(value)
            for value in (
                self.command_name,
                self.lifecycle_kind,
                self.profile_id,
                self.family,
                self.command_label,
                self.response_lifecycle_kind,
            )
        ):
            raise ValueError("command phrase profile identity is incomplete")


__all__ = ("CommandPhraseProfile",)
