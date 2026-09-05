#20260905_kpopmodder: Isolate fault-contained STOP classification diagnostics.
from __future__ import annotations


class StopInputDecisionObserver:
    def __init__(self, decision_logger: object):
        self._decision_logger = decision_logger

    def observe(self, *, event: object, decision: object) -> None:
        try:
            self._decision_logger.log(event=event, decision=decision)
        except Exception:
            pass


__all__ = ("StopInputDecisionObserver",)
