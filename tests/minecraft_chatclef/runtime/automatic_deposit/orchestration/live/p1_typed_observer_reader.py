#20260901_kpopmodder: Accept one observer value only when its sealed DTO type matches.
from __future__ import annotations

from collections.abc import Callable

from .p1_read_only_observer_call import call_p1_read_only_observer
from .p1_read_only_observer_call_result import P1ReadOnlyObserverCallResult


def read_typed_p1_observation(
    observer_name: str,
    reader: Callable[..., object],
    expected_type: type,
    *args: object,
) -> P1ReadOnlyObserverCallResult:
    result = call_p1_read_only_observer(observer_name, reader, *args)
    if not result.ok:
        return result
    if not isinstance(result.value, expected_type):
        return P1ReadOnlyObserverCallResult(
            ok=False,
            observer_name=observer_name,
            reason="P1_LIVE_OBSERVER_VALUE_NOT_TYPED",
            source_reason=result.source_reason,
        )
    return result
