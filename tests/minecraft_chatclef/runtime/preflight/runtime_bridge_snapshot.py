#20260819_kpopmodder: Select one unambiguous Fabric bridge status object for every observer.
from __future__ import annotations

from collections.abc import Mapping


def runtime_bridge_snapshot(payload: object) -> tuple[dict[str, object], str]:
    if not isinstance(payload, Mapping):
        return {}, "runtime status must be an object"
    direct = "backend_id" in payload
    nested_value = payload.get("details")
    nested = isinstance(nested_value, Mapping) and "backend_id" in nested_value
    if direct and nested:
        return {}, "runtime status contains ambiguous bridge objects"
    if direct:
        return dict(payload), ""
    if nested:
        return dict(nested_value), ""
    return {}, "Fabric ChatClef bridge status is missing"
