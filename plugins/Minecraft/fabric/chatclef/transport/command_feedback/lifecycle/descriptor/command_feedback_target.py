#20260907_kpopmodder: Preserve one bounded immutable item target without retaining raw speech text.
from __future__ import annotations

import re
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandFeedbackTarget:
    canonical_target: str
    requested_count: int
    quantity_semantics: str
    spoken_label: str
    slot_provenance: str = "explicit"

    _TARGET = re.compile(r"[a-z0-9_]+\Z", re.ASCII)

    def __post_init__(self) -> None:
        if (
            type(self.canonical_target) is not str
            or self._TARGET.fullmatch(self.canonical_target) is None
            or type(self.requested_count) is not int
            or self.requested_count < 1
            or self.quantity_semantics not in {
                "acquire_delta",
                "requested_count",
                "unknown",
            }
            or type(self.spoken_label) is not str
            or not self.spoken_label
            or self.slot_provenance not in {"explicit", "defaulted"}
        ):
            raise ValueError("command feedback target is invalid")


__all__ = ("CommandFeedbackTarget",)
