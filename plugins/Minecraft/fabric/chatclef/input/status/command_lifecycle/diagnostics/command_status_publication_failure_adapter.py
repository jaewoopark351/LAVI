#20260908_kpopmodder: Adapt backend-neutral dispatcher failures into STATUS custody.
from __future__ import annotations


class CommandStatusPublicationFailureAdapter:
    def __init__(self, *, custody_policy=None) -> None:
        self._custody_policy = custody_policy

    def observe(
        self,
        decision: object,
        *,
        stage: object,
        exception_class: object,
    ) -> None:
        if not self._matches(decision):
            return
        try:
            acknowledgement = decision.response_publication_acknowledgement
            custody = acknowledgement.publication_failure_diagnostic_custody
            callback = getattr(custody, "record_once", None)
            if callable(callback):
                callback(stage, exception_class)
        except Exception:
            return

    def _matches(self, decision: object) -> bool:
        matcher = getattr(self._custody_policy, "matches", self._custody_policy)
        if not callable(matcher):
            return False
        try:
            return matcher(decision) is True
        except Exception:
            return False


__all__ = ("CommandStatusPublicationFailureAdapter",)
