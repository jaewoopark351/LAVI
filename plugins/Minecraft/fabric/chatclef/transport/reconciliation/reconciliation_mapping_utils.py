#20260820_kpopmodder: Share read-only mapping helpers for active-command reconciliation diagnostics.
from __future__ import annotations

from typing import Any, Mapping


def mapping(value: Any) -> dict[str, Any]:
    return dict(value) if isinstance(value, Mapping) else {}


def text(value: Any) -> str | None:
    if value is None:
        return None
    if type(value) is bool:
        return None
    normalized = str(value).strip()
    return normalized or None


def lower_text(value: Any) -> str | None:
    normalized = text(value)
    return None if normalized is None else normalized.lower()


def short_text(value: Any, limit: int = 240) -> str | None:
    normalized = text(value)
    if normalized is None:
        return None
    if len(normalized) <= limit:
        return normalized
    return normalized[: limit - 3] + "..."


def first_text(payload: Mapping[str, Any], keys: tuple[str, ...]) -> str | None:
    for key in keys:
        normalized = text(payload.get(key))
        if normalized is not None:
            return normalized
    return None


def first_value(payload: Mapping[str, Any], keys: tuple[str, ...]) -> Any:
    for key in keys:
        value = payload.get(key)
        if value is not None and type(value) is not bool:
            return value
    return None


def bool_or_unknown(value: Any) -> bool | str:
    return value if type(value) is bool else "unknown"
