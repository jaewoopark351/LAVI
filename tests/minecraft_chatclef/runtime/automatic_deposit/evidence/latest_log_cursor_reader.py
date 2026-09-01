#20260831_kpopmodder: Detect latest.log identity and append state by bytes.
from __future__ import annotations

import hashlib
from collections.abc import Callable
from pathlib import Path

from ...preflight.minecraft_log_decoder import decode_log_bytes
from ...preflight.shared_file_reader import read_shared_bytes
from ..oracle.matrix_verdict import AutomaticDepositVerdict
from .latest_log_byte_cursor import (
    AutomaticDepositLatestLogByteCursor,
    automatic_deposit_latest_log_cursor_fingerprint,
)
from .latest_log_cursor_capture import AutomaticDepositLatestLogCursorCapture
from .latest_log_delta_result import (
    AutomaticDepositLatestLogDeltaResult,
    _create_latest_log_delta_result,
)


def capture_automatic_deposit_latest_log_cursor(
    path: object,
    *,
    stat_reader: Callable[[Path], object] | None = None,
    bytes_reader: Callable[[Path], bytes] | None = None,
    max_file_bytes: int = 64 * 1024 * 1024,
) -> AutomaticDepositLatestLogCursorCapture:
    latest_log = Path(str(path or "").strip())
    if not latest_log.is_absolute() or latest_log.name.casefold() != "latest.log":
        return _capture_failure("LATEST_LOG_PATH_INVALID")
    if max_file_bytes < 1:
        return _capture_failure("LATEST_LOG_BYTE_BOUND_INVALID")
    read_stat = stat_reader or _read_stat
    read_bytes = bytes_reader or read_shared_bytes
    try:
        before = read_stat(latest_log)
        before_values = _stat_values(before)
        if before_values is None:
            return _capture_failure("LATEST_LOG_STAT_INVALID")
        if before_values[2] > max_file_bytes:
            return _capture_failure("LATEST_LOG_EXCEEDS_BYTE_BOUND")
        raw = read_bytes(latest_log)
        after = read_stat(latest_log)
        after_values = _stat_values(after)
    except Exception as error:
        return _capture_failure(
            f"LATEST_LOG_CAPTURE_FAILED:{type(error).__name__}"
        )
    if not isinstance(raw, bytes):
        return _capture_failure("LATEST_LOG_READER_DID_NOT_RETURN_BYTES")
    if after_values is None or before_values != after_values:
        return _capture_failure("LATEST_LOG_CHANGED_DURING_CURSOR_CAPTURE")
    if len(raw) != before_values[2]:
        return _capture_failure("LATEST_LOG_SIZE_DID_NOT_MATCH_READ")
    if raw and not raw.endswith(b"\n"):
        return _capture_failure("LATEST_LOG_CURSOR_AT_PARTIAL_RECORD")
    cursor = AutomaticDepositLatestLogByteCursor(
        path=str(latest_log.resolve(strict=False)),
        device=before_values[0],
        inode=before_values[1],
        size=before_values[2],
        mtime_ns=before_values[3],
        prefix_sha256=hashlib.sha256(raw).hexdigest(),
    )
    return AutomaticDepositLatestLogCursorCapture(
        True,
        "LATEST_LOG_CURSOR_CAPTURED",
        None,
        cursor,
    )


def read_automatic_deposit_latest_log_delta(
    cursor: AutomaticDepositLatestLogByteCursor,
    *,
    stat_reader: Callable[[Path], object] | None = None,
    bytes_reader: Callable[[Path], bytes] | None = None,
    max_file_bytes: int = 64 * 1024 * 1024,
) -> AutomaticDepositLatestLogDeltaResult:
    latest_log = Path(cursor.path)
    if not latest_log.is_absolute() or latest_log.name.casefold() != "latest.log":
        return _delta_failure(cursor, "LATEST_LOG_CURSOR_PATH_INVALID")
    if cursor.size < 0 or max_file_bytes < 1:
        return _delta_failure(cursor, "LATEST_LOG_CURSOR_BOUND_INVALID")
    read_stat = stat_reader or _read_stat
    read_bytes = bytes_reader or read_shared_bytes
    try:
        before = read_stat(latest_log)
        before_values = _stat_values(before)
        if before_values is None:
            return _delta_failure(cursor, "LATEST_LOG_STAT_INVALID")
        if before_values[2] > max_file_bytes:
            return _delta_failure(cursor, "LATEST_LOG_EXCEEDS_BYTE_BOUND")
        raw = read_bytes(latest_log)
        after = read_stat(latest_log)
        after_values = _stat_values(after)
    except Exception as error:
        return _delta_failure(
            cursor,
            f"LATEST_LOG_DELTA_READ_FAILED:{type(error).__name__}",
        )
    if not isinstance(raw, bytes):
        return _delta_failure(
            cursor,
            "LATEST_LOG_READER_DID_NOT_RETURN_BYTES",
        )
    if after_values is None or before_values != after_values:
        return _delta_failure(cursor, "LATEST_LOG_CHANGED_DURING_DELTA_READ")
    device, inode, size, mtime_ns = before_values
    if _file_identity_changed(cursor, device, inode):
        return _delta_failure(cursor, "LATEST_LOG_ROTATED_OR_REPLACED")
    if size < cursor.size:
        return _delta_failure(cursor, "LATEST_LOG_TRUNCATED")
    if mtime_ns < cursor.mtime_ns:
        return _delta_failure(cursor, "LATEST_LOG_REPLACED")
    if len(raw) != size:
        return _delta_failure(cursor, "LATEST_LOG_SIZE_DID_NOT_MATCH_READ")
    prefix = raw[: cursor.size]
    if hashlib.sha256(prefix).hexdigest() != cursor.prefix_sha256:
        return _delta_failure(cursor, "LATEST_LOG_REPLACED")
    delta = raw[cursor.size :]
    if delta and not delta.endswith(b"\n"):
        return _delta_failure(cursor, "LATEST_LOG_DELTA_HAS_PARTIAL_RECORD")
    decoded = decode_log_bytes(delta)
    if decoded.get("ok") is not True:
        return _delta_failure(cursor, "LATEST_LOG_DELTA_DECODE_FAILED")
    return _create_latest_log_delta_result(
        True,
        "LATEST_LOG_DELTA_READ",
        None,
        cursor.size,
        size,
        str(decoded.get("text") or ""),
        str(decoded.get("encoding") or ""),
        automatic_deposit_latest_log_cursor_fingerprint(cursor),
    )


def _capture_failure(reason: str) -> AutomaticDepositLatestLogCursorCapture:
    return AutomaticDepositLatestLogCursorCapture(
        False,
        reason,
        AutomaticDepositVerdict.INCONCLUSIVE,
    )


def _delta_failure(
    cursor: AutomaticDepositLatestLogByteCursor,
    reason: str,
) -> AutomaticDepositLatestLogDeltaResult:
    return _create_latest_log_delta_result(
        False,
        reason,
        AutomaticDepositVerdict.INCONCLUSIVE,
        cursor.size,
        cursor.size,
        source_cursor_fingerprint=(
            automatic_deposit_latest_log_cursor_fingerprint(cursor)
        ),
    )


def _read_stat(path: Path) -> object:
    return path.stat()


def _stat_values(stat_result: object) -> tuple[int, int, int, int] | None:
    try:
        values = (
            int(getattr(stat_result, "st_dev")),
            int(getattr(stat_result, "st_ino")),
            int(getattr(stat_result, "st_size")),
            int(getattr(stat_result, "st_mtime_ns")),
        )
    except (AttributeError, TypeError, ValueError):
        return None
    if values[2] < 0 or values[3] < 0:
        return None
    return values


def _file_identity_changed(
    cursor: AutomaticDepositLatestLogByteCursor,
    current_device: int,
    current_inode: int,
) -> bool:
    if cursor.device and current_device and cursor.device != current_device:
        return True
    if cursor.inode and current_inode and cursor.inode != current_inode:
        return True
    return False
