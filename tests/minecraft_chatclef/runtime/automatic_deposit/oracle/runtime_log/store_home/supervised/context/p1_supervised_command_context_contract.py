#20260901_kpopmodder: Validate one supervised StoreHome boundary against the actual accepted request identity.
from __future__ import annotations

from ....diagnostic_record import ProductionDiagnosticRecord
from ...record_contract import placeholder, positive_canonical_integer, required_field


def verify_p1_supervised_command_context(
    record: ProductionDiagnosticRecord,
    *,
    expected_command_request_id: str,
) -> str | None:
    available, error = required_field(record, "commandContextAvailable")
    if error is not None:
        return error
    if available != "true":
        return f"P1_STORE_HOME_SUPERVISED_COMMAND_CONTEXT_UNAVAILABLE:{record.event_name}"
    request_id, error = required_field(record, "commandRequestId")
    if error is not None:
        return error
    if request_id != expected_command_request_id:
        return "P1_STORE_HOME_COMMAND_REQUEST_ID_NOT_EXPECTED"
    for key, expected, label in (
        ("commandText", "@store_home", "COMMAND_TEXT"),
        ("commandSource", "lavi_gui", "COMMAND_SOURCE"),
        ("commandContextError", "none", "COMMAND_CONTEXT_ERROR"),
    ):
        value, error = required_field(record, key)
        if error is not None:
            return error
        if value != expected:
            return f"P1_STORE_HOME_{label}_INVALID:{record.event_name}"
    for key, label in (
        ("commandCorrelationId", "COMMAND_CORRELATION_ID"),
        ("commandSessionId", "COMMAND_SESSION_ID"),
    ):
        value, error = required_field(record, key)
        if error is not None:
            return error
        if placeholder(value):
            return f"P1_STORE_HOME_{label}_PLACEHOLDER:{record.event_name}"
    generation, error = required_field(record, "commandConnectionGeneration")
    if error is not None:
        return error
    if not positive_canonical_integer(generation):
        return (
            "P1_STORE_HOME_COMMAND_CONNECTION_GENERATION_INVALID:"
            f"{record.event_name}"
        )
    return None
