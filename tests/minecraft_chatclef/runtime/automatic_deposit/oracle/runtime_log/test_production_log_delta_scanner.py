#20260901_kpopmodder: Verify production latest.log delta scanning with sealed in-memory evidence only.
from __future__ import annotations

import unittest

from ...evidence.latest_log_delta_result import (
    _create_latest_log_delta_result,
)
from ..matrix_verdict import AutomaticDepositVerdict
from .production_log_delta_scanner import (
    MAX_CANDIDATE_RECORDS,
    MAX_DELTA_LINES,
    scan_production_diagnostic_delta,
)


class ProductionDiagnosticLogDeltaScannerTests(unittest.TestCase):
    def test_ignores_unrelated_lines_and_preserves_candidate_order(self):
        delta = _delta(
            "ordinary Minecraft line\r\n"
            + _bounded_line(42)
            + "\r\nnear [LAVI ChatClefBoundary] but no Java prefix\r\n"
            + _auto_line(43)
            + "\r\n"
        )

        result = scan_production_diagnostic_delta(delta)

        self.assertTrue(result.ok)
        self.assertEqual("PRODUCTION_LOG_DELTA_SCANNED", result.reason)
        self.assertEqual(4, result.line_count)
        self.assertEqual(2, result.candidate_count)
        self.assertEqual(
            ("STORE_HOME_OPERATION_TERMINAL_SUMMARY", "STATE_TRANSITION"),
            tuple(record.event_name for record in result.records),
        )
        self.assertEqual((42, 43), tuple(record.event_sequence for record in result.records))

    def test_ignores_non_target_java_diagnostics_in_a_realistic_mixed_delta(self):
        delta = _delta(
            _generic_boundary_line(40)
            + "\n"
            + _generic_diagnostic_line(41)
            + "\n"
            + _bounded_line(42)
            + "\n"
            + _auto_line(43)
            + "\n"
        )

        result = scan_production_diagnostic_delta(delta)

        self.assertTrue(result.ok, (result.reason, result.failure_detail))
        self.assertEqual(4, result.line_count)
        self.assertEqual(2, result.candidate_count)
        self.assertEqual(
            ("STORE_HOME_OPERATION_TERMINAL_SUMMARY", "STATE_TRANSITION"),
            tuple(record.event_name for record in result.records),
        )

    def test_incomplete_store_home_target_is_not_skipped_as_generic_boundary(self):
        malformed = _bounded_line(42).replace(
            " diagnosticCaptureStatus=complete",
            "",
        )

        result = scan_production_diagnostic_delta(_delta(malformed + "\n"))

        self.assertFalse(result.ok)
        self.assertEqual("PRODUCTION_LOG_CANDIDATE_MALFORMED", result.reason)
        self.assertEqual("PRODUCTION_LOG_REQUIRED_FIELD_MISSING", result.failure_detail)

    def test_merged_generic_boundary_and_target_diagnostic_is_not_skipped(self):
        merged = _generic_boundary_line(40) + " " + _auto_line(43)

        result = scan_production_diagnostic_delta(_delta(merged + "\n"))

        self.assertFalse(result.ok)
        self.assertEqual("PRODUCTION_LOG_CANDIDATE_MALFORMED", result.reason)
        self.assertEqual(
            "PRODUCTION_LOG_JAVA_RECORD_PREFIX_AMBIGUOUS",
            result.failure_detail,
        )

    def test_rejects_non_typed_and_failed_deltas_without_parsing_text(self):
        non_typed = scan_production_diagnostic_delta({"ok": True, "text": ""})
        failed_delta = _delta(
            _bounded_line(42) + "\n",
            ok=False,
            reason="LATEST_LOG_ROTATED_OR_REPLACED",
        )
        failed = scan_production_diagnostic_delta(failed_delta)

        self.assertEqual("PRODUCTION_LOG_DELTA_NOT_TYPED", non_typed.reason)
        self.assertEqual("PRODUCTION_LOG_DELTA_NOT_OK", failed.reason)
        self.assertEqual("LATEST_LOG_ROTATED_OR_REPLACED", failed.failure_detail)
        self.assertEqual((), failed.records)

    def test_requires_a_complete_newline_terminated_delta(self):
        result = scan_production_diagnostic_delta(_delta(_bounded_line(42)))

        self.assertFalse(result.ok)
        self.assertEqual("PRODUCTION_LOG_DELTA_NOT_COMPLETE", result.reason)
        self.assertEqual((), result.records)

    def test_one_malformed_candidate_fails_the_whole_scan_with_line_evidence(self):
        malformed = _auto_line(43).replace("eventSequence=43", "eventSequence=00")
        delta = _delta(_bounded_line(42) + "\n" + malformed + "\n")

        result = scan_production_diagnostic_delta(delta)

        self.assertFalse(result.ok)
        self.assertEqual("PRODUCTION_LOG_CANDIDATE_MALFORMED", result.reason)
        self.assertEqual(2, result.failed_line_number)
        self.assertEqual(2, result.candidate_count)
        self.assertEqual(
            "PRODUCTION_LOG_EVENT_SEQUENCE_INVALID", result.failure_detail
        )
        self.assertEqual((), result.records)

    def test_candidate_controls_are_rejected_by_the_record_parser(self):
        controlled = _bounded_line(42).replace(" reason=", "\t reason=")

        result = scan_production_diagnostic_delta(_delta(controlled + "\n"))

        self.assertEqual("PRODUCTION_LOG_CANDIDATE_MALFORMED", result.reason)
        self.assertEqual(
            "PRODUCTION_LOG_RECORD_HAS_CONTROL_CHARACTER", result.failure_detail
        )

    def test_enforces_line_and_candidate_hard_bounds(self):
        three_lines = _delta("ignored\nignored\nignored\n")
        two_candidates = _delta(_bounded_line(42) + "\n" + _auto_line(43) + "\n")

        lines = scan_production_diagnostic_delta(three_lines, max_lines=2)
        candidates = scan_production_diagnostic_delta(
            two_candidates, max_candidate_records=1
        )
        weakened_lines = scan_production_diagnostic_delta(
            _delta(""), max_lines=MAX_DELTA_LINES + 1
        )
        weakened_candidates = scan_production_diagnostic_delta(
            _delta(""), max_candidate_records=MAX_CANDIDATE_RECORDS + 1
        )

        self.assertEqual(
            "PRODUCTION_LOG_DELTA_LINE_COUNT_EXCEEDS_BOUND", lines.reason
        )
        self.assertEqual(3, lines.line_count)
        self.assertEqual(
            "PRODUCTION_LOG_CANDIDATE_COUNT_EXCEEDS_BOUND", candidates.reason
        )
        self.assertEqual(2, candidates.failed_line_number)
        self.assertEqual("PRODUCTION_LOG_SCAN_BOUND_INVALID", weakened_lines.reason)
        self.assertEqual(
            "PRODUCTION_LOG_SCAN_BOUND_INVALID", weakened_candidates.reason
        )


def _delta(
    text: str,
    *,
    ok: bool = True,
    reason: str = "LATEST_LOG_DELTA_READ",
):
    return _create_latest_log_delta_result(
        ok,
        reason,
        None if ok else AutomaticDepositVerdict.INCONCLUSIVE,
        100,
        100 + len(text.encode("utf-8", errors="strict")),
        text,
        "utf-8",
        "a" * 64,
    )


def _bounded_line(sequence: int) -> str:
    return (
        "[01:04:05] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefBoundary] traceId=trace-7 clientTickId=812 "
        f"eventSequence={sequence} taskInstanceId=task-1 taskRunId=run-1 "
        "parentTaskRunId=unavailable threadName=Render%20thread level=BOUNDARY "
        "event=STORE_HOME_OPERATION_TERMINAL_SUMMARY "
        "reason=paired_delta_confirmed taskClass=lavi.StoreHomeTask "
        "diagnosticCaptureStatus=complete"
    )


def _auto_line(sequence: int) -> str:
    return (
        "[01:05:06] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefDiag] traceId=trace-8 clientTickId=813 "
        f"eventSequence={sequence} taskInstanceId=task-2 taskRunId=run-2 "
        "parentTaskRunId=unavailable threadName=Render%20thread "
        "eventType=AUTO_DEPOSIT_ALL phase=STATE_TRANSITION "
        "reason=high_water_threshold_crossed taskClass=lavi.AutoDepositTask"
    )


def _generic_boundary_line(sequence: int) -> str:
    return (
        "[01:03:04] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefBoundary] traceId=trace-6 clientTickId=811 "
        f"eventSequence={sequence} taskInstanceId=task-1 taskRunId=run-1 "
        "parentTaskRunId=unavailable threadName=Render%20thread level=BOUNDARY "
        "event=TOOL_SELECTION_DECISION reason=destroy_block_tool_selection "
        "taskClass=lavi.ToolTask selectedSlot=2"
    )


def _generic_diagnostic_line(sequence: int) -> str:
    return (
        "[01:03:05] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefDiag] traceId=trace-6 clientTickId=811 "
        f"eventSequence={sequence} taskInstanceId=task-1 taskRunId=run-1 "
        "parentTaskRunId=unavailable threadName=Render%20thread "
        "eventType=TASK phase=START reason=first_tick "
        "taskClass=lavi.UnrelatedTask"
    )


if __name__ == "__main__":
    unittest.main()
