#20260907_kpopmodder: Return one typed publication transition result.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandFeedbackPublicationResolution:
    accepted: bool
    terminal_response: object | None = None
    retire_lifecycle: bool = False

    def __post_init__(self) -> None:
        if type(self.accepted) is not bool or type(self.retire_lifecycle) is not bool:
            raise TypeError("command feedback publication flags must be exact bools")

    @classmethod
    def rejected(cls) -> "CommandFeedbackPublicationResolution":
        return cls(accepted=False)

    @classmethod
    def resolved(
        cls,
        terminal_response: object | None = None,
        *,
        retire_lifecycle: bool = False,
    ) -> "CommandFeedbackPublicationResolution":
        return cls(
            accepted=True,
            terminal_response=terminal_response,
            retire_lifecycle=retire_lifecycle,
        )


__all__ = ("CommandFeedbackPublicationResolution",)
