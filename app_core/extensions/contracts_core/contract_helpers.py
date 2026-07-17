#20260717_kpopmodder: Keeps shared game extension DTO coercion helpers in one module.
from typing import Any, Dict, Mapping


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, Mapping) else {}


def _coerce_action(value: Any) -> str:
    return str(value or "").strip().lower()


_RESULT_RESERVED_KEYS = {
    "ok",
    "action",
    "status",
    "error",
    "message",
    "details",
    "running",
    "started",
    "stopped",
}

_STATUS_RESERVED_KEYS = {
    "name",
    "initialized",
    "started",
    "plugin",
    "worker",
    "runtime",
    "runtime_context",
    "details",
    "error",
    "extension",
    "game_status",
}


def _merge_details(payload: Mapping[str, Any], reserved_keys: set[str]) -> Dict[str, Any]:
    details = _coerce_dict(payload.get("details"))
    for key, value in payload.items():
        if key in reserved_keys:
            continue
        details.setdefault(str(key), value)
    return details
