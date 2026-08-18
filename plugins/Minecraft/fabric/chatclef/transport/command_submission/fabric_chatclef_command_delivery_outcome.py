#20260819_kpopmodder: Keep command-delivery result data separate from delivery behavior.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class FabricChatClefCommandDeliveryOutcome:
    status: str
    error: Exception | None = None
