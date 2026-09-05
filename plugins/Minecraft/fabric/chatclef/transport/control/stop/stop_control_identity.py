#20260905_kpopmodder: Represent the immutable Python STOP wire correlation quartet.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class StopControlIdentity:
    request_id: str
    message_id: str
    session_id: str
    server_connection_generation: int

    def as_tuple(self) -> tuple[object, ...]:
        return (
            self.request_id,
            self.message_id,
            self.session_id,
            self.server_connection_generation,
        )


__all__ = ("StopControlIdentity",)
