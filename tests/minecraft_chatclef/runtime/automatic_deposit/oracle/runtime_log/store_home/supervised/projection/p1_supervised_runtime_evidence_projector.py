#20260901_kpopmodder: Project only a complete externally bound supervised StoreHome lifecycle.
from __future__ import annotations

from ....diagnostic_record import ProductionDiagnosticRecord
from ...event_contract import verify_p1_activation, verify_p1_terminal
from ...projection_result import (
    P1_STORE_HOME_FALSE_EVIDENCE,
    P1_STORE_HOME_TRUE_EVIDENCE,
    P1StoreHomeRuntimeEvidenceProjection,
)
from ...record_contract import (
    consistent_non_placeholder_field,
    first_ambiguous_repeated_field,
    placeholder,
    positive_canonical_integer,
    record_identity_problem,
    required_field,
    strictly_increasing,
)
from ..context.p1_supervised_command_context_contract import (
    verify_p1_supervised_command_context,
)
from ..context.p1_supervised_run_manifest_event_contract import (
    verify_p1_supervised_run_manifest_event,
)


_MANIFEST_EVENT = "STORE_HOME_RUN_MANIFEST"
_START_EVENT = "STORE_HOME_OPERATION_STARTED"
_ACTIVATION_EVENT = "STORE_HOME_CANDIDATE_ACTIVATED"
_TERMINAL_EVENT = "STORE_HOME_OPERATION_TERMINAL_SUMMARY"
_EVENT_ORDER = (
    _MANIFEST_EVENT,
    _START_EVENT,
    _ACTIVATION_EVENT,
    _TERMINAL_EVENT,
)
_P1_EVENTS = frozenset(_EVENT_ORDER)
_REQUIRED_MARKER = "[LAVI ChatClefBoundary]"
_REQUIRED_ENVELOPE_KIND = "BOUNDED_EVENT"


def project_p1_supervised_store_home_runtime_evidence(
    records: tuple[ProductionDiagnosticRecord, ...],
    *,
    expected_candidate_position: object,
    expected_run_manifest_id: object,
    expected_command_request_id: object,
    expected_command_session_id: object,
    expected_world_key: object,
    expected_dimension: object,
    expected_destination_canonical_key: object,
    expected_destination_id: object,
) -> P1StoreHomeRuntimeEvidenceProjection:
    if not isinstance(records, tuple):
        return _failure("P1_STORE_HOME_RECORDS_NOT_TUPLE")
    if not isinstance(expected_candidate_position, str):
        return _failure("P1_STORE_HOME_EXPECTED_CANDIDATE_POSITION_NOT_TEXT")
    if placeholder(expected_candidate_position):
        return _failure("P1_STORE_HOME_EXPECTED_CANDIDATE_POSITION_INVALID")
    for value, label in (
        (expected_run_manifest_id, "RUN_MANIFEST_ID"),
        (expected_command_request_id, "COMMAND_REQUEST_ID"),
        (expected_command_session_id, "COMMAND_SESSION_ID"),
        (expected_world_key, "WORLD_KEY"),
        (expected_dimension, "DIMENSION"),
        (expected_destination_canonical_key, "DESTINATION_CANONICAL_KEY"),
        (expected_destination_id, "DESTINATION_ID"),
    ):
        error = _expected_identity_error(value, label)
        if error is not None:
            return _failure(error)
    if any(not isinstance(record, ProductionDiagnosticRecord) for record in records):
        return _failure("P1_STORE_HOME_RECORD_NOT_TYPED")

    selected = tuple(record for record in records if record.event_name in _P1_EVENTS)
    for record in selected:
        identity_error = record_identity_problem(record)
        if identity_error is not None:
            return _failure(identity_error)
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
    ambiguous = first_ambiguous_repeated_field(selected)
    if ambiguous is not None:
        event_name, key = ambiguous
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
        for event_name in _EVENT_ORDER
    }
    labels = (
        (_MANIFEST_EVENT, "RUN_MANIFEST_EVENT"),
        (_START_EVENT, "START"),
        (_ACTIVATION_EVENT, "ACTIVATION"),
        (_TERMINAL_EVENT, "TERMINAL"),
    )
    for event_name, label in labels:
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
    ordered = tuple(grouped[event_name][0] for event_name in _EVENT_ORDER)
    if tuple(record.event_name for record in selected) != _EVENT_ORDER:
        return _failure(
            "P1_STORE_HOME_EVENT_ORDER_INVALID",
            operation_id=operation_id,
        )

    for record in ordered:
        context_error = verify_p1_supervised_command_context(
            record,
            expected_command_request_id=expected_command_request_id,
        )
        if context_error is not None:
            return _failure(context_error, operation_id=operation_id)
    context_values: dict[str, str] = {}
    for key, label in (
        ("commandCorrelationId", "COMMAND_CORRELATION_ID"),
        ("commandSessionId", "COMMAND_SESSION_ID"),
        ("commandConnectionGeneration", "COMMAND_CONNECTION_GENERATION"),
    ):
        value, error = consistent_non_placeholder_field(ordered, key, label)
        if error is not None:
            return _failure(error, operation_id=operation_id)
        context_values[key] = value
    if context_values["commandSessionId"] != expected_command_session_id:
        return _failure(
            "P1_STORE_HOME_COMMAND_SESSION_ID_NOT_EXPECTED",
            operation_id=operation_id,
        )

    manifest_error = verify_p1_supervised_run_manifest_event(
        grouped[_MANIFEST_EVENT][0],
        expected_run_manifest_id=expected_run_manifest_id,
    )
    if manifest_error is not None:
        return _failure(manifest_error, operation_id=operation_id)
    run_manifest_id, manifest_error = consistent_non_placeholder_field(
        ordered,
        "runManifestId",
        "RUN_MANIFEST_ID",
    )
    if manifest_error is not None:
        return _failure(manifest_error, operation_id=operation_id)
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
        return _failure(activation_error, operation_id=operation_id)
    for key, expected, label in (
        ("worldKey", expected_world_key, "WORLD_KEY"),
        ("dimension", expected_dimension, "DIMENSION"),
        (
            "destinationCanonicalKey",
            expected_destination_canonical_key,
            "DESTINATION_CANONICAL_KEY",
        ),
        ("destinationId", expected_destination_id, "DESTINATION_ID"),
    ):
        actual, field_error = required_field(activation, key)
        if field_error is not None:
            return _failure(field_error, operation_id=operation_id)
        if actual != expected:
            return _failure(
                f"P1_STORE_HOME_{label}_NOT_EXPECTED",
                operation_id=operation_id,
            )
    terminal_error = verify_p1_terminal(grouped[_TERMINAL_EVENT][0])
    if terminal_error is not None:
        return _failure(terminal_error, operation_id=operation_id)
    return P1StoreHomeRuntimeEvidenceProjection(
        ok=True,
        reason="P1_SUPERVISED_STORE_HOME_RUNTIME_EVIDENCE_VERIFIED",
        operation_id=operation_id,
        command_request_id=expected_command_request_id,
        evidence=P1_STORE_HOME_TRUE_EVIDENCE,
    )


def _failure(
    reason: str,
    *,
    operation_id: str = "",
) -> P1StoreHomeRuntimeEvidenceProjection:
    return P1StoreHomeRuntimeEvidenceProjection(
        ok=False,
        reason=reason,
        operation_id=operation_id,
        command_request_id="",
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
