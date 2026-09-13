#20260913_kpopmodder: Format fixed-size GOTO diagnostics without raw text or payloads.
from __future__ import annotations

from dataclasses import fields

from .goto_terminal_diagnostic_record import GotoTerminalDiagnosticRecord


class GotoTerminalDiagnosticFormatter:
    MAX_RECORD_CHARS = 2304

    def format(self, record: GotoTerminalDiagnosticRecord) -> str:
        if type(record) is not GotoTerminalDiagnosticRecord:
            raise TypeError("expected an exact GOTO diagnostic record")
        message = "event=goto_terminal_diagnostic " + " ".join(
            f"{field.name}={getattr(record, field.name)}" for field in fields(record)
        )
        if len(message) > self.MAX_RECORD_CHARS:
            raise ValueError("GOTO diagnostic record exceeded its fixed bound")
        return message
