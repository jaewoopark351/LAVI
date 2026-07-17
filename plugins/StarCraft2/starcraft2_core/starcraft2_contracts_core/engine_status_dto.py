#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260628_kpopmodder: Add shared StarCraft2 contracts to replace ad-hoc dict payloads.
from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Optional

import time

from app_core.extensions.game_extension_contracts import GameResultDTO, GameStatusDTO


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, dict) else {}


def _normalize_ports(value: Any) -> list[int]:
    if value is None:
        return [5677, 5678]
    if isinstance(value, str):
        raw = [part.strip() for part in value.split(",")]
    elif isinstance(value, (list, tuple)):
        raw = [str(part).strip() for part in value]
    else:
        return [5677, 5678]
    ports: list[int] = []
    for item in raw:
        try:
            port = int(item)
        except (TypeError, ValueError):
            continue
        if 0 < port < 65536 and port not in ports:
            ports.append(port)
    return ports or [5677, 5678]


def _coerce_str(value: Any, fallback: str = "") -> str:
    return str(value or "").strip() if value is not None else fallback


def _coerce_float(value: Any, fallback: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return float(fallback)


def _coerce_int(value: Any, fallback: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return int(fallback)


def _coerce_optional_int(value: Any) -> Optional[int]:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


#20260715_kpopmodder: Add typed command, result, and status contracts for SC2 engines.

@dataclass(frozen=True)
class EngineStatusDTO:
    """Stable status returned by every SC2 engine contract."""

    engine: str = "unknown"
    running: bool = False
    status: Dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None

    @classmethod
    def from_mapping(
        cls,
        value: Any,
        engine: str = "unknown",
    ) -> "EngineStatusDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        normalized_engine = _coerce_str(payload.get("engine"), engine or "unknown")
        nested_status = payload.get("status")
        status = _coerce_dict(nested_status) if isinstance(nested_status, dict) else dict(payload)
        status.pop("engine", None)
        status.pop("error", None)
        running = bool(payload.get("running", status.get("running", False)))
        status["running"] = running
        error = payload.get("error", status.get("last_error"))
        return cls(
            engine=normalized_engine,
            running=running,
            status=status,
            error=None if error is None else str(error),
        )

    def to_dict(self) -> Dict[str, Any]:
        payload = dict(self.status)
        payload["engine"] = self.engine
        payload["running"] = bool(self.running)
        if self.error is not None:
            payload["error"] = self.error
        return payload
