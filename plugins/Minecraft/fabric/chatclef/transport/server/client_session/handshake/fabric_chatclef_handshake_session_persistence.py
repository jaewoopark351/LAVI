#20260905_kpopmodder: Isolate accepted Fabric ChatClef session persistence.
from __future__ import annotations

from typing import Any, Callable


class FabricChatClefHandshakeSessionPersistence:
    def __init__(self, *, session_registry, now_ms: Callable[[], int], diagnostics=None):
        self._session_registry = session_registry
        self._now_ms = now_ms
        self._diagnostics = diagnostics

    def persist(self, *, session_id: str, envelope: object) -> None:
        session = self._session_registry.upsert(
            session_id=session_id,
            timestamp_ms=self._now_ms(),
            protocol_version=envelope.protocol_version,
            capabilities=self.payload_dict(
                envelope.payload.get("capabilities")
            ),
            metadata=self.payload_dict(envelope.payload.get("metadata")),
        )
        #20260915_kpopmodder: Observe bounded metadata validation at accepted handshake.
        summary = session.metadata.get("korean_command_catalogue_status", {})
        if self._diagnostics is not None:
            try:
                self._diagnostics.info(
                    "command_catalogue boundary=handshake "
                    f"available={summary.get('available', False)} "
                    f"entries={summary.get('entry_count', 0)} "
                    f"reason={summary.get('reason', 'validated')}")
            except Exception:
                pass

    @staticmethod
    def payload_dict(value: Any) -> dict[str, Any]:
        return dict(value) if isinstance(value, dict) else {}


__all__ = ("FabricChatClefHandshakeSessionPersistence",)
