#20260907_kpopmodder: Record lifecycle speech queue acceptance without claiming playback.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class TtsLifecycleResponseEnqueueReceipt:
    event_id: str
    route_kind: str
    response_kind: str
    accepted: bool
    item_count: int
    reason: str
    response_generation: int | None = None
    delivery_mode: str = "current_input"

    def __post_init__(self) -> None:
        for name, value in (
            ("event_id", self.event_id),
            ("route_kind", self.route_kind),
            ("response_kind", self.response_kind),
            ("reason", self.reason),
        ):
            if type(value) is not str or not value or len(value) > 160:
                raise ValueError(f"{name} must be a bounded non-empty exact str")
        if type(self.accepted) is not bool:
            raise TypeError("accepted must be an exact bool")
        if self.response_generation is not None and (
            type(self.response_generation) is not int
            or self.response_generation < 0
        ):
            raise ValueError(
                "response_generation must be a non-negative exact int or None"
            )
        if self.delivery_mode not in {"current_input", "non_preempting"}:
            raise ValueError("delivery_mode must be a registered exact str")
        if type(self.item_count) is not int or self.item_count < 0:
            raise ValueError("item_count must be a non-negative exact int")


__all__ = ("TtsLifecycleResponseEnqueueReceipt",)
