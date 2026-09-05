#20260905_kpopmodder: Emit fault-contained delivery state without response text.
from __future__ import annotations

from core.logger import log_print

from .command_feedback_delivery_formatter import (
    CommandFeedbackDeliveryFormatter,
)
from .command_feedback_delivery_record import CommandFeedbackDeliveryRecord


class CommandFeedbackDeliveryLogger:
    def __init__(self, callback=log_print, formatter=None):
        self._callback = callback
        self._formatter = formatter or CommandFeedbackDeliveryFormatter()

    def log(
        self,
        *,
        event_id: object,
        route_kind: object,
        response_kind: object,
        sink: object,
        response_generation: object,
        delivered: object,
        reason: object,
    ) -> bool:
        record = CommandFeedbackDeliveryRecord(
            event_id=event_id,
            route_kind=route_kind,
            response_kind=response_kind,
            sink=sink,
            response_generation=response_generation,
            delivered=delivered,
            reason=reason,
        )
        try:
            self._callback(self._formatter.format(record))
        except Exception:
            return False
        return True


__all__ = ("CommandFeedbackDeliveryLogger",)
