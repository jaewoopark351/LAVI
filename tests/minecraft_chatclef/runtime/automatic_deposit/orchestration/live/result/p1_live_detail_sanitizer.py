#20260901_kpopmodder: Bound and redact untrusted P1 result detail text.
from __future__ import annotations

import re
from collections.abc import Iterable


_SAFE_DETAIL = re.compile(r"[A-Z0-9][A-Z0-9_.:-]{0,255}\Z", re.ASCII)


def bounded_p1_details(values: Iterable[object]) -> list[str]:
    bounded: list[str] = []
    for value in tuple(values)[:32]:
        bounded.append(
            value
            if is_safe_p1_detail(value)
            else "P1_LIVE_DETAIL_REDACTED"
        )
    return bounded


def is_safe_p1_detail(value: object) -> bool:
    return isinstance(value, str) and _SAFE_DETAIL.fullmatch(value) is not None


def is_safe_p1_error_type(value: object) -> bool:
    return (
        isinstance(value, str)
        and len(value) <= 128
        and value.isidentifier()
    )
