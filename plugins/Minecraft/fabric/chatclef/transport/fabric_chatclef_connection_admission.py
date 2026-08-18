#20260819_kpopmodder: Keep connection-admission result data separate from ownership behavior.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class FabricChatClefConnectionAdmission:
    accepted: bool
    session_id: str
    generation: int
    reason: str = ""
