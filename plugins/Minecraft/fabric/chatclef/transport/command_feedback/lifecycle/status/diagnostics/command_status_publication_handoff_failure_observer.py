#20260908_kpopmodder: Observe a custody-factory failure without retaining raw STATUS state.
from __future__ import annotations


class CommandStatusPublicationHandoffFailureObserver:
    def __init__(self, *, projection_callback, record_callback) -> None:
        if not callable(projection_callback) or not callable(record_callback):
            raise TypeError("STATUS handoff observer callbacks must be callable")
        self._projection_callback = projection_callback
        self._record_callback = record_callback

    def observe(
        self,
        query: object,
        snapshot: object,
        *,
        stage: object,
        exception_class: object,
    ) -> bool:
        try:
            record = self._projection_callback(
                stage=stage,
                exception_class=exception_class,
                query=query,
                snapshot=snapshot,
            )
            self._record_callback(record)
        except Exception:
            return False
        return True


__all__ = ("CommandStatusPublicationHandoffFailureObserver",)
