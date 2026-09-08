#20260907_kpopmodder: Freeze one correlated terminal fact before phrase rendering.
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class CommandTerminalFact:
    descriptor: object
    status: str
    verified: bool
    dispatch_started: bool
    result_reason: str
    event_id: str
    owner_token: object = field(compare=False, repr=False)
    evidence_projection: object = field(default=None, compare=False, repr=False)

    def __post_init__(self) -> None:
        if type(self.verified) is not bool or type(self.dispatch_started) is not bool:
            raise TypeError("command terminal proof flags must be exact bools")
        if type(self.status) is not str or not self.status:
            raise ValueError("command terminal status is required")


__all__ = ("CommandTerminalFact",)
