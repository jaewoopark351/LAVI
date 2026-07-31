#20260801_kpopmodder: Define command result states shared by Minecraft backend protocols.
from __future__ import annotations

from enum import Enum


class CommandResultStatus(str, Enum):
    ACCEPTED = "accepted"
    RUNNING = "running"
    COMPLETED = "completed"
    REJECTED = "rejected"
    FAILED = "failed"
    CANCELLED = "cancelled"
    DEADLINE_EXCEEDED = "deadline_exceeded"
    UNKNOWN = "unknown"

    @property
    def ok(self) -> bool:
        return self in {
            CommandResultStatus.ACCEPTED,
            CommandResultStatus.RUNNING,
            CommandResultStatus.COMPLETED,
        }
