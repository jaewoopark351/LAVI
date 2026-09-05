#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Mapping


class StopControlTerminalIdentityValidator:
    def valid(
        self,
        *,
        envelope: object,
        payload: Mapping[str, object],
        data: Mapping[str, object],
        result: object,
        tracker: object,
    ) -> bool:
        identity = tracker.identity
        return (
            type(getattr(envelope, "correlation_id", None)) is str
            and type(getattr(envelope, "session_id", None)) is str
            and payload.get("request_id") == identity.request_id
            and getattr(result, "request_id", None) == identity.request_id
            and getattr(envelope, "correlation_id", None) == identity.message_id
            and getattr(envelope, "session_id", None) == identity.session_id
            and type(data.get("connection_generation")) is int
            and data.get("connection_generation")
            == identity.server_connection_generation
        )


__all__ = ("StopControlTerminalIdentityValidator",)
