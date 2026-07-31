#20260801_kpopmodder: Keep Phase 1 Fabric ChatClef config minimal and inert.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class FabricChatClefConfig:
    enabled: bool = False
