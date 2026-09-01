#20260901_kpopmodder: Carry one bounded production-log delta scan result without turning it into a gameplay verdict.
from __future__ import annotations

from dataclasses import dataclass

from .diagnostic_record import ProductionDiagnosticRecord


@dataclass(frozen=True, slots=True)
class ProductionDiagnosticLogScanResult:
    ok: bool
    reason: str
    records: tuple[ProductionDiagnosticRecord, ...] = ()
    line_count: int = 0
    candidate_count: int = 0
    failed_line_number: int | None = None
    failure_detail: str = ""
