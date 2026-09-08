#20260908_kpopmodder: Emit one fault-contained bounded status-route failure record.
from __future__ import annotations

from core.logger import log_print

from .command_status_route_failure_formatter import (
    CommandStatusRouteFailureFormatter,
)
from .command_status_route_failure_projector import (
    CommandStatusRouteFailureProjector,
)


class CommandStatusRouteFailureDiagnostics:
    def __init__(self, callback=log_print, *, projector=None, formatter=None) -> None:
        self._callback = callback
        self._projector = projector or CommandStatusRouteFailureProjector()
        self._formatter = formatter or CommandStatusRouteFailureFormatter()

    def record(
        self,
        *,
        stage: object,
        exception_class: object,
        query: object = None,
        snapshot: object = None,
    ) -> bool:
        try:
            record = self._projector.project(
                stage=stage,
                exception_class=exception_class,
                query=query,
                snapshot=snapshot,
            )
            self._callback(self._formatter.format(record))
        except Exception:
            return False
        return True


__all__ = ("CommandStatusRouteFailureDiagnostics",)
