#20260913_kpopmodder: Isolate passive diagnostic emission from terminal decisions.
from __future__ import annotations

from .goto_terminal_diagnostic_formatter import GotoTerminalDiagnosticFormatter
from .goto_terminal_diagnostic_projector import GotoTerminalDiagnosticProjector


class GotoTerminalDiagnosticObserver:
    def __init__(self, log_callback=None, *, role="late_terminal", projector=None, formatter=None):
        self._log_callback = log_callback
        self._role = role
        self._projector = projector or GotoTerminalDiagnosticProjector()
        self._formatter = formatter or GotoTerminalDiagnosticFormatter()

    def evidence_decided(self, **values) -> None:
        self._observe("evidence", values)

    def response_rendered(self, **values) -> None:
        self._observe("response", values)

    def _observe(self, method, values) -> None:
        if self._log_callback is None:
            return
        try:
            project = getattr(self._projector, method)
            record = project(role=self._role, **values)
            if record is not None:
                self._log_callback(self._formatter.format(record))
        except Exception:
            # No retry, terminal mutation, or replacement result on diagnostic failure.
            pass
