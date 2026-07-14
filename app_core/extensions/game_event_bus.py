#20260715_kpopmodder: Added a shared event bus for game extension observer-style events.
from __future__ import annotations

import time
from dataclasses import asdict, dataclass, field
from typing import Any, Callable, Dict, List, Mapping, Optional

from core.logger import log_print


GameEventCallback = Callable[[Dict[str, Any]], None]


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


class GameEventBusSubscription:
    def __init__(self, bus: "GameEventBus", callback: GameEventCallback):
        self._bus = bus
        self._callback = callback
        self._unsubscribed = False

    def unsubscribe(self) -> None:
        if self._unsubscribed:
            return
        self._unsubscribed = True
        self._bus._unsubscribe(self._callback)


class GameEventBus:
    #20260715_kpopmodder: Keep game events on a typed observer path without replacing legacy buses.
    def __init__(self):
        self._subscribers: List[GameEventCallback] = []

    def subscribe(self, callback: Optional[GameEventCallback]) -> GameEventBusSubscription:
        if not callable(callback):
            return GameEventBusSubscription(self, lambda event: None)
        self._subscribers.append(callback)
        return GameEventBusSubscription(self, callback)

    def emit(self, event: Dict[str, Any] | GameEventDTO | None) -> bool:
        normalized = GameEventDTO.from_mapping(event)
        if not normalized.event_type:
            return False
        payload = normalized.to_dict()
        delivered = False
        for subscriber in list(self._subscribers):
            if not callable(subscriber):
                continue
            try:
                subscriber(payload)
                delivered = True
            except Exception as e:
                log_print(f"[GameEventBus] subscriber failed: {e}")
        return delivered

    def _unsubscribe(self, callback: GameEventCallback) -> None:
        self._subscribers = [item for item in self._subscribers if item is not callback]

