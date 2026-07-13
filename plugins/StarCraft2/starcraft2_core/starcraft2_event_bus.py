#20260713_kpopmodder: Centralize StarCraft2 event fan-out through a single subscription channel.
from __future__ import annotations

from typing import Any, Callable, Dict, List, Optional

from .starcraft2_contracts import StarCraft2Event
from core.logger import log_print


EventCallback = Callable[[Dict[str, Any]], None]


class _StarCraft2EventBusSubscription:
    #20260713_kpopmodder: Keep subscription lifecycle explicit and safely reversible.
    def __init__(self, bus: "_StarCraft2EventBus", callback: EventCallback):
        self._bus = bus
        self._callback = callback
        self._unsubscribed = False

    def unsubscribe(self) -> None:
        if self._unsubscribed:
            return
        self._unsubscribed = True
        self._bus._unsubscribe(self._callback)


class _StarCraft2EventBus:
    def __init__(self):
        self._subscribers: List[EventCallback] = []
        self._status_event_callback_subscription = None

    def subscribe(self, callback: Optional[EventCallback]) -> _StarCraft2EventBusSubscription:
        if not callable(callback):
            return _StarCraft2EventBusSubscription(self, lambda event: None)
        self._subscribers.append(callback)
        return _StarCraft2EventBusSubscription(self, callback)

    def set_status_event_callback(self, callback: Optional[EventCallback]) -> Optional[_StarCraft2EventBusSubscription]:
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
