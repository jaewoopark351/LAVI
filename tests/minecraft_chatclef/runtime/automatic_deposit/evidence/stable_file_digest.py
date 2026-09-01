#20260831_kpopmodder: Hash one read-only file only when its identity stays stable.
from __future__ import annotations

import hashlib
import stat
from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path

from ...preflight.shared_file_reader import read_shared_bytes


_MAX_EVIDENCE_FILE_BYTES = 512 * 1024 * 1024


@dataclass(frozen=True, slots=True)
class AutomaticDepositStableFileDigest:
    path: str
    sha256: str
    size: int
    mtime_ns: int


def read_automatic_deposit_stable_file_digest(
    path: Path,
    *,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
) -> tuple[AutomaticDepositStableFileDigest | None, str]:
    digest, _payload, reason = read_automatic_deposit_stable_file(
        path,
        bytes_reader=bytes_reader,
        stat_reader=stat_reader,
    )
    return digest, reason


def read_automatic_deposit_stable_file(
    path: Path,
    *,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
) -> tuple[AutomaticDepositStableFileDigest | None, bytes | None, str]:
    if not path.is_absolute():
        return None, None, "STABLE_FILE_PATH_NOT_ABSOLUTE"
    canonical_path = path.resolve(strict=False)
    reader = bytes_reader or read_shared_bytes
    read_stat = stat_reader or (lambda candidate: candidate.stat())
    try:
        before = read_stat(canonical_path)
        before_identity = _stat_identity(before)
        if not stat.S_ISREG(int(getattr(before, "st_mode"))):
            return None, None, "STABLE_FILE_NOT_REGULAR"
        size = int(getattr(before, "st_size"))
        if size < 0 or size > _MAX_EVIDENCE_FILE_BYTES:
            return None, None, "STABLE_FILE_SIZE_OUT_OF_BOUND"
        payload = reader(canonical_path)
        after = read_stat(canonical_path)
        after_identity = _stat_identity(after)
    except Exception as error:
        return None, None, f"STABLE_FILE_READ_FAILED:{type(error).__name__}"
    if not isinstance(payload, bytes):
        return None, None, "STABLE_FILE_READER_DID_NOT_RETURN_BYTES"
    if before_identity != after_identity:
        return None, None, "STABLE_FILE_CHANGED_DURING_READ"
    if len(payload) != size:
        return None, None, "STABLE_FILE_BYTE_COUNT_MISMATCH"
    return (
        AutomaticDepositStableFileDigest(
            path=str(canonical_path),
            sha256=hashlib.sha256(payload).hexdigest(),
            size=size,
            mtime_ns=int(getattr(before, "st_mtime_ns")),
        ),
        payload,
        "STABLE_FILE_DIGEST_COLLECTED",
    )


def _stat_identity(value: object) -> tuple[int, int, int, int, int]:
    return (
        int(getattr(value, "st_dev")),
        int(getattr(value, "st_ino")),
        int(getattr(value, "st_mode")),
        int(getattr(value, "st_size")),
        int(getattr(value, "st_mtime_ns")),
    )
