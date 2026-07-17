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

from .start_result_dto import StartResultDTO

@dataclass(frozen=True)
class LocalMatchRuntimeStatusDTO:
    mode: str = "local_human_vs_changeling"
    result: Optional[StartResultDTO] = None
    ladder_proxy: Dict[str, Any] = field(default_factory=dict)
    game_result: Optional[GameResultDTO] = None
    game_status: Optional[GameStatusDTO] = None

    def to_dict(self) -> Dict[str, Any]:
        payload = {
            "mode": str(self.mode),
            "result": (
                self.result.to_dict() if isinstance(self.result, StartResultDTO) else _coerce_dict(self.result)
            ),
            "ladder_proxy": _coerce_dict(self.ladder_proxy),
        }
        if isinstance(self.game_result, GameResultDTO):
            payload["game_result"] = self.game_result.to_dict()
        if isinstance(self.game_status, GameStatusDTO):
            payload["game_status"] = self.game_status.to_dict()
        return payload
