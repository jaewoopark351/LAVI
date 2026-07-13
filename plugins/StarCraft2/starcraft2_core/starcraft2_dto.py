#20260713_kpopmodder: Introduce shared DTOs for local-match orchestration contracts.
from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Optional


def _coerce_dict(value: Any) -> Dict[str, Any]:
    return dict(value) if isinstance(value, dict) else {}


@dataclass(frozen=True)
class StarCraft2LocalMatchCommand:
    executable_path: str
    working_directory: str
    args: str | list[str]
    proxy_ports: str
    bot_name: str
    ai_race: str = "Zerg"
    human_race: str = "Terran"
    keep_local_match_identity_args: bool = False
    capture_output: bool = True

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass(frozen=True)
class StarCraft2CommandResult:
    ok: bool
    running: bool = False
    action: str = "local_match"
    status: Dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None
    message: Optional[str] = None
    details: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_mapping(cls, value: Any, action: str = "local_match") -> "StarCraft2CommandResult":
        payload = value if isinstance(value, dict) else {}
        return cls(
            ok=bool(payload.get("ok", False)),
            running=bool(payload.get("running", False)),
            action=action if action else str(payload.get("action") or "local_match"),
            status=_coerce_dict(payload.get("status")),
            error=None if payload.get("error") is None else str(payload.get("error")),
            message=None if payload.get("message") is None else str(payload.get("message")),
            details=_coerce_dict(payload.get("details")),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "ok": bool(self.ok),
            "running": bool(self.running),
            "action": str(self.action),
            "status": self.status,
            "error": self.error,
            "message": self.message,
            "details": self.details,
        }


@dataclass(frozen=True)
class StarCraft2LocalMatchStatus:
    mode: str = "local_human_vs_changeling"
    result: Optional[StarCraft2CommandResult] = None
    ladder_proxy: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "mode": str(self.mode),
            "result": self.result.to_dict() if isinstance(self.result, StarCraft2CommandResult) else self._normalize_result(self.result),
            "ladder_proxy": _coerce_dict(self.ladder_proxy),
        }

    @staticmethod
    def _normalize_result(value: Any) -> Dict[str, Any]:
        if isinstance(value, StarCraft2CommandResult):
            return value.to_dict()
        if isinstance(value, dict):
            return dict(value)
        if value is None:
            return {}
        return {"ok": bool(value)}
