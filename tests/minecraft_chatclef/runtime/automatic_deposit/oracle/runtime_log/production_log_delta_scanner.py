#20260901_kpopmodder: Scan only sealed latest.log deltas and fail closed on malformed production diagnostic candidates.
from __future__ import annotations

from ...evidence.latest_log_delta_result import (
    AutomaticDepositLatestLogDeltaResult,
)
from .diagnostic_record import ProductionDiagnosticRecord
from .diagnostic_record_parser import parse_production_diagnostic_record
from .production_log_scan_result import ProductionDiagnosticLogScanResult

MAX_DELTA_LINES = 100_000
MAX_CANDIDATE_RECORDS = 5_000

_BOUNDARY_CANDIDATE = "ALTO CLEF: [LAVI ChatClefBoundary]"
_DIAGNOSTIC_CANDIDATE = "ALTO CLEF: [LAVI ChatClefDiag]"
_BOUNDED_CAPTURE_FIELD = " diagnosticCaptureStatus="
_STORE_HOME_EVENT_FIELD_PREFIX = " event=STORE_HOME_"
_AUTO_DEPOSIT_EVENT_TYPE = " eventType=AUTO_DEPOSIT_ALL"


def scan_production_diagnostic_delta(
    delta: object,
    *,
    max_lines: int = MAX_DELTA_LINES,
    max_candidate_records: int = MAX_CANDIDATE_RECORDS,
) -> ProductionDiagnosticLogScanResult:
    if not isinstance(delta, AutomaticDepositLatestLogDeltaResult):
        return _failure("PRODUCTION_LOG_DELTA_NOT_TYPED")
    if delta.ok is not True:
        return _failure(
            "PRODUCTION_LOG_DELTA_NOT_OK",
            failure_detail=_bounded_source_reason(delta.reason),
        )
    if (
        not isinstance(max_lines, int)
        or isinstance(max_lines, bool)
        or max_lines < 1
        or max_lines > MAX_DELTA_LINES
        or not isinstance(max_candidate_records, int)
        or isinstance(max_candidate_records, bool)
        or max_candidate_records < 1
        or max_candidate_records > MAX_CANDIDATE_RECORDS
    ):
        return _failure("PRODUCTION_LOG_SCAN_BOUND_INVALID")
    if not isinstance(delta.text, str):
        return _failure("PRODUCTION_LOG_DELTA_TEXT_INVALID")
    if delta.text and not delta.text.endswith("\n"):
        return _failure("PRODUCTION_LOG_DELTA_NOT_COMPLETE")

    lines = delta.text.split("\n")[:-1] if delta.text else []
    line_count = len(lines)
    if line_count > max_lines:
        return _failure(
            "PRODUCTION_LOG_DELTA_LINE_COUNT_EXCEEDS_BOUND",
            line_count=line_count,
        )

    records: list[ProductionDiagnosticRecord] = []
    candidate_count = 0
    for line_number, raw_line in enumerate(lines, start=1):
        line = raw_line[:-1] if raw_line.endswith("\r") else raw_line
        if not _is_candidate(line):
            continue
        candidate_count += 1
        if candidate_count > max_candidate_records:
            return _failure(
                "PRODUCTION_LOG_CANDIDATE_COUNT_EXCEEDS_BOUND",
                line_count=line_count,
                candidate_count=candidate_count,
                failed_line_number=line_number,
            )
        parsed = parse_production_diagnostic_record(line)
        if not parsed.ok or parsed.record is None:
            return _failure(
                "PRODUCTION_LOG_CANDIDATE_MALFORMED",
                line_count=line_count,
                candidate_count=candidate_count,
                failed_line_number=line_number,
                failure_detail=parsed.reason,
            )
        records.append(parsed.record)

    return ProductionDiagnosticLogScanResult(
        ok=True,
        reason="PRODUCTION_LOG_DELTA_SCANNED",
        records=tuple(records),
        line_count=line_count,
        candidate_count=candidate_count,
    )


def _is_candidate(line: str) -> bool:
    boundary_target = _BOUNDARY_CANDIDATE in line and (
            _BOUNDED_CAPTURE_FIELD in line
            or _STORE_HOME_EVENT_FIELD_PREFIX in line
    )
    diagnostic_target = (
        _DIAGNOSTIC_CANDIDATE in line
        and _contains_exact_field_value(
            line,
            _AUTO_DEPOSIT_EVENT_TYPE,
        )
    )
    return boundary_target or diagnostic_target


def _contains_exact_field_value(line: str, field_and_value: str) -> bool:
    start = 0
    while True:
        offset = line.find(field_and_value, start)
        if offset < 0:
            return False
        end = offset + len(field_and_value)
        if end == len(line) or line[end] == " ":
            return True
        start = end


def _bounded_source_reason(reason: object) -> str:
    if not isinstance(reason, str) or not reason or len(reason) > 160:
        return "PRODUCTION_LOG_DELTA_SOURCE_REASON_UNAVAILABLE"
    if any(ord(character) < 0x20 or ord(character) == 0x7F for character in reason):
        return "PRODUCTION_LOG_DELTA_SOURCE_REASON_UNAVAILABLE"
    return reason


def _failure(
    reason: str,
    *,
    line_count: int = 0,
    candidate_count: int = 0,
    failed_line_number: int | None = None,
    failure_detail: str = "",
) -> ProductionDiagnosticLogScanResult:
    return ProductionDiagnosticLogScanResult(
        ok=False,
        reason=reason,
        line_count=line_count,
        candidate_count=candidate_count,
        failed_line_number=failed_line_number,
        failure_detail=failure_detail,
    )
