#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260713_kpopmodder: Centralize StarCraft2 event fan-out through a single subscription channel.
from __future__ import annotations

from typing import Any, Callable, Dict, List, Optional

from .starcraft2_contracts import StarCraft2Event
from .starcraft2_game_event_bridge import StarCraft2GameEventBridge
from core.logger import log_print


EventCallback = Callable[[Dict[str, Any]], None]
TypedEventCallback = Callable[[StarCraft2Event], None]

class StarCraft2EventBusSubscription:
    #20260713_kpopmodder: Keep subscription lifecycle explicit and safely reversible.
    def __init__(
        self,
        bus: "StarCraft2EventBus",
        callback: EventCallback | TypedEventCallback,
        typed: bool = False,
    ):
        self._bus = bus
        self._callback = callback
        self._typed = bool(typed)
        self._unsubscribed = False

    def unsubscribe(self) -> None:
        if self._unsubscribed:
            return
        self._unsubscribed = True
        self._bus._unsubscribe(self._callback, typed=self._typed)
