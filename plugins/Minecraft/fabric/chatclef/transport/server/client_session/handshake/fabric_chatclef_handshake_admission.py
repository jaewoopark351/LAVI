#20260905_kpopmodder: Isolate locked Fabric ChatClef handshake admission.
from __future__ import annotations


class FabricChatClefHandshakeAdmission:
    def __init__(self, *, connection_ownership, command_lock):
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock

    def inspect(self, *, websocket: object, session_id: str):
        with self._command_lock:
            return self._connection_ownership.try_activate(
                websocket=websocket,
                session_id=session_id,
            )


__all__ = ("FabricChatClefHandshakeAdmission",)
