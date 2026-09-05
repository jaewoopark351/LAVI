#20260905_kpopmodder: Isolate accepted Fabric ChatClef session persistence.
from __future__ import annotations

from typing import Any, Callable


class FabricChatClefHandshakeSessionPersistence:
    def __init__(self, *, session_registry, now_ms: Callable[[], int]):
        self._session_registry = session_registry
        self._now_ms = now_ms

    def persist(self, *, session_id: str, envelope: object) -> None:
        self._session_registry.upsert(
            session_id=session_id,
            timestamp_ms=self._now_ms(),
            protocol_version=envelope.protocol_version,
            capabilities=self.payload_dict(
                envelope.payload.get("capabilities")
            ),
            metadata=self.payload_dict(envelope.payload.get("metadata")),
        )

    @staticmethod
    def payload_dict(value: Any) -> dict[str, Any]:
        return dict(value) if isinstance(value, dict) else {}


__all__ = ("FabricChatClefHandshakeSessionPersistence",)
