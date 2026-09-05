#20260905_kpopmodder: Carry one immutable ordinary-command delivery outcome.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class FabricChatClefCommandDeliveryOutcome:
    status: str
    error: Exception | None = None
