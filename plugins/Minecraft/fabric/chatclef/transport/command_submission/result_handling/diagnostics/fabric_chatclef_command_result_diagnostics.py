#20260907_kpopmodder: Report command-result diagnostics through the existing sink.
from __future__ import annotations

from typing import Any

from .fabric_chatclef_command_result_diagnostic_formatter import (
    FabricChatClefCommandResultDiagnosticFormatter,
)


class FabricChatClefCommandResultDiagnostics:
    def __init__(self, diagnostics: Any) -> None:
        self._diagnostics = diagnostics
        self._formatter = FabricChatClefCommandResultDiagnosticFormatter()

    def report_malformed(self, error: Exception) -> None:
        self._diagnostics.warning(self._formatter.malformed(error))

    def report_rejected(self, *, envelope: Any, result: Any, outcome: Any) -> None:
        self._diagnostics.warning(
            self._formatter.rejected(
                envelope=envelope,
                result=result,
                outcome=outcome,
            )
        )

    def report_accepted(self, *, result: Any, outcome: Any) -> None:
        self._diagnostics.info(
            self._formatter.accepted(result=result, outcome=outcome)
        )
