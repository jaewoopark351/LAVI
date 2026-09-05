#20260905_kpopmodder: Isolate live WebSocket/session/generation prewrite validation.
from __future__ import annotations


class StopControlPrewriteConnectionValidator:
    def __init__(self, connection_ownership: object):
        self._connection_ownership = connection_ownership

    def matches(self, tracker: object) -> bool:
        identity = tracker.identity
        return (
            self._connection_ownership.is_active_websocket(
                tracker.transport_websocket
            )
            is True
            and type(self._connection_ownership.active_session_id) is str
            and self._connection_ownership.active_session_id == identity.session_id
            and type(self._connection_ownership.active_generation) is int
            and self._connection_ownership.active_generation
            == identity.server_connection_generation
        )


__all__ = ("StopControlPrewriteConnectionValidator",)
