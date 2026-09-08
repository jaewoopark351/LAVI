#20260907_kpopmodder: Carry immutable provenance for one direct Fabric ChatClef UI callback.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandFeedbackUiEventReceipt:
    request_id: str
    event_id: str
    source: str
    event_kind: str
    provider_id: str
    text_digest: str


__all__ = ("CommandFeedbackUiEventReceipt",)
