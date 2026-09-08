#20260907_kpopmodder: Bound lifecycle TTS event-ID replay suppression to one TTS instance.
from __future__ import annotations

from collections import OrderedDict
import threading


class TtsLifecycleResponseEventDeduplicator:
    def __init__(self, *, capacity: int = 1024) -> None:
        if type(capacity) is not int or capacity <= 0:
            raise ValueError("capacity must be a positive exact int")
        self._capacity = capacity
        self._claimed = OrderedDict()
        self._lock = threading.Lock()

    @property
    def capacity(self) -> int:
        return self._capacity

    def claim(
        self,
        event_id: object,
        *,
        route_kind: object = None,
        response_kind: object = None,
    ) -> bool:
        identity = self._identity(event_id, route_kind, response_kind)
        if identity is None:
            return False
        with self._lock:
            if identity in self._claimed:
                return False
            self._claimed[identity] = None
            while len(self._claimed) > self._capacity:
                self._claimed.popitem(last=False)
        return True

    def release(
        self,
        event_id: object,
        *,
        route_kind: object = None,
        response_kind: object = None,
    ) -> bool:
        identity = self._identity(event_id, route_kind, response_kind)
        if identity is None:
            return False
        with self._lock:
            if identity not in self._claimed:
                return False
            self._claimed.pop(identity)
            return True

    def contains(
        self,
        event_id: object,
        *,
        route_kind: object = None,
        response_kind: object = None,
    ) -> bool:
        identity = self._identity(event_id, route_kind, response_kind)
        if identity is None:
            return False
        with self._lock:
            return identity in self._claimed

    def clear(self) -> None:
        with self._lock:
            self._claimed.clear()

    @staticmethod
    def _identity(event_id, route_kind, response_kind):
        values = (event_id, route_kind, response_kind)
        if type(event_id) is not str or not event_id or len(event_id) > 160:
            return None
        if route_kind is None and response_kind is None:
            return (event_id, "", "")
        if any(type(value) is not str or not value or len(value) > 160 for value in values):
            return None
        return values


__all__ = ("TtsLifecycleResponseEventDeduplicator",)
