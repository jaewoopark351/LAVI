#20260717_kpopmodder: Isolates game event subscription lifecycle from the bus.
from __future__ import annotations

from typing import TYPE_CHECKING

from .game_event_callback import GameEventCallback

if TYPE_CHECKING:
    from .game_event_bus import GameEventBus


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
