#20260907_kpopmodder: Carry one closed lifecycle TTS payload identity.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class TtsLifecycleResponseIdentity:
    event_id: str
    route_kind: str
    response_kind: str
    delivery_mode: str

    def as_tuple(self) -> tuple[str, str, str, str]:
        return (
            self.event_id,
            self.route_kind,
            self.response_kind,
            self.delivery_mode,
        )


__all__ = ("TtsLifecycleResponseIdentity",)
