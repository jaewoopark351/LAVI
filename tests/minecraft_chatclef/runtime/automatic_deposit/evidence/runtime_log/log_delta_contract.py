#20260901_kpopmodder: Verify one sealed latest.log delta independently of event schema.
from __future__ import annotations

from ..latest_log_byte_cursor import (
    AutomaticDepositLatestLogByteCursor,
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ..latest_log_delta_result import AutomaticDepositLatestLogDeltaResult


def verify_automatic_deposit_runtime_log_delta(
    expected_cursor: object,
    log_delta: object,
) -> tuple[str, ...]:
    if not isinstance(expected_cursor, AutomaticDepositLatestLogByteCursor):
        return ("LATEST_LOG_CURSOR_NOT_TYPED",)
    if not isinstance(log_delta, AutomaticDepositLatestLogDeltaResult):
        return ("LATEST_LOG_DELTA_NOT_TYPED",)
    if not log_delta.ok:
        return (f"LATEST_LOG_DELTA_NOT_VERIFIED:{log_delta.reason}",)
    if log_delta.source_cursor_fingerprint != (
        automatic_deposit_latest_log_cursor_fingerprint(expected_cursor)
    ):
        return ("LATEST_LOG_DELTA_CURSOR_FINGERPRINT_MISMATCH",)
    if log_delta.start_offset != expected_cursor.size:
        return ("LATEST_LOG_DELTA_START_OFFSET_MISMATCH",)
    if log_delta.end_offset < log_delta.start_offset:
        return ("LATEST_LOG_DELTA_OFFSET_ORDER_INVALID",)
    if log_delta.encoding not in ("utf-8", "cp949"):
        return ("LATEST_LOG_DELTA_ENCODING_INVALID",)
    try:
        byte_count = len(log_delta.text.encode(log_delta.encoding, errors="strict"))
    except UnicodeEncodeError:
        return ("LATEST_LOG_DELTA_TEXT_ENCODING_MISMATCH",)
    if byte_count != log_delta.end_offset - log_delta.start_offset:
        return ("LATEST_LOG_DELTA_BYTE_COUNT_MISMATCH",)
    if log_delta.text and not log_delta.text.endswith("\n"):
        return ("LATEST_LOG_DELTA_PARTIAL_RECORD",)
    return ()
