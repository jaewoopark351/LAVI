#20260801_kpopmodder: Keep Fabric ChatClef bridge endpoint config backend-owned.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class FabricChatClefConfig:
    enabled: bool = False
    host: str = "127.0.0.1"
    port: int = 4316
    startup_timeout_sec: float = 3.0
    reconcile_stale_deposit_to_unknown_enabled: bool = False

    @property
    def endpoint(self) -> str:
        return f"ws://{self.host}:{self.port}"
