#20260907_kpopmodder: Distinguish unrelated TTS input from handled lifecycle input.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class TtsLifecycleResponseInputOutcome:
    handled: bool
    receipt: object = None

    def __post_init__(self) -> None:
        if type(self.handled) is not bool:
            raise TypeError("handled must be an exact bool")
        if self.handled is False and self.receipt is not None:
            raise ValueError("unhandled lifecycle input cannot carry a receipt")

    @classmethod
    def unrelated(cls) -> "TtsLifecycleResponseInputOutcome":
        return cls(handled=False)

    @classmethod
    def handled_receipt(
        cls,
        receipt: object,
    ) -> "TtsLifecycleResponseInputOutcome":
        return cls(handled=True, receipt=receipt)


__all__ = ("TtsLifecycleResponseInputOutcome",)
