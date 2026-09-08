#20260907_kpopmodder: Describe one source-backed raw command grammar without granting execution authority.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandFeedbackRawFormProfile:
    command_name: str
    grammar_id: str

    def __post_init__(self) -> None:
        if not all(
            type(value) is str and bool(value)
            for value in (self.command_name, self.grammar_id)
        ):
            raise ValueError("raw command form profile identity is incomplete")


__all__ = ("CommandFeedbackRawFormProfile",)
