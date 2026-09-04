#20260905_kpopmodder: Defines the immutable ingress envelope while keeping fallback payloads opaque.
from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True, slots=True)
class LaviInputEvent:
    text: str
    source: str
    event_id: str
    event_kind: str
    final: bool
    provider_id: str
    fallback_payload: Any


__all__ = ["LaviInputEvent"]
