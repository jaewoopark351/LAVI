#20260901_kpopmodder: Validate P1 StoreHome activation and terminal event facts independently of flow selection.
from __future__ import annotations

from ..diagnostic_record import ProductionDiagnosticRecord
from .record_contract import (
    placeholder,
    required_field,
    required_non_negative_integer,
)


def verify_p1_activation(
    record: ProductionDiagnosticRecord,
    *,
    expected_candidate_position: str,
) -> str | None:
    candidate_activated, error = required_field(record, "candidateActivated")
    if error is not None:
        return error
    if candidate_activated != "true":
        return "P1_STORE_HOME_CANDIDATE_NOT_ACTIVATED"

    activation_result, error = required_field(record, "activationResult")
    if error is not None:
        return error
    if activation_result != "EXACT_SESSION_INSTALLED":
        return "P1_STORE_HOME_ACTIVATION_RESULT_NOT_EXACT_SESSION"

    destination_id, error = required_field(record, "destinationId")
    if error is not None:
        return error
    if placeholder(destination_id):
        return "P1_STORE_HOME_DESTINATION_ID_PLACEHOLDER"

    candidate_position, error = required_field(record, "candidatePosition")
    if error is not None:
        return error
    if placeholder(candidate_position):
        return "P1_STORE_HOME_CANDIDATE_POSITION_PLACEHOLDER"
    if candidate_position != expected_candidate_position:
        return "P1_STORE_HOME_CANDIDATE_POSITION_MISMATCH"
    return None


def verify_p1_terminal(record: ProductionDiagnosticRecord) -> str | None:
    terminal_result, error = required_field(record, "terminalResult")
    if error is not None:
        return error
    operation_result, error = required_field(record, "operationResult")
    if error is not None:
        return error
    if terminal_result != "COMPLETED":
        return "P1_STORE_HOME_TERMINAL_RESULT_NOT_COMPLETED"
    if operation_result != "COMPLETED":
        return "P1_STORE_HOME_OPERATION_RESULT_NOT_COMPLETED"

    remaining, error = required_non_negative_integer(
        record,
        "remainingStackCount",
    )
    if error is not None:
        return error
    if remaining != 0:
        return "P1_STORE_HOME_REMAINING_STACKS_NONZERO"

    stored_items, error = required_non_negative_integer(record, "storedItems")
    if error is not None:
        return error
    if stored_items < 1:
        return "P1_STORE_HOME_STORED_ITEMS_NOT_OBSERVED"

    touched_stacks, error = required_non_negative_integer(
        record,
        "touchedStackCount",
    )
    if error is not None:
        return error
    if touched_stacks < 1:
        return "P1_STORE_HOME_TOUCHED_STACKS_NOT_OBSERVED"
    return None
