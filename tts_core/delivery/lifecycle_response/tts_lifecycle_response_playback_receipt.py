#20260907_kpopmodder: Record observed lifecycle playback independently from enqueue acceptance.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class TtsLifecycleResponsePlaybackReceipt:
    event_id: str
    route_kind: str
    response_kind: str
    observed: bool
    played_item_count: int
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
        if type(self.observed) is not bool:
            raise TypeError("observed must be an exact bool")
        if self.response_generation is not None and (
            type(self.response_generation) is not int
            or self.response_generation < 0
        ):
            raise ValueError(
                "response_generation must be a non-negative exact int or None"
            )
        if self.delivery_mode not in {"current_input", "non_preempting"}:
            raise ValueError("delivery_mode must be a registered exact str")
        for name, value in (
            ("played_item_count", self.played_item_count),
            ("item_count", self.item_count),
        ):
            if type(value) is not int or value < 0:
                raise ValueError(f"{name} must be a non-negative exact int")
        if self.played_item_count > self.item_count:
            raise ValueError("played_item_count cannot exceed item_count")


__all__ = ("TtsLifecycleResponsePlaybackReceipt",)
