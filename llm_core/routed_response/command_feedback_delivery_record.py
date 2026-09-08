#20260905_kpopmodder: Carry only canonical command-feedback delivery metadata.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandFeedbackDeliveryRecord:
    event_id: object
    route_kind: object
    response_kind: object
    sink: object
    response_generation: object
    delivered: object
    reason: object
    delivery_mode: object = "current_input"


__all__ = ("CommandFeedbackDeliveryRecord",)
