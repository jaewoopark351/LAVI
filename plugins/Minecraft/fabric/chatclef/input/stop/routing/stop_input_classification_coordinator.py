#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.stop.routing.stop_input_decision_observer import StopInputDecisionObserver


class StopInputClassificationCoordinator:
    def __init__(self, *, classifier: object, decision_logger: object):
        self._classifier = classifier
        self._observer = StopInputDecisionObserver(decision_logger)

    def classify(self, event: object):
        decision = self._classifier.classify(getattr(event, "text", None))
        self._observer.observe(event=event, decision=decision)
        return decision


__all__ = ("StopInputClassificationCoordinator",)
