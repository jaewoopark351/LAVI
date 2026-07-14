#20260713_kpopmodder: Centralize StarCraft2 event fan-out through a single subscription channel.
from __future__ import annotations

from typing import Any, Callable, Dict, List, Optional

from .starcraft2_contracts import StarCraft2Event
from .starcraft2_game_event_bridge import StarCraft2GameEventBridge
from core.logger import log_print


EventCallback = Callable[[Dict[str, Any]], None]


class StarCraft2EventBusSubscription:
    #20260713_kpopmodder: Keep subscription lifecycle explicit and safely reversible.
    def __init__(self, bus: "StarCraft2EventBus", callback: EventCallback):
        self._bus = bus
        self._callback = callback
        self._unsubscribed = False

    def unsubscribe(self) -> None:
        if self._unsubscribed:
            return
        self._unsubscribed = True
        self._bus._unsubscribe(self._callback)


class StarCraft2EventBus:
    #20260713_kpopmodder: Public event fan-out boundary for SC2 stdout/game/TTS/memory flow.
    def __init__(self, common_event_bus: Any = None):
        self._subscribers: List[EventCallback] = []
        self._status_event_callback_subscription = None
        self._common_event_bridge = StarCraft2GameEventBridge(common_event_bus)

    def set_common_event_bus(self, common_event_bus: Any = None) -> None:
        self._common_event_bridge.set_event_bus(common_event_bus)

    def subscribe_common_events(self, callback: Optional[EventCallback]):
        common_bus = getattr(self._common_event_bridge, "game_event_bus", None)
        subscribe = getattr(common_bus, "subscribe", None)
        if not callable(subscribe):
            return None
        return subscribe(callback)

    def subscribe(self, callback: Optional[EventCallback]) -> StarCraft2EventBusSubscription:
        if not callable(callback):
            return StarCraft2EventBusSubscription(self, lambda event: None)
        self._subscribers.append(callback)
        return StarCraft2EventBusSubscription(self, callback)

    def set_status_event_callback(self, callback: Optional[EventCallback]) -> Optional[StarCraft2EventBusSubscription]:
        # 20260713_kpopmodder: keep compatibility for legacy setter calls
        # by routing through the subscription channel (single path only).
        previous = self._status_event_callback_subscription
        if previous is not None:
            previous.unsubscribe()
            self._status_event_callback_subscription = None
        if not callable(callback):
            return None
        subscription = self.subscribe(callback)
        self._status_event_callback_subscription = subscription
        return subscription

    def set_subscribers(self, callbacks: List[EventCallback]) -> None:
        self._subscribers = list(callbacks) if callbacks is not None else []

    def emit(self, event: Dict[str, Any] | StarCraft2Event | None) -> bool:
        normalized = StarCraft2Event.from_mapping(event)
        if not normalized.event_type:
            return False
        payload = normalized.to_dict()
        event_type = str(payload.get("event_type") or "").strip().lower()
        if event_type:
            payload = dict(payload)
        self._emit_common_event(payload)
        delivered = False
        for subscriber in list(self._subscribers):
            if not callable(subscriber):
                continue
            try:
                subscriber(payload)
                delivered = True
            except Exception as e:
                log_print(f"[StarCraft2EventBus] event subscriber failed: {e}")

        return delivered

    def publish(self, event: Dict[str, Any] | None) -> bool:
        # Backward-compatible alias for existing call sites.
        return self.emit(event)

    def _unsubscribe(self, callback: EventCallback) -> None:
        self._subscribers = [item for item in self._subscribers if item is not callback]

    def _emit_common_event(self, payload: Dict[str, Any]) -> None:
        bridge = getattr(self, "_common_event_bridge", None)
        emit = getattr(bridge, "emit", None)
        if not callable(emit):
            return
        emit(payload)


_StarCraft2EventBusSubscription = StarCraft2EventBusSubscription
_StarCraft2EventBus = StarCraft2EventBus
