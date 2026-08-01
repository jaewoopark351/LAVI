#20260801_kpopmodder: Isolate Fabric ChatClef session ownership from transport I/O.
from __future__ import annotations

import threading
from typing import Any

from .fabric_chatclef_session import FabricChatClefSession


class FabricChatClefSessionRegistry:
    def __init__(self):
        self._lock = threading.RLock()
        self._sessions: dict[str, FabricChatClefSession] = {}
        self._active_session_id: str | None = None

    def upsert(
        self,
        *,
        session_id: str,
        timestamp_ms: int,
        protocol_version: int = 1,
        capabilities: dict[str, Any] | None = None,
        metadata: dict[str, Any] | None = None,
    ) -> FabricChatClefSession:
        with self._lock:
            session = self._sessions.get(session_id)
            if session is None:
                session = FabricChatClefSession(
                    session_id=session_id,
                    connected_at_ms=timestamp_ms,
                    last_seen_at_ms=timestamp_ms,
                    protocol_version=protocol_version,
                    capabilities=dict(capabilities or {}),
                    metadata=dict(metadata or {}),
                )
                self._sessions[session_id] = session
            else:
                session.touch(timestamp_ms)
                session.capabilities = dict(capabilities or session.capabilities)
                session.metadata = dict(metadata or session.metadata)
            self._active_session_id = session_id
            return session

    def remove(self, session_id: str) -> None:
        with self._lock:
            self._sessions.pop(session_id, None)
            if self._active_session_id == session_id:
                self._active_session_id = None

    def clear(self) -> None:
        with self._lock:
            self._sessions.clear()
            self._active_session_id = None

    def active_session(self) -> FabricChatClefSession | None:
        with self._lock:
            if self._active_session_id is None:
                return None
            return self._sessions.get(self._active_session_id)

    def count(self) -> int:
        with self._lock:
            return len(self._sessions)

    def snapshot(self) -> dict[str, Any]:
        with self._lock:
            active = self.active_session()
            return {
                "active_session_id": self._active_session_id,
                "session_count": len(self._sessions),
                "active_session": None if active is None else active.to_dict(),
            }
