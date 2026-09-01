#20260831_kpopmodder: Preserve exact latest.log byte identity at scenario start.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True, slots=True)
class AutomaticDepositLatestLogByteCursor:
    path: str
    device: int
    inode: int
    size: int
    mtime_ns: int
    prefix_sha256: str


def automatic_deposit_latest_log_cursor_fingerprint(
    cursor: AutomaticDepositLatestLogByteCursor,
) -> str:
    raw = json.dumps(
        {
            "path": str(Path(cursor.path).resolve(strict=False)).casefold(),
            "device": cursor.device,
            "inode": cursor.inode,
            "size": cursor.size,
            "mtime_ns": cursor.mtime_ns,
            "prefix_sha256": cursor.prefix_sha256.lower(),
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()
