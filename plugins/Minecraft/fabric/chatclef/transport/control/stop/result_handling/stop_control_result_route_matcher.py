#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Mapping


class StopControlResultRouteMatcher:
    def __init__(self, connection_ownership: object):
        self._connection_ownership = connection_ownership

    def extract(self, envelope: object) -> tuple[dict, dict]:
        raw = getattr(envelope, "payload", None)
        payload = dict(raw) if isinstance(raw, Mapping) else {}
        data = payload.get("data")
        return payload, dict(data) if isinstance(data, Mapping) else {}

    def matches_live_connection(
        self,
        websocket: object,
        tracker: object,
    ) -> bool:
        identity = tracker.identity
        try:
            return (
                self._connection_ownership.is_active_websocket(websocket) is True
                and type(self._connection_ownership.active_session_id) is str
                and self._connection_ownership.active_session_id
                == identity.session_id
                and type(self._connection_ownership.active_generation) is int
                and self._connection_ownership.active_generation
                == identity.server_connection_generation
            )
        except Exception:
            return False

    def matches_tracker_routing_identity(
        self,
        payload: Mapping[str, object],
        envelope: object,
        tracker: object,
    ) -> bool:
        identity = tracker.identity
        return (
            payload.get("request_id") == identity.request_id
            and getattr(envelope, "correlation_id", None) == identity.message_id
            and getattr(envelope, "session_id", None) == identity.session_id
        )

    def has_stop_marker(self, data: Mapping[str, object]) -> bool:
        return (
            data.get("request_kind") == "stop_control_v1"
            or data.get("operation") == "stop_ai"
        )


__all__ = ("StopControlResultRouteMatcher",)
