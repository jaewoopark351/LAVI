#20260717_kpopmodder: Isolates game event bus dispatch behavior from event DTOs.
from typing import Any, Dict, List, Optional

from core.logger import log_print

from .game_event_bus_subscription import GameEventBusSubscription
from .game_event_callback import GameEventCallback
from .game_event_dto import GameEventDTO


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
