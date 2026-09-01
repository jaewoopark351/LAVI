#20260901_kpopmodder: Invoke one read-only P1 observer through an exact fail-closed shape.
from __future__ import annotations

from collections.abc import Callable

from .p1_read_only_observer_call_result import P1ReadOnlyObserverCallResult


_MAX_REASON_LENGTH = 160


def call_p1_read_only_observer(
    observer_name: str,
    reader: Callable[..., object],
    *args: object,
) -> P1ReadOnlyObserverCallResult:
    if not callable(reader):
        return _failure(observer_name, "P1_LIVE_OBSERVER_NOT_CALLABLE")
    try:
        raw_result = reader(*args)
    except Exception as error:
        return _failure(
            observer_name,
            "P1_LIVE_OBSERVER_CALL_FAILED",
            error_type=type(error).__name__,
        )
    if type(raw_result) is not tuple or len(raw_result) != 2:
        return _failure(observer_name, "P1_LIVE_OBSERVER_RESULT_SHAPE_INVALID")
    value, source_reason = raw_result
    if not _valid_reason(source_reason):
        return _failure(observer_name, "P1_LIVE_OBSERVER_SOURCE_REASON_INVALID")
    if value is None:
        return _failure(
            observer_name,
            "P1_LIVE_OBSERVER_REPORTED_INCONCLUSIVE",
            source_reason=source_reason,
        )
    return P1ReadOnlyObserverCallResult(
        ok=True,
        observer_name=observer_name,
        reason="P1_LIVE_OBSERVER_VALUE_OBSERVED",
        source_reason=source_reason,
        value=value,
    )


def _failure(
    observer_name: str,
    reason: str,
    *,
    source_reason: str = "",
    error_type: str = "",
) -> P1ReadOnlyObserverCallResult:
    return P1ReadOnlyObserverCallResult(
        ok=False,
        observer_name=str(observer_name or ""),
        reason=reason,
        source_reason=source_reason,
        error_type=error_type,
    )


def _valid_reason(value: object) -> bool:
    return (
        isinstance(value, str)
        and value == value.strip()
        and 0 < len(value) <= _MAX_REASON_LENGTH
        and not any(
            ord(character) < 0x20 or ord(character) == 0x7F
            for character in value
        )
    )
