#20260901_kpopmodder: Prove P1 StoreHome projection is complete, correlated, and fail-closed.
from __future__ import annotations

import unittest

from ..diagnostic_record import ProductionDiagnosticRecord
from .p1_runtime_evidence_projector import (
    project_p1_store_home_runtime_evidence as _project_p1_store_home_runtime_evidence,
)


_POSITION = "12, 64, -9"
_RUN_MANIFEST_ID = "manifest-p1"
_TRUE_EVIDENCE = {
    "runtime_reported_completion": True,
    "trusted_binding_verified": True,
    "transfer_observed": True,
    "typed_terminal_observed": True,
}
_FALSE_EVIDENCE = {key: False for key in _TRUE_EVIDENCE}


class P1StoreHomeRuntimeEvidenceProjectorTests(unittest.TestCase):
    def test_projects_all_runtime_evidence_only_from_complete_correlated_flow(self):
        unrelated = _record("SOME_OTHER_EVENT", 1, operation_id="999")

        result = project_p1_store_home_runtime_evidence(
            (unrelated, *_successful_records(sequence_offset=1)),
            expected_candidate_position=_POSITION,
        )

        self.assertTrue(result.ok)
        self.assertEqual("P1_STORE_HOME_RUNTIME_EVIDENCE_VERIFIED", result.reason)
        self.assertEqual("17", result.operation_id)
        self.assertEqual("", result.command_request_id)
        self.assertEqual(_TRUE_EVIDENCE, result.evidence_mapping())

    def test_rejects_more_than_one_store_home_operation(self):
        result = project_p1_store_home_runtime_evidence(
            (
                *_successful_records(operation_id="17"),
                *_successful_records(operation_id="18", sequence_offset=3),
            ),
            expected_candidate_position=_POSITION,
        )

        self._assert_failed(result, "P1_STORE_HOME_MULTIPLE_OPERATIONS")

    def test_rejects_each_missing_required_boundary(self):
        cases = (
            (
                "STORE_HOME_OPERATION_STARTED",
                "P1_STORE_HOME_START_MISSING",
            ),
            (
                "STORE_HOME_CANDIDATE_ACTIVATED",
                "P1_STORE_HOME_ACTIVATION_MISSING",
            ),
            (
                "STORE_HOME_OPERATION_TERMINAL_SUMMARY",
                "P1_STORE_HOME_TERMINAL_MISSING",
            ),
        )
        for missing_event, expected_reason in cases:
            with self.subTest(missing_event=missing_event):
                records = tuple(
                    record
                    for record in _successful_records()
                    if record.event_name != missing_event
                )
                result = project_p1_store_home_runtime_evidence(
                    records,
                    expected_candidate_position=_POSITION,
                )
                self._assert_failed(result, expected_reason)

    def test_rejects_duplicate_terminal_boundary(self):
        start, activation, terminal = _successful_records()
        duplicate_terminal = _with_sequence(terminal, 4)

        result = project_p1_store_home_runtime_evidence(
            (start, activation, terminal, duplicate_terminal),
            expected_candidate_position=_POSITION,
        )

        self._assert_failed(result, "P1_STORE_HOME_TERMINAL_DUPLICATE")

    def test_rejects_candidate_position_mismatch(self):
        result = project_p1_store_home_runtime_evidence(
            _successful_records(),
            expected_candidate_position="13, 64, -9",
        )

        self._assert_failed(result, "P1_STORE_HOME_CANDIDATE_POSITION_MISMATCH")

    def test_rejects_non_text_expected_candidate_position_explicitly(self):
        result = project_p1_store_home_runtime_evidence(
            _successful_records(),
            expected_candidate_position=None,
        )

        self._assert_failed(
            result,
            "P1_STORE_HOME_EXPECTED_CANDIDATE_POSITION_NOT_TEXT",
        )

    def test_rejects_wrong_marker_or_envelope_on_selected_record(self):
        cases = (
            (
                "[LAVI ChatClefDiag]",
                "BOUNDED_EVENT",
                "P1_STORE_HOME_RECORD_MARKER_INVALID:"
                "STORE_HOME_CANDIDATE_ACTIVATED",
            ),
            (
                "[LAVI ChatClefBoundary]",
                "AUTO_DEPOSIT_ALL_PHASE",
                "P1_STORE_HOME_RECORD_ENVELOPE_INVALID:"
                "STORE_HOME_CANDIDATE_ACTIVATED",
            ),
        )
        for marker, envelope_kind, expected_reason in cases:
            with self.subTest(marker=marker, envelope_kind=envelope_kind):
                start, activation, terminal = _successful_records()
                activation = ProductionDiagnosticRecord(
                    marker=marker,
                    envelope_kind=envelope_kind,
                    event_name=activation.event_name,
                    event_sequence=activation.event_sequence,
                    ordered_fields=activation.ordered_fields,
                )
                result = project_p1_store_home_runtime_evidence(
                    (start, activation, terminal),
                    expected_candidate_position=_POSITION,
                )
                self._assert_failed(result, expected_reason)

    def test_rejects_record_attributes_inconsistent_with_ordered_fields(self):
        start, activation, terminal = _successful_records()
        inconsistent_event = _replace_field(activation, "event", "OTHER_EVENT")
        inconsistent_sequence = ProductionDiagnosticRecord(
            marker=activation.marker,
            envelope_kind=activation.envelope_kind,
            event_name=activation.event_name,
            event_sequence=999,
            ordered_fields=activation.ordered_fields,
        )

        event_result = project_p1_store_home_runtime_evidence(
            (start, inconsistent_event, terminal),
            expected_candidate_position=_POSITION,
        )
        sequence_result = project_p1_store_home_runtime_evidence(
            (start, inconsistent_sequence, terminal),
            expected_candidate_position=_POSITION,
        )

        self._assert_failed(
            event_result,
            "P1_STORE_HOME_RECORD_EVENT_NAME_INCONSISTENT:"
            "STORE_HOME_CANDIDATE_ACTIVATED",
        )
        self._assert_failed(
            sequence_result,
            "P1_STORE_HOME_RECORD_EVENT_SEQUENCE_INCONSISTENT:"
            "STORE_HOME_CANDIDATE_ACTIVATED",
        )

    def test_rejects_conflicting_duplicate_field(self):
        start, activation, terminal = _successful_records()
        conflicting_activation = _append_field(
            activation,
            "candidatePosition",
            "99, 70, 99",
        )

        result = project_p1_store_home_runtime_evidence(
            (start, conflicting_activation, terminal),
            expected_candidate_position=_POSITION,
        )

        self._assert_failed(
            result,
            "P1_STORE_HOME_FIELD_REPEATS_AMBIGUOUS:"
            "STORE_HOME_CANDIDATE_ACTIVATED:candidatePosition",
        )

    def test_accepts_conservatively_resolvable_placeholder_then_value(self):
        start, activation, terminal = _successful_records()
        activation = _prepend_field(activation, "destinationId", "UNAVAILABLE")

        result = project_p1_store_home_runtime_evidence(
            (start, activation, terminal),
            expected_candidate_position=_POSITION,
        )

        self.assertTrue(result.ok)
        self.assertEqual(_TRUE_EVIDENCE, result.evidence_mapping())

    def test_rejects_nonmonotonic_event_sequence(self):
        start, activation, terminal = _successful_records()

        result = project_p1_store_home_runtime_evidence(
            (start, _with_sequence(activation, 3), _with_sequence(terminal, 2)),
            expected_candidate_position=_POSITION,
        )

        self._assert_failed(
            result,
            "P1_STORE_HOME_EVENT_SEQUENCE_NOT_STRICTLY_INCREASING",
        )

    def test_rejects_noncompleted_terminal_results(self):
        for key in ("terminalResult", "operationResult"):
            with self.subTest(key=key):
                start, activation, terminal = _successful_records()
                terminal = _replace_field(terminal, key, "FAILED")
                result = project_p1_store_home_runtime_evidence(
                    (start, activation, terminal),
                    expected_candidate_position=_POSITION,
                )
                expected_reason = (
                    "P1_STORE_HOME_TERMINAL_RESULT_NOT_COMPLETED"
                    if key == "terminalResult"
                    else "P1_STORE_HOME_OPERATION_RESULT_NOT_COMPLETED"
                )
                self._assert_failed(result, expected_reason)

    def test_rejects_remaining_stacks_or_absent_transfer(self):
        cases = (
            (
                "remainingStackCount",
                "1",
                "P1_STORE_HOME_REMAINING_STACKS_NONZERO",
            ),
            (
                "storedItems",
                "0",
                "P1_STORE_HOME_STORED_ITEMS_NOT_OBSERVED",
            ),
            (
                "touchedStackCount",
                "0",
                "P1_STORE_HOME_TOUCHED_STACKS_NOT_OBSERVED",
            ),
        )
        for key, value, expected_reason in cases:
            with self.subTest(key=key):
                start, activation, terminal = _successful_records()
                terminal = _replace_field(terminal, key, value)
                result = project_p1_store_home_runtime_evidence(
                    (start, activation, terminal),
                    expected_candidate_position=_POSITION,
                )
                self._assert_failed(result, expected_reason)

    def test_requires_complete_manual_command_context_on_every_boundary(self):
        manual_context_keys = (
            "commandContextAvailable",
            "commandRequestId",
            "commandCorrelationId",
            "commandSessionId",
            "commandConnectionGeneration",
            "commandText",
            "commandSource",
            "commandContextError",
        )
        for index in range(3):
            for key in manual_context_keys:
                with self.subTest(record_index=index, key=key):
                    records = list(_successful_records())
                    records[index] = _remove_field(records[index], key)
                    result = project_p1_store_home_runtime_evidence(
                        tuple(records),
                        expected_candidate_position=_POSITION,
                    )
                    self._assert_failed(
                        result,
                        "P1_STORE_HOME_FIELD_MISSING:"
                        f"{records[index].event_name}:{key}",
                    )

    def test_rejects_bridge_command_context_as_manual_p1_contamination(self):
        contamination_cases = (
            ("commandContextAvailable", "true"),
            ("commandRequestId", "request-p1"),
            ("commandCorrelationId", "correlation-p1"),
            ("commandSessionId", "session-p1"),
            ("commandConnectionGeneration", "7"),
            ("commandText", "@store_home"),
            ("commandSource", "LAVI_GUI"),
            ("commandContextError", "none"),
        )
        for key, value in contamination_cases:
            with self.subTest(key=key):
                start, activation, terminal = _successful_records()
                activation = _replace_field(activation, key, value)
                result = project_p1_store_home_runtime_evidence(
                    (start, activation, terminal),
                    expected_candidate_position=_POSITION,
                )
                self._assert_failed(
                    result,
                    "P1_STORE_HOME_MANUAL_COMMAND_CONTEXT_CONTAMINATED:"
                    f"STORE_HOME_CANDIDATE_ACTIVATED:{key}",
                )

    def test_rejects_manifest_identity_mismatch(self):
        start, activation, terminal = _successful_records()
        activation = _replace_field(activation, "runManifestId", "other-identity")

        result = project_p1_store_home_runtime_evidence(
            (start, activation, terminal),
            expected_candidate_position=_POSITION,
        )

        self._assert_failed(result, "P1_STORE_HOME_RUN_MANIFEST_ID_MISMATCH")

    def test_rejects_a_different_externally_expected_run_identity(self):
        result = _project_p1_store_home_runtime_evidence(
            _successful_records(),
            expected_candidate_position=_POSITION,
            expected_run_manifest_id="other-manifest",
        )

        self._assert_failed(result, "P1_STORE_HOME_RUN_MANIFEST_ID_NOT_EXPECTED")

    def test_rejects_unusable_externally_expected_identity(self):
        cases = (
            (
                {"expected_run_manifest_id": None},
                "P1_STORE_HOME_EXPECTED_RUN_MANIFEST_ID_NOT_TEXT",
            ),
            (
                {"expected_run_manifest_id": "UNAVAILABLE"},
                "P1_STORE_HOME_EXPECTED_RUN_MANIFEST_ID_INVALID",
            ),
        )
        for overrides, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                result = _project_p1_store_home_runtime_evidence(
                    _successful_records(),
                    expected_candidate_position=_POSITION,
                    expected_run_manifest_id=overrides.get(
                        "expected_run_manifest_id",
                        _RUN_MANIFEST_ID,
                    ),
                )
                self._assert_failed(result, expected_reason)

    def test_rejects_inexact_activation_or_placeholder_destination(self):
        cases = (
            (
                "candidateActivated",
                "false",
                "P1_STORE_HOME_CANDIDATE_NOT_ACTIVATED",
            ),
            (
                "activationResult",
                "FALLBACK_SESSION",
                "P1_STORE_HOME_ACTIVATION_RESULT_NOT_EXACT_SESSION",
            ),
            (
                "destinationId",
                "unavailable",
                "P1_STORE_HOME_DESTINATION_ID_PLACEHOLDER",
            ),
        )
        for key, value, expected_reason in cases:
            with self.subTest(key=key):
                start, activation, terminal = _successful_records()
                activation = _replace_field(activation, key, value)
                result = project_p1_store_home_runtime_evidence(
                    (start, activation, terminal),
                    expected_candidate_position=_POSITION,
                )
                self._assert_failed(result, expected_reason)

    def _assert_failed(self, result, expected_reason: str) -> None:
        self.assertFalse(result.ok)
        self.assertEqual(expected_reason, result.reason)
        self.assertEqual(_FALSE_EVIDENCE, result.evidence_mapping())


def _successful_records(
    *,
    operation_id: str = "17",
    sequence_offset: int = 0,
) -> tuple[ProductionDiagnosticRecord, ...]:
    return (
        _record(
            "STORE_HOME_OPERATION_STARTED",
            1 + sequence_offset,
            operation_id=operation_id,
        ),
        _record(
            "STORE_HOME_CANDIDATE_ACTIVATED",
            2 + sequence_offset,
            operation_id=operation_id,
            extra=(
                ("candidateActivated", "true"),
                ("activationResult", "EXACT_SESSION_INSTALLED"),
                ("destinationId", "trusted-home-1"),
                ("candidatePosition", _POSITION),
            ),
        ),
        _record(
            "STORE_HOME_OPERATION_TERMINAL_SUMMARY",
            3 + sequence_offset,
            operation_id=operation_id,
            extra=(
                ("terminalResult", "COMPLETED"),
                ("operationResult", "COMPLETED"),
                ("remainingStackCount", "0"),
                ("storedItems", "64"),
                ("touchedStackCount", "2"),
            ),
        ),
    )


def project_p1_store_home_runtime_evidence(
    records: tuple[ProductionDiagnosticRecord, ...],
    *,
    expected_candidate_position: object,
):
    return _project_p1_store_home_runtime_evidence(
        records,
        expected_candidate_position=expected_candidate_position,
        expected_run_manifest_id=_RUN_MANIFEST_ID,
    )


def _record(
    event_name: str,
    event_sequence: int,
    *,
    operation_id: str,
    extra: tuple[tuple[str, str], ...] = (),
) -> ProductionDiagnosticRecord:
    fields = (
        ("traceId", "trace-p1"),
        ("eventSequence", str(event_sequence)),
        ("event", event_name),
        ("runManifestId", "manifest-p1"),
        ("operationId", operation_id),
        ("commandContextAvailable", "false"),
        ("commandRequestId", ""),
        ("commandCorrelationId", ""),
        ("commandSessionId", ""),
        ("commandConnectionGeneration", "unavailable"),
        ("commandText", ""),
        ("commandSource", ""),
        ("commandContextError", "no_active_command"),
        *extra,
    )
    return ProductionDiagnosticRecord(
        marker="[LAVI ChatClefBoundary]",
        envelope_kind="BOUNDED_EVENT",
        event_name=event_name,
        event_sequence=event_sequence,
        ordered_fields=fields,
    )


def _with_sequence(
    record: ProductionDiagnosticRecord,
    sequence: int,
) -> ProductionDiagnosticRecord:
    return ProductionDiagnosticRecord(
        marker=record.marker,
        envelope_kind=record.envelope_kind,
        event_name=record.event_name,
        event_sequence=sequence,
        ordered_fields=tuple(
            (key, str(sequence) if key == "eventSequence" else value)
            for key, value in record.ordered_fields
        ),
    )


def _replace_field(
    record: ProductionDiagnosticRecord,
    key_to_replace: str,
    replacement: str,
) -> ProductionDiagnosticRecord:
    return _with_fields(
        record,
        tuple(
            (key, replacement if key == key_to_replace else value)
            for key, value in record.ordered_fields
        ),
    )


def _remove_field(
    record: ProductionDiagnosticRecord,
    key_to_remove: str,
) -> ProductionDiagnosticRecord:
    return _with_fields(
        record,
        tuple(
            (key, value)
            for key, value in record.ordered_fields
            if key != key_to_remove
        ),
    )


def _append_field(
    record: ProductionDiagnosticRecord,
    key: str,
    value: str,
) -> ProductionDiagnosticRecord:
    return _with_fields(record, (*record.ordered_fields, (key, value)))


def _prepend_field(
    record: ProductionDiagnosticRecord,
    key: str,
    value: str,
) -> ProductionDiagnosticRecord:
    return _with_fields(record, ((key, value), *record.ordered_fields))


def _with_fields(
    record: ProductionDiagnosticRecord,
    fields: tuple[tuple[str, str], ...],
) -> ProductionDiagnosticRecord:
    return ProductionDiagnosticRecord(
        marker=record.marker,
        envelope_kind=record.envelope_kind,
        event_name=record.event_name,
        event_sequence=record.event_sequence,
        ordered_fields=fields,
    )


if __name__ == "__main__":
    unittest.main()
