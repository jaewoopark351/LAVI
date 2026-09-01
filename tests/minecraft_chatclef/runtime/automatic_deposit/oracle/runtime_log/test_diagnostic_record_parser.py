#20260901_kpopmodder: Verify production diagnostic parsing without files, services, or Minecraft access.
from __future__ import annotations

import unittest

from .diagnostic_record_parser import (
    AUTO_DEPOSIT_ALL_PHASE_ENVELOPE,
    BOUNDED_EVENT_ENVELOPE,
    parse_production_diagnostic_record,
)


class ProductionDiagnosticRecordParserTests(unittest.TestCase):
    def test_parses_complete_bounded_event_and_preserves_order(self):
        result = parse_production_diagnostic_record(
            _bounded_line(
                "operationId=UNAVAILABLE operationId=store-7 "
                "detail=paired%20delta%3Dconfirmed"
            )
        )

        self.assertTrue(result.ok)
        self.assertIsNotNone(result.record)
        record = result.record
        assert record is not None
        self.assertEqual(BOUNDED_EVENT_ENVELOPE, record.envelope_kind)
        self.assertEqual("STORE_HOME_OPERATION_TERMINAL_SUMMARY", record.event_name)
        self.assertEqual(42, record.event_sequence)
        self.assertEqual(
            ("UNAVAILABLE", "store-7"), record.values("operationId")
        )
        self.assertEqual(
            ("store-7", "PRODUCTION_LOG_FIELD_PLACEHOLDER_THEN_CONCRETE"),
            record.resolved_value("operationId"),
        )
        self.assertEqual("paired delta=confirmed", record.values("detail")[0])
        self.assertLess(
            _field_index(record.ordered_fields, "operationId", "UNAVAILABLE"),
            _field_index(record.ordered_fields, "operationId", "store-7"),
        )

    def test_parses_auto_deposit_all_event_type_and_phase_envelope(self):
        result = parse_production_diagnostic_record(
            _auto_deposit_line("occupiedSlots=35 totalSlots=36")
        )

        self.assertTrue(result.ok)
        self.assertIsNotNone(result.record)
        record = result.record
        assert record is not None
        self.assertEqual(AUTO_DEPOSIT_ALL_PHASE_ENVELOPE, record.envelope_kind)
        self.assertEqual("STATE_TRANSITION", record.event_name)
        self.assertEqual(43, record.event_sequence)

    def test_identical_repeats_resolve_without_discarding_occurrences(self):
        result = parse_production_diagnostic_record(
            _bounded_line("operationId=store-7 operationId=store-7")
        )

        record = _record(result)
        self.assertEqual(("store-7", "store-7"), record.values("operationId"))
        self.assertEqual(
            ("store-7", "PRODUCTION_LOG_FIELD_IDENTICAL_REPEATS"),
            record.resolved_value("operationId"),
        )

    def test_conflicting_or_reversed_concrete_duplicates_stay_ambiguous(self):
        conflict = _record(
            parse_production_diagnostic_record(
                _bounded_line("operationId=store-7 operationId=store-8")
            )
        )
        reversed_order = _record(
            parse_production_diagnostic_record(
                _bounded_line("operationId=store-7 operationId=UNAVAILABLE")
            )
        )
        repeated_concrete = _record(
            parse_production_diagnostic_record(
                _bounded_line(
                    "operationId=UNAVAILABLE operationId=store-7 "
                    "operationId=store-7"
                )
            )
        )

        for record in (conflict, reversed_order, repeated_concrete):
            with self.subTest(values=record.values("operationId")):
                self.assertEqual(
                    (None, "PRODUCTION_LOG_FIELD_REPEATS_AMBIGUOUS"),
                    record.resolved_value("operationId"),
                )

    def test_none_is_a_placeholder_only_when_the_caller_marks_it_safe(self):
        record = _record(
            parse_production_diagnostic_record(
                _bounded_line("operationId=NONE operationId=store-7")
            )
        )

        self.assertEqual(
            (None, "PRODUCTION_LOG_FIELD_REPEATS_AMBIGUOUS"),
            record.resolved_value("operationId"),
        )
        self.assertEqual(
            ("store-7", "PRODUCTION_LOG_FIELD_PLACEHOLDER_THEN_CONCRETE"),
            record.resolved_value("operationId", none_is_placeholder=True),
        )

    def test_rejects_partial_missing_or_unavailable_bounded_capture(self):
        partial = _bounded_line().replace(
            "diagnosticCaptureStatus=complete",
            "diagnosticCaptureStatus=partial",
        )
        missing = _bounded_line().replace(" diagnosticCaptureStatus=complete", "")
        unavailable = _bounded_line(
            "boundedPayloadUnavailable=true"
        )

        self.assertEqual(
            "PRODUCTION_LOG_CAPTURE_NOT_COMPLETE",
            parse_production_diagnostic_record(partial).reason,
        )
        self.assertEqual(
            "PRODUCTION_LOG_REQUIRED_FIELD_MISSING",
            parse_production_diagnostic_record(missing).reason,
        )
        self.assertEqual(
            "PRODUCTION_LOG_BOUNDED_PAYLOAD_UNAVAILABLE",
            parse_production_diagnostic_record(unavailable).reason,
        )

    def test_accepts_complete_no_active_candidate_capture(self):
        line = _bounded_line().replace(
            "diagnosticCaptureStatus=complete",
            "diagnosticCaptureStatus=complete_no_active_candidate",
        )

        self.assertTrue(parse_production_diagnostic_record(line).ok)

    def test_rejects_non_boolean_bounded_payload_availability(self):
        result = parse_production_diagnostic_record(
            _bounded_line("boundedPayloadUnavailable=UNAVAILABLE")
        )

        self.assertFalse(result.ok)
        self.assertEqual(
            "PRODUCTION_LOG_BOUNDED_PAYLOAD_FLAG_INVALID",
            result.reason,
        )

    def test_rejects_non_line_non_utf8_and_oversized_inputs(self):
        not_text = parse_production_diagnostic_record(b"not text")
        multiline = parse_production_diagnostic_record(_bounded_line() + "\nnext")
        surrogate = parse_production_diagnostic_record(_bounded_line("detail=\ud800"))
        oversized = parse_production_diagnostic_record(
            _bounded_line("detail=" + ("x" * 9000))
        )

        self.assertEqual("PRODUCTION_LOG_LINE_NOT_TEXT", not_text.reason)
        self.assertEqual(
            "PRODUCTION_LOG_RECORD_NOT_ONE_COMPLETE_LINE", multiline.reason
        )
        self.assertEqual("PRODUCTION_LOG_LINE_NOT_STRICT_UTF8", surrogate.reason)
        self.assertEqual(
            "PRODUCTION_LOG_RECORD_EXCEEDS_BYTE_BOUND", oversized.reason
        )

    def test_java_record_cap_excludes_the_latest_log_wrapper(self):
        wrapped = ("W" * 8192) + _bounded_line()
        outer_oversized = ("W" * 16384) + _bounded_line()

        self.assertGreater(len(wrapped.encode("utf-8")), 8192)
        self.assertTrue(parse_production_diagnostic_record(wrapped).ok)
        self.assertEqual(
            "PRODUCTION_LOG_OUTER_LINE_EXCEEDS_BYTE_BOUND",
            parse_production_diagnostic_record(outer_oversized).reason,
        )

    def test_enforces_explicit_field_key_and_value_bounds(self):
        too_many = parse_production_diagnostic_record(_bounded_line(), max_fields=5)
        long_key = parse_production_diagnostic_record(
            _bounded_line("longKey=value"), max_key_chars=6
        )
        long_value = parse_production_diagnostic_record(
            _bounded_line("detail=abcd"), max_encoded_value_chars=3
        )
        weakened_hard_cap = parse_production_diagnostic_record(
            _bounded_line(), max_record_utf8_bytes=8193
        )

        self.assertEqual(
            "PRODUCTION_LOG_FIELD_COUNT_EXCEEDS_BOUND", too_many.reason
        )
        self.assertEqual("PRODUCTION_LOG_KEY_EXCEEDS_BOUND", long_key.reason)
        self.assertEqual("PRODUCTION_LOG_VALUE_EXCEEDS_BOUND", long_value.reason)
        self.assertEqual(
            "PRODUCTION_LOG_PARSER_BOUND_INVALID", weakened_hard_cap.reason
        )

    def test_decodes_only_strict_java_percent_encoding(self):
        encoded = _record(
            parse_production_diagnostic_record(
                _bounded_line("detail=%ED%95%9C%EA%B8%80 empty=~EMPTY~")
            )
        )
        lowercase_hex = parse_production_diagnostic_record(
            _bounded_line("detail=bad%2fvalue")
        )
        encoded_control = parse_production_diagnostic_record(
            _bounded_line("detail=%00")
        )
        raw_unicode = parse_production_diagnostic_record(
            _bounded_line("detail=한글")
        )

        self.assertEqual("한글", encoded.values("detail")[0])
        self.assertEqual("", encoded.values("empty")[0])
        for result in (lowercase_hex, encoded_control, raw_unicode):
            with self.subTest(reason=result.reason):
                self.assertEqual(
                    "PRODUCTION_LOG_VALUE_ENCODING_INVALID", result.reason
                )

    def test_rejects_ambiguous_or_marker_mismatched_envelopes(self):
        both = parse_production_diagnostic_record(
            _bounded_line("eventType=AUTO_DEPOSIT_ALL phase=TRIGGERED")
        )
        wrong_boundary_marker = parse_production_diagnostic_record(
            _bounded_line().replace(
                "[LAVI ChatClefBoundary]", "[LAVI ChatClefDiag]"
            )
        )
        wrong_auto_marker = parse_production_diagnostic_record(
            _auto_deposit_line().replace(
                "[LAVI ChatClefDiag]", "[LAVI ChatClefBoundary]"
            )
        )

        self.assertEqual("PRODUCTION_LOG_ENVELOPE_AMBIGUOUS", both.reason)
        self.assertEqual(
            "PRODUCTION_LOG_BOUNDARY_MARKER_INVALID", wrong_boundary_marker.reason
        )
        self.assertEqual(
            "PRODUCTION_LOG_AUTO_DEPOSIT_MARKER_INVALID", wrong_auto_marker.reason
        )

    def test_rejects_two_java_records_merged_into_one_physical_line(self):
        merged = (
            "[01:04:04] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
            "[LAVI ChatClefLifecycle] event=TASK_STARTED "
            + _bounded_line()
        )

        result = parse_production_diagnostic_record(merged)

        self.assertFalse(result.ok)
        self.assertEqual("PRODUCTION_LOG_JAVA_RECORD_PREFIX_AMBIGUOUS", result.reason)

    def test_rejects_conflicting_envelope_fields_and_invalid_sequence(self):
        conflicting_event = parse_production_diagnostic_record(
            _bounded_line().replace(
                "event=STORE_HOME_OPERATION_TERMINAL_SUMMARY",
                "event=STORE_HOME_OPERATION_TERMINAL_SUMMARY event=OTHER",
            )
        )
        invalid_sequence = parse_production_diagnostic_record(
            _auto_deposit_line().replace("eventSequence=43", "eventSequence=00")
        )
        zero_sequence = parse_production_diagnostic_record(
            _auto_deposit_line().replace("eventSequence=43", "eventSequence=0")
        )

        self.assertEqual(
            "PRODUCTION_LOG_REQUIRED_FIELD_AMBIGUOUS", conflicting_event.reason
        )
        self.assertEqual(
            "PRODUCTION_LOG_EVENT_SEQUENCE_INVALID", invalid_sequence.reason
        )
        self.assertEqual(
            "PRODUCTION_LOG_EVENT_SEQUENCE_INVALID", zero_sequence.reason
        )


def _bounded_line(extra: str = "") -> str:
    suffix = f" {extra}" if extra else ""
    return (
        "[01:04:05] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefBoundary] traceId=trace-7 clientTickId=812 "
        "eventSequence=42 taskInstanceId=task-1 taskRunId=run-1 "
        "parentTaskRunId=unavailable threadName=Render%20thread level=BOUNDARY "
        "event=STORE_HOME_OPERATION_TERMINAL_SUMMARY "
        "reason=paired_delta_confirmed taskClass=lavi.StoreHomeTask "
        f"diagnosticCaptureStatus=complete{suffix}"
    )


def _auto_deposit_line(extra: str = "") -> str:
    suffix = f" {extra}" if extra else ""
    return (
        "[01:05:06] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefDiag] traceId=trace-8 clientTickId=813 "
        "eventSequence=43 taskInstanceId=task-2 taskRunId=run-2 "
        "parentTaskRunId=unavailable threadName=Render%20thread "
        "eventType=AUTO_DEPOSIT_ALL phase=STATE_TRANSITION "
        "reason=high_water_threshold_crossed taskClass=lavi.AutoDepositTask"
        f"{suffix}"
    )


def _record(result):
    if not result.ok or result.record is None:
        raise AssertionError(f"expected parsed record, got {result.reason}")
    return result.record


def _field_index(
    fields: tuple[tuple[str, str], ...],
    key: str,
    value: str,
) -> int:
    return fields.index((key, value))


if __name__ == "__main__":
    unittest.main()
