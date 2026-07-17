#20260717_kpopmodder: Isolates stop-specific game result DTO behavior.
from dataclasses import dataclass
from typing import Any, Dict, Mapping

from .game_result_dto import GameResultDTO


@dataclass(frozen=True)
class GameStopResultDTO(GameResultDTO):
    action: str = "stop"
    running: bool = False
    stopped: bool = False

    @classmethod
    def from_mapping(cls, value: Any, action: str = "stop") -> "GameStopResultDTO":
        if isinstance(value, cls):
            return value
        base = GameResultDTO.from_mapping(value, action=action)
        payload = value if isinstance(value, Mapping) else {}
        running = bool(payload.get("running", False))
        return cls(
            ok=base.ok,
            action=base.action or "stop",
            status=dict(base.status),
            error=base.error,
            message=base.message,
            details=dict(base.details),
            running=running,
            stopped=bool(payload.get("stopped", base.ok and not running)),
        )

    def to_legacy_dict(self) -> Dict[str, Any]:
        payload = super().to_legacy_dict()
        payload["running"] = bool(self.running)
        payload["stopped"] = bool(self.stopped)
        return payload
