#20260901_kpopmodder: Validate persisted one-shot guard records without mutation.
from __future__ import annotations

import re
from collections.abc import Mapping
from datetime import datetime, timedelta

from .one_shot_guard_record_identity import OneShotGuardRecordIdentity


_FINGERPRINT_PATTERN = re.compile(r"[0-9a-f]{64}")
_BLOCK_KEYS = frozenset(
    {
        "invocation_fingerprint",
        "command_fingerprint",
        "claimed_at_utc",
    }
)
_RECONCILIATION_KEYS = frozenset(
    {
        "invocation_fingerprint",
        "command_fingerprint",
        "reason",
        "recorded_at_utc",
    }
)


def parse_live_run_block(record: object) -> OneShotGuardRecordIdentity:
    values = _require_shape(record, _BLOCK_KEYS, "live-run block")
    _require_utc_timestamp(values["claimed_at_utc"], "claimed_at_utc")
    return _identity(values)


def parse_reconciliation_requirement(
    record: object,
) -> OneShotGuardRecordIdentity:
    values = _require_shape(
        record,
        _RECONCILIATION_KEYS,
        "reconciliation requirement",
    )
    _require_utc_timestamp(values["recorded_at_utc"], "recorded_at_utc")
    reason = values["reason"]
    if not isinstance(reason, str) or not reason.strip() or len(reason) > 120:
        raise ValueError("reconciliation reason is invalid")
    identity = _identity(values)
    return OneShotGuardRecordIdentity(
        invocation_fingerprint=identity.invocation_fingerprint,
        command_fingerprint=identity.command_fingerprint,
        reconciliation_reason=reason,
    )


def _require_shape(
    record: object,
    expected_keys: frozenset[str],
    label: str,
) -> Mapping[str, object]:
    if not isinstance(record, Mapping):
        raise ValueError(f"{label} record is not a mapping")
    if set(record) != expected_keys:
        raise ValueError(f"{label} record shape is invalid")
    return record


def _identity(values: Mapping[str, object]) -> OneShotGuardRecordIdentity:
    invocation = _require_fingerprint(
        values["invocation_fingerprint"],
        "invocation fingerprint",
    )
    command = _require_fingerprint(
        values["command_fingerprint"],
        "command fingerprint",
    )
    return OneShotGuardRecordIdentity(
        invocation_fingerprint=invocation,
        command_fingerprint=command,
    )


def _require_fingerprint(value: object, label: str) -> str:
    if not isinstance(value, str) or _FINGERPRINT_PATTERN.fullmatch(value) is None:
        raise ValueError(f"{label} is invalid")
    return value


def _require_utc_timestamp(value: object, label: str) -> None:
    if not isinstance(value, str):
        raise ValueError(f"{label} is invalid")
    try:
        timestamp = datetime.fromisoformat(value)
    except ValueError as error:
        raise ValueError(f"{label} is invalid") from error
    if timestamp.utcoffset() != timedelta(0):
        raise ValueError(f"{label} is not UTC")
