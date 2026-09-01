#20260831_kpopmodder: Return only complete appended latest.log records.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field

from ..oracle.matrix_verdict import AutomaticDepositVerdict


_LATEST_LOG_DELTA_SEAL = object()


@dataclass(frozen=True, slots=True)
class AutomaticDepositLatestLogDeltaResult:
    ok: bool
    reason: str
    verdict: AutomaticDepositVerdict | None
    start_offset: int
    end_offset: int
    text: str = ""
    encoding: str = ""
    source_cursor_fingerprint: str = ""
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _LATEST_LOG_DELTA_SEAL:
            raise ValueError("latest.log delta must be created by its reader")
        if self._integrity != _delta_integrity(
            self.ok,
            self.reason,
            self.verdict,
            self.start_offset,
            self.end_offset,
            self.text,
            self.encoding,
            self.source_cursor_fingerprint,
        ):
            raise ValueError("latest.log delta integrity mismatch")


def _create_latest_log_delta_result(
    ok: bool,
    reason: str,
    verdict: AutomaticDepositVerdict | None,
    start_offset: int,
    end_offset: int,
    text: str = "",
    encoding: str = "",
    source_cursor_fingerprint: str = "",
) -> AutomaticDepositLatestLogDeltaResult:
    integrity = _delta_integrity(
        ok,
        reason,
        verdict,
        start_offset,
        end_offset,
        text,
        encoding,
        source_cursor_fingerprint,
    )
    return AutomaticDepositLatestLogDeltaResult(
        ok=ok,
        reason=reason,
        verdict=verdict,
        start_offset=start_offset,
        end_offset=end_offset,
        text=text,
        encoding=encoding,
        source_cursor_fingerprint=source_cursor_fingerprint,
        _seal=_LATEST_LOG_DELTA_SEAL,
        _integrity=integrity,
    )


def _delta_integrity(
    ok: bool,
    reason: str,
    verdict: AutomaticDepositVerdict | None,
    start_offset: int,
    end_offset: int,
    text: str,
    encoding: str,
    source_cursor_fingerprint: str,
) -> str:
    raw = json.dumps(
        (
            ok,
            reason,
            verdict.value if verdict is not None else None,
            start_offset,
            end_offset,
            text,
            encoding,
            source_cursor_fingerprint,
        ),
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()
