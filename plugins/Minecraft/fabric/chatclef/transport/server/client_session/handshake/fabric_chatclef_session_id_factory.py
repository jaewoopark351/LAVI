#20260905_kpopmodder: Isolate Fabric ChatClef handshake session identifiers.
from __future__ import annotations

import uuid


class FabricChatClefSessionIdFactory:
    def create(self, envelope: object) -> str:
        candidate = envelope.session_id or envelope.payload.get("session_id")
        session_id = str(candidate or "").strip()
        return session_id or f"fabric-chatclef-{uuid.uuid4().hex}"


__all__ = ("FabricChatClefSessionIdFactory",)
