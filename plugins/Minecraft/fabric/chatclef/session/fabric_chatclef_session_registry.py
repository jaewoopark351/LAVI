#20260801_kpopmodder: Isolate Fabric ChatClef session ownership from transport I/O.
from __future__ import annotations

import threading
from typing import Any

from .fabric_chatclef_session import FabricChatClefSession
from .catalogue import FabricCommandCatalogueStore


class FabricChatClefSessionRegistry:
    def __init__(self):
        self._lock = threading.RLock()
        self._sessions: dict[str, FabricChatClefSession] = {}
        self._active_session_id: str | None = None
        self._command_catalogue = FabricCommandCatalogueStore()

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
            #20260915_kpopmodder: Retain only a bounded summary in public status snapshots.
            existing = self._sessions.get(session_id)
            if not metadata and existing is not None:
                metadata = existing.metadata
            metadata = dict(metadata or {})
            catalogue = metadata.pop("korean_command_catalogue_v1", None)
            summary = self._command_catalogue.replace(session_id, catalogue)
            metadata["korean_command_catalogue_status"] = summary
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
            self._command_catalogue.clear(session_id)
            self._sessions.pop(session_id, None)
            if self._active_session_id == session_id:
                self._active_session_id = None

    def clear(self) -> None:
        with self._lock:
            self._command_catalogue.clear()
            self._sessions.clear()
            self._active_session_id = None

    #20260915_kpopmodder: Session ownership is checked by the transport before updates.
    def update_command_catalogue(self, session_id: str, wrapper: object):
        with self._lock:
            if session_id != self._active_session_id or session_id not in self._sessions:
                return {"available": False, "reason": "catalogue_foreign_session"}
            summary = self._command_catalogue.replace(session_id, wrapper)
            self._sessions[session_id].metadata["korean_command_catalogue_status"] = summary
            return summary

    def command_catalogue(self, session_id: str):
        with self._lock:
            if session_id != self._active_session_id:
                return None
            return self._command_catalogue.get(session_id)

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
