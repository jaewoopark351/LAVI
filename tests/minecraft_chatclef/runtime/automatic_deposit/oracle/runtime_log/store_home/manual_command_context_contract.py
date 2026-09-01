#20260901_kpopmodder: Keep manual P1 command absence separate from supervised bridge identity correlation.
from __future__ import annotations

from ..diagnostic_record import ProductionDiagnosticRecord
from .record_contract import required_field


_MANUAL_COMMAND_CONTEXT = (
    ("commandContextAvailable", "false"),
    ("commandRequestId", ""),
    ("commandCorrelationId", ""),
    ("commandSessionId", ""),
    ("commandConnectionGeneration", "unavailable"),
    ("commandText", ""),
    ("commandSource", ""),
    ("commandContextError", "no_active_command"),
)


def verify_manual_p1_command_context(
    record: ProductionDiagnosticRecord,
) -> str | None:
    for key, expected_value in _MANUAL_COMMAND_CONTEXT:
        value, error = required_field(record, key)
        if error is not None:
            return error
        if value != expected_value:
            return (
                "P1_STORE_HOME_MANUAL_COMMAND_CONTEXT_CONTAMINATED:"
                f"{record.event_name}:{key}"
            )
    return None
