#20260909_kpopmodder: Freeze the exact active-command identity observed by a typed busy precheck.
from __future__ import annotations

from dataclasses import dataclass
from typing import ClassVar


@dataclass(frozen=True, slots=True)
class CommandBusyObservedIdentity:
    MAX_TEXT_LENGTH: ClassVar[int] = 160
    MAX_GENERATION: ClassVar[int] = 9_223_372_036_854_775_807

    active_session_id: str
    active_generation: int
    active_request_id: str
    active_command_message_id: str

    def __post_init__(self) -> None:
        for value in (
            self.active_session_id,
            self.active_request_id,
            self.active_command_message_id,
        ):
            if (
                type(value) is not str
                or not value
                or value != value.strip()
                or len(value) > self.MAX_TEXT_LENGTH
            ):
                raise ValueError("busy command identity text is invalid")
        if (
            type(self.active_generation) is not int
            or self.active_generation < 1
            or self.active_generation > self.MAX_GENERATION
        ):
            raise ValueError("busy command generation is invalid")


__all__ = ("CommandBusyObservedIdentity",)
