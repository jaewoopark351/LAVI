#20260907_kpopmodder: Carry only the closed raw grammar identity recovered before submission.
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class CommandFeedbackRawForm:
    command_name: str
    form_kind: str
    argument_units: tuple[str, ...] = field(default=(), repr=False)

    def __post_init__(self) -> None:
        if not all(
            type(value) is str and bool(value)
            for value in (self.command_name, self.form_kind)
        ):
            raise ValueError("raw command feedback form identity is incomplete")
        if (
            type(self.argument_units) is not tuple
            or any(type(value) is not str or not value for value in self.argument_units)
        ):
            raise ValueError("raw command feedback arguments are invalid")


__all__ = ("CommandFeedbackRawForm",)
