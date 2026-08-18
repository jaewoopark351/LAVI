#20260818_kpopmodder: Read a bounded stable latest.log snapshot with Windows sharing enabled.
from __future__ import annotations

import os
import time
from pathlib import Path

from .minecraft_log_decoder import decode_log_bytes
from .shared_file_reader import read_shared_bytes


MAX_LOG_AGE_SEC = 15 * 60
MAX_LOG_SIZE_BYTES = 128 * 1024 * 1024
STABLE_READ_ATTEMPTS = 2


def read_stable_log_snapshot(
    path: Path,
    *,
    now_sec: float | None = None,
    max_age_sec: int = MAX_LOG_AGE_SEC,
    max_size_bytes: int = MAX_LOG_SIZE_BYTES,
    attempts: int = STABLE_READ_ATTEMPTS,
) -> dict[str, object]:
    current_time = time.time() if now_sec is None else now_sec
    last_reason = "stable log snapshot was not obtained"
    for _attempt in range(max(1, attempts)):
        try:
            before = path.stat()
        except OSError as error:
            return _failure(f"latest.log stat failed: {type(error).__name__}: {error}")
        if not path.is_file():
            return _failure("latest.log is not a file")
        if before.st_size > max_size_bytes:
            return _failure("latest.log exceeds the bounded size policy")
        age_sec = current_time - before.st_mtime
        if age_sec < -5 or age_sec > max_age_sec:
            return _failure("latest.log is stale or has an invalid future timestamp")
        try:
            raw = read_shared_bytes(path)
            after = path.stat()
        except OSError as error:
            last_reason = f"latest.log snapshot read failed: {type(error).__name__}: {error}"
            continue
        if _stat_identity(before) != _stat_identity(after):
            last_reason = "latest.log changed during snapshot read"
            continue
        if len(raw) != after.st_size:
            last_reason = "latest.log byte count changed during snapshot read"
            continue
        decoded = decode_log_bytes(raw)
        if not decoded["ok"]:
            return decoded
        return {
            "ok": True,
            "reason": "stable_log_snapshot",
            "text": decoded["text"],
            "encoding": decoded["encoding"],
            "size": after.st_size,
            "age_sec": max(0.0, age_sec),
            "final_line_complete": raw.endswith((b"\n", b"\r")),
        }
    return _failure(last_reason)


def _stat_identity(stat_result: os.stat_result) -> tuple[int, int, int, int]:
    return (
        int(stat_result.st_dev),
        int(stat_result.st_ino),
        int(stat_result.st_size),
        int(stat_result.st_mtime_ns),
    )


def _failure(reason: str) -> dict[str, object]:
    return {
        "ok": False,
        "reason": reason,
        "text": "",
        "encoding": "",
        "size": None,
        "age_sec": None,
        "final_line_complete": False,
    }
