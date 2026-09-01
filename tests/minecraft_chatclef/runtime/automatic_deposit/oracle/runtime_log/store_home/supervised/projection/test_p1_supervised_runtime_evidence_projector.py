#20260901_kpopmodder: Require externally bound manifest and actual bridge request identity for supervised P1.
from __future__ import annotations

import hashlib
import unittest

from minecraft_chatclef.runtime.automatic_deposit.oracle.runtime_log.diagnostic_record import (
    ProductionDiagnosticRecord,
)

from .p1_supervised_runtime_evidence_projector import (
    project_p1_supervised_store_home_runtime_evidence,
)


_POSITION = "12, 64, -9"
_MANIFEST_ID = "opaque-p1-run"
_REQUEST_ID = "lavi-gui-request-17"
_SESSION_ID = "fabric-chatclef-session-1"
_WORLD_KEY = "world-1"
_DIMENSION = "OVERWORLD"
_DESTINATION_CANONICAL_KEY = "world-1|OVERWORLD|12|64|-9"
_DESTINATION_ID = "td_" + hashlib.sha256(
    _DESTINATION_CANONICAL_KEY.encode("utf-8")
).hexdigest()[:24]


class P1SupervisedRuntimeEvidenceProjectorTests(unittest.TestCase):
    def test_complete_manifest_start_activation_terminal_flow_preserves_actual_request(self):
        result = _project(_successful_records())

        self.assertTrue(result.ok, result.reason)
        self.assertEqual("P1_SUPERVISED_STORE_HOME_RUNTIME_EVIDENCE_VERIFIED", result.reason)
        self.assertEqual("17", result.operation_id)
        self.assertEqual(_REQUEST_ID, result.command_request_id)
        self.assertTrue(all(result.evidence_mapping().values()))

    def test_rejects_actual_request_id_mismatch_without_inference(self):
        records = _replace_field(
            _successful_records(),
            2,
            "commandRequestId",
            "lavi-gui-another-request",
        )

        result = _project(records)

        self._assert_failed(result, "P1_STORE_HOME_COMMAND_REQUEST_ID_NOT_EXPECTED")

    def test_rejects_runtime_session_or_exact_activation_fixture_mismatch(self):
        cases = (
            (1, "commandSessionId", "different-session", "P1_STORE_HOME_COMMAND_SESSION_ID_NOT_EXPECTED"),
            (2, "worldKey", "different-world", "P1_STORE_HOME_WORLD_KEY_NOT_EXPECTED"),
            (2, "dimension", "NETHER", "P1_STORE_HOME_DIMENSION_NOT_EXPECTED"),
            (
                2,
                "destinationCanonicalKey",
                "wrong|canonical|key",
                "P1_STORE_HOME_DESTINATION_CANONICAL_KEY_NOT_EXPECTED",
            ),
            (2, "destinationId", "td_wrong", "P1_STORE_HOME_DESTINATION_ID_NOT_EXPECTED"),
        )
        for index, key, value, expected_reason in cases:
            with self.subTest(key=key):
                records = (
                    _replace_all_fields(_successful_records(), key, value)
                    if key == "commandSessionId"
                    else _replace_field(_successful_records(), index, key, value)
                )
                self._assert_failed(_project(records), expected_reason)

    def test_rejects_missing_or_runtime_generated_manifest(self):
        cases = (
            (
                tuple(record for record in _successful_records() if record.event_name != "STORE_HOME_RUN_MANIFEST"),
                "P1_STORE_HOME_RUN_MANIFEST_EVENT_MISSING",
            ),
            (
                _replace_field(
                    _successful_records(),
                    0,
                    "runManifestIdSource",
                    "RUNTIME_GENERATED",
                ),
                "P1_STORE_HOME_RUN_MANIFEST_SOURCE_NOT_EXTERNAL",
            ),
        )
        for records, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                self._assert_failed(_project(records), expected_reason)

    def test_rejects_envelope_or_manifest_run_id_mismatch(self):
        cases = (
            ("runManifestId", "different-run"),
            ("runId", "different-run"),
        )
        for key, value in cases:
            with self.subTest(key=key):
                records = _replace_field(_successful_records(), 0, key, value)
                self._assert_failed(
                    _project(records),
                    "P1_STORE_HOME_RUN_MANIFEST_ID_NOT_EXPECTED",
                )

    def test_requires_exact_supervised_context_on_every_boundary(self):
        cases = (
            ("commandContextAvailable", "false"),
            ("commandText", "@deposit_all"),
            ("commandSource", "operator_manual"),
            ("commandContextError", "no_active_command"),
            ("commandCorrelationId", "UNAVAILABLE"),
            ("commandSessionId", ""),
            ("commandConnectionGeneration", "0"),
        )
        for key, value in cases:
            with self.subTest(key=key):
                records = _replace_field(_successful_records(), 1, key, value)
                result = _project(records)
                self.assertFalse(result.ok)
                self.assertEqual("", result.command_request_id)

    def test_rejects_context_identity_drift_and_event_order_drift(self):
        context_drift = _replace_field(
            _successful_records(),
            3,
            "commandSessionId",
            "other-session",
        )
        manifest, start, activation, terminal = _successful_records()
        reordered = (
            manifest,
            _with_sequence(start, 3),
            _with_sequence(activation, 2),
            terminal,
        )

        self._assert_failed(
            _project(context_drift),
            "P1_STORE_HOME_COMMAND_SESSION_ID_MISMATCH",
        )
        self._assert_failed(
            _project(reordered),
            "P1_STORE_HOME_EVENT_SEQUENCE_NOT_STRICTLY_INCREASING",
        )

    def test_rejects_duplicate_manifest_and_multiple_operations(self):
        manifest, start, activation, terminal = _successful_records()
        duplicate_manifest = _with_sequence(manifest, 2)
        duplicate_records = (
            manifest,
            duplicate_manifest,
            _with_sequence(start, 3),
            _with_sequence(activation, 4),
            _with_sequence(terminal, 5),
        )
        other_operation = _replace_field(
            _successful_records(),
            3,
            "operationId",
            "18",
        )

        self._assert_failed(
            _project(duplicate_records),
            "P1_STORE_HOME_RUN_MANIFEST_EVENT_DUPLICATE",
        )
        self._assert_failed(
            _project(other_operation),
            "P1_STORE_HOME_MULTIPLE_OPERATIONS",
        )

    def _assert_failed(self, result, reason: str) -> None:
        self.assertFalse(result.ok)
        self.assertEqual(reason, result.reason)
        self.assertEqual("", result.command_request_id)
        self.assertFalse(any(result.evidence_mapping().values()))


def _project(records: tuple[ProductionDiagnosticRecord, ...]):
    return project_p1_supervised_store_home_runtime_evidence(
        records,
        expected_candidate_position=_POSITION,
        expected_run_manifest_id=_MANIFEST_ID,
        expected_command_request_id=_REQUEST_ID,
        expected_command_session_id=_SESSION_ID,
        expected_world_key=_WORLD_KEY,
        expected_dimension=_DIMENSION,
        expected_destination_canonical_key=_DESTINATION_CANONICAL_KEY,
        expected_destination_id=_DESTINATION_ID,
    )


def _successful_records() -> tuple[ProductionDiagnosticRecord, ...]:
    return (
        _record(
            "STORE_HOME_RUN_MANIFEST",
            1,
            extra=(
                ("runId", _MANIFEST_ID),
                ("runManifestIdSource", "EXTERNAL_BUILD_DEPLOY_MANIFEST"),
            ),
        ),
        _record("STORE_HOME_OPERATION_STARTED", 2),
        _record(
            "STORE_HOME_CANDIDATE_ACTIVATED",
            3,
            extra=(
                ("candidateActivated", "true"),
                ("activationResult", "EXACT_SESSION_INSTALLED"),
                ("worldKey", _WORLD_KEY),
                ("dimension", _DIMENSION),
                ("destinationCanonicalKey", _DESTINATION_CANONICAL_KEY),
                ("destinationId", _DESTINATION_ID),
                ("candidatePosition", _POSITION),
            ),
        ),
        _record(
            "STORE_HOME_OPERATION_TERMINAL_SUMMARY",
            4,
            extra=(
                ("terminalResult", "COMPLETED"),
                ("operationResult", "COMPLETED"),
                ("remainingStackCount", "0"),
                ("storedItems", "64"),
                ("touchedStackCount", "2"),
            ),
        ),
    )


def _record(
    event_name: str,
    sequence: int,
    *,
    extra: tuple[tuple[str, str], ...] = (),
) -> ProductionDiagnosticRecord:
    fields = (
        ("traceId", "trace-p1"),
        ("eventSequence", str(sequence)),
        ("event", event_name),
        ("runManifestId", _MANIFEST_ID),
        ("operationId", "17"),
        ("commandContextAvailable", "true"),
        ("commandRequestId", _REQUEST_ID),
        ("commandCorrelationId", "message-17"),
        ("commandSessionId", _SESSION_ID),
        ("commandConnectionGeneration", "7"),
        ("commandText", "@store_home"),
        ("commandSource", "lavi_gui"),
        ("commandContextError", "none"),
        *extra,
    )
    return ProductionDiagnosticRecord(
        marker="[LAVI ChatClefBoundary]",
        envelope_kind="BOUNDED_EVENT",
        event_name=event_name,
        event_sequence=sequence,
        ordered_fields=fields,
    )


def _replace_field(
    records: tuple[ProductionDiagnosticRecord, ...],
    index: int,
    key: str,
    value: str,
) -> tuple[ProductionDiagnosticRecord, ...]:
    changed = list(records)
    record = changed[index]
    changed[index] = ProductionDiagnosticRecord(
        marker=record.marker,
        envelope_kind=record.envelope_kind,
        event_name=record.event_name,
        event_sequence=record.event_sequence,
        ordered_fields=tuple(
            (field, value if field == key else current)
            for field, current in record.ordered_fields
        ),
    )
    return tuple(changed)


def _replace_all_fields(
    records: tuple[ProductionDiagnosticRecord, ...],
    key: str,
    value: str,
) -> tuple[ProductionDiagnosticRecord, ...]:
    changed = records
    for index in range(len(changed)):
        changed = _replace_field(changed, index, key, value)
    return changed


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


if __name__ == "__main__":
    unittest.main()
