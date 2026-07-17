#20260717_kpopmodder: Isolates the typed game event payload DTO from the bus.
import time
from dataclasses import asdict, dataclass, field
from typing import Any, Dict, Mapping


@dataclass(frozen=True)
class GameEventDTO:
    event_type: str
    game: str = ""
    source: str = "game_extension"
    details: Dict[str, Any] = field(default_factory=dict)
    time: float = field(default_factory=lambda: round(time.time(), 6))

    @classmethod
    def from_mapping(cls, value: Any) -> "GameEventDTO":
        if isinstance(value, cls):
            return value
        payload = value if isinstance(value, Mapping) else {}
        return cls(
            event_type=str(payload.get("event_type") or payload.get("type") or ""),
            game=str(payload.get("game") or payload.get("target") or ""),
            source=str(payload.get("source") or "game_extension"),
            details=dict(payload.get("details") or {}) if isinstance(payload.get("details"), Mapping) else {},
            time=float(payload.get("time") or time.time())
            if isinstance(payload.get("time"), (int, float))
            else time.time(),
        )

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)
