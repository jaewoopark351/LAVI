#20260907_kpopmodder: Return one immutable read-only generalized lifecycle snapshot.
#20260908_kpopmodder: Freeze the synchronized terminal claim state used by bounded STATUS diagnostics.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import ClassVar


@dataclass(frozen=True, slots=True)
class CommandFeedbackLifecycleSnapshot:
    RUNNING: ClassVar[str] = "running"
    PENDING: ClassVar[str] = "pending"
    IDLE: ClassVar[str] = "idle"
    UNAVAILABLE: ClassVar[str] = "unavailable"

    state: str
    descriptor: object = field(default=None, compare=False, repr=False)
    command_name: str | None = None
    requested_family: str | None = None
    target_item: str | None = None
    requested_count: int | None = None
    result_reason: str = ""
    query_matched: bool = True
    query_family_matched: bool = True
    query_target_matched: bool = True
    owner_present: bool = False
    availability_reason: str = ""
    publication_acknowledgement: object = field(
        default=None,
        compare=False,
        repr=False,
    )
    terminal_state: str = "none"


__all__ = ("CommandFeedbackLifecycleSnapshot",)
