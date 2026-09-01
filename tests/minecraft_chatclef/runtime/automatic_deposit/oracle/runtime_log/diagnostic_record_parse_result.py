#20260901_kpopmodder: Return production-log parse success or one explicit fail-closed reason.
from __future__ import annotations

from dataclasses import dataclass

from .diagnostic_record import ProductionDiagnosticRecord


@dataclass(frozen=True, slots=True)
class ProductionDiagnosticRecordParseResult:
    ok: bool
    reason: str
    record: ProductionDiagnosticRecord | None = None
