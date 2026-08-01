#20260801_kpopmodder: Represent one Fabric ChatClef bridge client session.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass
class FabricChatClefSession:
    session_id: str
    connected_at_ms: int
    last_seen_at_ms: int
    protocol_version: int = 1
    capabilities: dict[str, Any] = field(default_factory=dict)
    metadata: dict[str, Any] = field(default_factory=dict)

    def touch(self, timestamp_ms: int) -> None:
        self.last_seen_at_ms = int(timestamp_ms or self.last_seen_at_ms)

    def to_dict(self) -> dict[str, Any]:
        return {
            "session_id": self.session_id,
            "connected_at_ms": self.connected_at_ms,
            "last_seen_at_ms": self.last_seen_at_ms,
            "protocol_version": self.protocol_version,
            "capabilities": dict(self.capabilities),
            "metadata": dict(self.metadata),
        }
