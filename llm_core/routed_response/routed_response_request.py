#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class RoutedResponseRequest:
    text: str
    source: str
    send_output: bool
    send_full_output: bool
    remember_history: bool
    event_id: object
    route_kind: str
    response_kind: str


__all__ = ("RoutedResponseRequest",)
