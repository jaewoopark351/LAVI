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

from .ladder_proxy_ports_status_dto import LadderProxyPortsStatusDTO

@dataclass(frozen=True)
class LadderProxyStatusDTO:
    running: bool = False
    pid: Optional[int] = None
    returncode: Optional[int] = None
    uptime_sec: float = 0.0
    last_error: str = ""
    stdout_tail: list[str] = field(default_factory=list)
    stderr_tail: list[str] = field(default_factory=list)
    validation: Dict[str, Any] = field(default_factory=dict)
    ports: Optional[LadderProxyPortsStatusDTO] = None
    launch_diagnostics: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, value: Any) -> "LadderProxyStatusDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, dict) else {}
        ports = payload.get("ports")
        return cls(
            running=bool(payload.get("running", False)),
            pid=_coerce_optional_int(payload.get("pid")),
            returncode=_coerce_optional_int(payload.get("returncode")),
            uptime_sec=_coerce_float(payload.get("uptime_sec"), 0.0),
            last_error=_coerce_str(payload.get("last_error")),
            stdout_tail=[str(item) for item in payload.get("stdout_tail", [])],
            stderr_tail=[str(item) for item in payload.get("stderr_tail", [])],
            validation=_coerce_dict(payload.get("validation")),
            ports=(
                LadderProxyPortsStatusDTO.from_mapping(ports)
                if isinstance(ports, (dict, LadderProxyPortsStatusDTO))
                else None
            ),
            launch_diagnostics=_coerce_dict(payload.get("launch_diagnostics")),
        )

    def to_dict(self) -> Dict[str, Any]:
        payload = {
            "running": bool(self.running),
            "pid": self.pid,
            "returncode": self.returncode,
            "uptime_sec": self.uptime_sec,
            "last_error": self.last_error,
            "stdout_tail": list(self.stdout_tail),
            "stderr_tail": list(self.stderr_tail),
            "launch_diagnostics": dict(self.launch_diagnostics),
        }
        if self.validation:
            payload["validation"] = dict(self.validation)
        if self.ports is not None:
            payload["ports"] = self.ports.to_dict()
        return payload
