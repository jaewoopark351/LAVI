#20260905_kpopmodder: Keep routed-response presentation in the established responsibility-split boundary.
#20260907_kpopmodder: Record presentation-sink acceptance without claiming browser rendering.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class RoutedResponsePresentationReceipt:
    event_id: str
    sink: str
    accepted: bool
    reason: str

    def __post_init__(self) -> None:
        for name, value in (
            ("event_id", self.event_id),
            ("sink", self.sink),
            ("reason", self.reason),
        ):
            if type(value) is not str or not value or len(value) > 160:
                raise ValueError(f"{name} must be a bounded non-empty exact str")
        if type(self.accepted) is not bool:
            raise TypeError("accepted must be an exact bool")


__all__ = ("RoutedResponsePresentationReceipt",)
