#20260901_kpopmodder: Project only proven manual P1 StoreHome facts from bounded production diagnostic records.
from __future__ import annotations

from ..diagnostic_record import ProductionDiagnosticRecord
from .event_contract import verify_p1_activation, verify_p1_terminal
from .manual_command_context_contract import verify_manual_p1_command_context
from .projection_result import (
    P1_STORE_HOME_FALSE_EVIDENCE,
    P1_STORE_HOME_TRUE_EVIDENCE,
    P1StoreHomeRuntimeEvidenceProjection,
)
from .record_contract import (
    consistent_non_placeholder_field,
    first_ambiguous_repeated_field,
    placeholder,
    positive_canonical_integer,
    record_identity_problem,
    required_field,
    strictly_increasing,
)


_START_EVENT = "STORE_HOME_OPERATION_STARTED"
_ACTIVATION_EVENT = "STORE_HOME_CANDIDATE_ACTIVATED"
_TERMINAL_EVENT = "STORE_HOME_OPERATION_TERMINAL_SUMMARY"
_P1_EVENTS = frozenset({_START_EVENT, _ACTIVATION_EVENT, _TERMINAL_EVENT})
_REQUIRED_MARKER = "[LAVI ChatClefBoundary]"
_REQUIRED_ENVELOPE_KIND = "BOUNDED_EVENT"


def project_p1_store_home_runtime_evidence(
    records: tuple[ProductionDiagnosticRecord, ...],
    *,
    expected_candidate_position: object,
    expected_run_manifest_id: object,
) -> P1StoreHomeRuntimeEvidenceProjection:
    if not isinstance(records, tuple):
        return _failure("P1_STORE_HOME_RECORDS_NOT_TUPLE")
    if not isinstance(expected_candidate_position, str):
        return _failure("P1_STORE_HOME_EXPECTED_CANDIDATE_POSITION_NOT_TEXT")
    if placeholder(expected_candidate_position):
        return _failure("P1_STORE_HOME_EXPECTED_CANDIDATE_POSITION_INVALID")
    expected_manifest_error = _expected_identity_error(
        expected_run_manifest_id,
        "RUN_MANIFEST_ID",
    )
    if expected_manifest_error is not None:
        return _failure(expected_manifest_error)
    if any(not isinstance(record, ProductionDiagnosticRecord) for record in records):
        return _failure("P1_STORE_HOME_RECORD_NOT_TYPED")

    selected = tuple(record for record in records if record.event_name in _P1_EVENTS)
    for record in selected:
        identity_problem = record_identity_problem(record)
        if identity_problem is not None:
            return _failure(identity_problem)
        if record.marker != _REQUIRED_MARKER:
            return _failure(
                f"P1_STORE_HOME_RECORD_MARKER_INVALID:{record.event_name}"
            )
        if record.envelope_kind != _REQUIRED_ENVELOPE_KIND:
            return _failure(
                f"P1_STORE_HOME_RECORD_ENVELOPE_INVALID:{record.event_name}"
            )
    if not strictly_increasing(record.event_sequence for record in selected):
        return _failure("P1_STORE_HOME_EVENT_SEQUENCE_NOT_STRICTLY_INCREASING")

    ambiguous_field = first_ambiguous_repeated_field(selected)
    if ambiguous_field is not None:
        event_name, key = ambiguous_field
        return _failure(
            f"P1_STORE_HOME_FIELD_REPEATS_AMBIGUOUS:{event_name}:{key}"
        )

    operation_values: list[str] = []
    for record in selected:
        operation_id, error = required_field(record, "operationId")
        if error is not None:
            return _failure(error)
        if not positive_canonical_integer(operation_id):
            return _failure(
                f"P1_STORE_HOME_OPERATION_ID_INVALID:{record.event_name}"
            )
        operation_values.append(operation_id)
    operation_ids = tuple(dict.fromkeys(operation_values))
    if len(operation_ids) > 1:
        return _failure("P1_STORE_HOME_MULTIPLE_OPERATIONS")
    operation_id = operation_ids[0] if operation_ids else ""

    grouped = {
        event_name: tuple(
            record for record in selected if record.event_name == event_name
        )
        for event_name in _P1_EVENTS
    }
    for event_name, label in (
        (_START_EVENT, "START"),
        (_ACTIVATION_EVENT, "ACTIVATION"),
        (_TERMINAL_EVENT, "TERMINAL"),
    ):
        count = len(grouped[event_name])
        if count == 0:
            return _failure(
                f"P1_STORE_HOME_{label}_MISSING",
                operation_id=operation_id,
            )
        if count != 1:
            return _failure(
                f"P1_STORE_HOME_{label}_DUPLICATE",
                operation_id=operation_id,
            )

    for record in (
        grouped[_START_EVENT][0],
        grouped[_ACTIVATION_EVENT][0],
        grouped[_TERMINAL_EVENT][0],
    ):
        command_context_error = verify_manual_p1_command_context(record)
        if command_context_error is not None:
            return _failure(command_context_error, operation_id=operation_id)

    run_manifest_id, manifest_error = consistent_non_placeholder_field(
        selected,
        "runManifestId",
        "RUN_MANIFEST_ID",
    )
    if manifest_error is not None:
        return _failure(
            manifest_error,
            operation_id=operation_id,
        )
    if run_manifest_id != expected_run_manifest_id:
        return _failure(
            "P1_STORE_HOME_RUN_MANIFEST_ID_NOT_EXPECTED",
            operation_id=operation_id,
        )

    activation = grouped[_ACTIVATION_EVENT][0]
    activation_error = verify_p1_activation(
        activation,
        expected_candidate_position=expected_candidate_position,
    )
    if activation_error is not None:
        return _failure(
            activation_error,
            operation_id=operation_id,
        )

    terminal = grouped[_TERMINAL_EVENT][0]
    terminal_error = verify_p1_terminal(terminal)
    if terminal_error is not None:
        return _failure(
            terminal_error,
            operation_id=operation_id,
        )

    return P1StoreHomeRuntimeEvidenceProjection(
        ok=True,
        reason="P1_STORE_HOME_RUNTIME_EVIDENCE_VERIFIED",
        operation_id=operation_id,
        command_request_id="",
        evidence=P1_STORE_HOME_TRUE_EVIDENCE,
    )


def _failure(
    reason: str,
    *,
    operation_id: str = "",
    command_request_id: str = "",
) -> P1StoreHomeRuntimeEvidenceProjection:
    return P1StoreHomeRuntimeEvidenceProjection(
        ok=False,
        reason=reason,
        operation_id=operation_id,
        command_request_id=command_request_id,
        evidence=P1_STORE_HOME_FALSE_EVIDENCE,
    )


def _expected_identity_error(value: object, label: str) -> str | None:
    if not isinstance(value, str):
        return f"P1_STORE_HOME_EXPECTED_{label}_NOT_TEXT"
    if (
        len(value) > 512
        or placeholder(value)
        or any(ord(character) < 0x20 or ord(character) == 0x7F for character in value)
    ):
        return f"P1_STORE_HOME_EXPECTED_{label}_INVALID"
    return None
