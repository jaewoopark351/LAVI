#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Emit one closed contextual-busy pre-permit diagnostic.
from __future__ import annotations

from core.logger import log_print

from .contextual_busy_response_failure_formatter import (
    ContextualBusyResponseFailureFormatter,
)
from .contextual_busy_response_failure_projector import (
    ContextualBusyResponseFailureProjector,
)


class ContextualBusyResponseFailureLogger:
    def __init__(
        self,
        callback=log_print,
        *,
        projector=None,
        formatter=None,
        command_names: object = None,
    ) -> None:
        self._callback = callback
        self._projector = projector or ContextualBusyResponseFailureProjector(
            command_names=command_names,
        )
        self._formatter = formatter or ContextualBusyResponseFailureFormatter()

    def record(
        self,
        *,
        stage: object,
        snapshot: object = None,
        availability_reason: object = None,
        exception_class: object = "none",
    ) -> bool:
        try:
            record = self._projector.project(
                stage=stage,
                snapshot=snapshot,
                availability_reason=availability_reason,
                exception_class=exception_class,
            )
            self._callback(self._formatter.format(record))
        except Exception:
            return False
        return True


__all__ = ("ContextualBusyResponseFailureLogger",)
