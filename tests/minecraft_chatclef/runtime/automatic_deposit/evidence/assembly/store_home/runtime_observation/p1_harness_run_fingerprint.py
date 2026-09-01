# 20260901_kpopmodder: Fingerprint one complete harness run without treating it as a JVM ID.
from __future__ import annotations

import hashlib
import json
from dataclasses import fields, is_dataclass
from enum import Enum

from ....run_manifest import AutomaticDepositRunManifest


def p1_harness_run_manifest_fingerprint(
    run_manifest: AutomaticDepositRunManifest,
) -> str:
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        raise TypeError("P1 harness run manifest is not typed")
    payload = json.dumps(
        _normalized(run_manifest),
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def _normalized(value: object) -> object:
    if is_dataclass(value) and not isinstance(value, type):
        return {
            field.name: _normalized(getattr(value, field.name))
            for field in fields(value)
        }
    if isinstance(value, Enum):
        return value.value
    if isinstance(value, tuple):
        return [_normalized(item) for item in value]
    if isinstance(value, (str, int, bool)) or value is None:
        return value
    raise TypeError(f"unsupported P1 harness run field: {type(value).__name__}")
