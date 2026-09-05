#20260905_kpopmodder: Emit one fault-contained STOP classification boundary record.
from __future__ import annotations

from core.logger import log_print

from .stop_input_decision_formatter import StopInputDecisionFormatter
from .stop_input_decision_projector import StopInputDecisionProjector


class StopInputDecisionLogger:
    def __init__(self, callback=log_print, *, projector=None, formatter=None):
        self._callback = callback
        self._projector = projector or StopInputDecisionProjector()
        self._formatter = formatter or StopInputDecisionFormatter()

    def log(self, *, event: object, decision: object) -> bool:
        try:
            record = self._projector.project(event=event, decision=decision)
            self._callback(self._formatter.format(record))
        except Exception:
            return False
        return True


__all__ = ("StopInputDecisionLogger",)
