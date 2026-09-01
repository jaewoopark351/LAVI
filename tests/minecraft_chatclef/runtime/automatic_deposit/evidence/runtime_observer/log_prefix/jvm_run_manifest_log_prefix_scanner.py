# 20260901_kpopmodder: Scan only exact JVM run-manifest candidates from a sealed log prefix.
from __future__ import annotations

from ....oracle.runtime_log.diagnostic_record import ProductionDiagnosticRecord
from ....oracle.runtime_log.diagnostic_record_parser import (
    parse_production_diagnostic_record,
)
from ....oracle.runtime_log.production_log_delta_scanner import (
    MAX_CANDIDATE_RECORDS,
    MAX_DELTA_LINES,
)
from ....oracle.runtime_log.production_log_scan_result import (
    ProductionDiagnosticLogScanResult,
)
from ...latest_log_delta_result import AutomaticDepositLatestLogDeltaResult


_BOUNDARY_CANDIDATE = "ALTO CLEF: [LAVI ChatClefBoundary]"
_MANIFEST_EVENT_FIELD = " event=STORE_HOME_RUN_MANIFEST"


def scan_jvm_run_manifest_log_prefix(
    delta: object,
    *,
    max_lines: int = MAX_DELTA_LINES,
    max_candidate_records: int = MAX_CANDIDATE_RECORDS,
) -> ProductionDiagnosticLogScanResult:
    if not isinstance(delta, AutomaticDepositLatestLogDeltaResult):
        return _failure("JVM_RUN_MANIFEST_DELTA_NOT_TYPED")
    if delta.ok is not True:
        return _failure("JVM_RUN_MANIFEST_DELTA_NOT_OK")
    if not _valid_bound(max_lines, MAX_DELTA_LINES) or not _valid_bound(
        max_candidate_records,
        MAX_CANDIDATE_RECORDS,
    ):
        return _failure("JVM_RUN_MANIFEST_SCAN_BOUND_INVALID")
    if not isinstance(delta.text, str):
        return _failure("JVM_RUN_MANIFEST_DELTA_TEXT_INVALID")
    if delta.text and not delta.text.endswith("\n"):
        return _failure("JVM_RUN_MANIFEST_DELTA_NOT_COMPLETE")

    lines = delta.text.split("\n")[:-1] if delta.text else []
    line_count = len(lines)
    if line_count > max_lines:
        return _failure(
            "JVM_RUN_MANIFEST_LINE_COUNT_EXCEEDS_BOUND",
            line_count=line_count,
        )

    records: list[ProductionDiagnosticRecord] = []
    candidate_count = 0
    for line_number, raw_line in enumerate(lines, start=1):
        line = raw_line[:-1] if raw_line.endswith("\r") else raw_line
        if not _is_exact_manifest_candidate(line):
            continue
        candidate_count += 1
        if candidate_count > max_candidate_records:
            return _failure(
                "JVM_RUN_MANIFEST_CANDIDATE_COUNT_EXCEEDS_BOUND",
                line_count=line_count,
                candidate_count=candidate_count,
                failed_line_number=line_number,
            )
        parsed = parse_production_diagnostic_record(line)
        if not parsed.ok or parsed.record is None:
            return _failure(
                "JVM_RUN_MANIFEST_CANDIDATE_MALFORMED",
                line_count=line_count,
                candidate_count=candidate_count,
                failed_line_number=line_number,
                failure_detail=parsed.reason,
            )
        records.append(parsed.record)

    return ProductionDiagnosticLogScanResult(
        ok=True,
        reason="JVM_RUN_MANIFEST_LOG_PREFIX_SCANNED",
        records=tuple(records),
        line_count=line_count,
        candidate_count=candidate_count,
    )


def _is_exact_manifest_candidate(line: str) -> bool:
    return _BOUNDARY_CANDIDATE in line and _contains_exact_field_value(
        line,
        _MANIFEST_EVENT_FIELD,
    )


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


def _valid_bound(value: object, maximum: int) -> bool:
    return type(value) is int and 1 <= value <= maximum


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
