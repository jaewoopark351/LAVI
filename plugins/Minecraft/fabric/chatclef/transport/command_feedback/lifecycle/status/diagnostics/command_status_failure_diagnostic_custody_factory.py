#20260908_kpopmodder: Freeze complete bounded query and snapshot facts before STATUS routing.
from __future__ import annotations

from .command_status_failure_diagnostic_custody import (
    CommandStatusFailureDiagnosticCustody,
)


class CommandStatusFailureDiagnosticCustodyFactory:
    def __init__(self, *, projection_callback, record_callback) -> None:
        if not callable(projection_callback) or not callable(record_callback):
            raise TypeError("STATUS diagnostic factory callbacks must be callable")
        self._projection_callback = projection_callback
        self._record_callback = record_callback

    def create(self, query: object, snapshot: object):
        base_record = self._projection_callback(
            stage="publication_handoff_snapshot",
            exception_class="none",
            query=query,
            snapshot=snapshot,
        )
        return CommandStatusFailureDiagnosticCustody(
            _base_record=base_record,
            _record_callback=self._record_callback,
        )


__all__ = ("CommandStatusFailureDiagnosticCustodyFactory",)
