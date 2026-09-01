#20260831_kpopmodder: Return a log cursor or explicit INCONCLUSIVE evidence.
from __future__ import annotations

from dataclasses import dataclass

from ..oracle.matrix_verdict import AutomaticDepositVerdict
from .latest_log_byte_cursor import AutomaticDepositLatestLogByteCursor


@dataclass(frozen=True, slots=True)
class AutomaticDepositLatestLogCursorCapture:
    ok: bool
    reason: str
    verdict: AutomaticDepositVerdict | None
    cursor: AutomaticDepositLatestLogByteCursor | None = None
