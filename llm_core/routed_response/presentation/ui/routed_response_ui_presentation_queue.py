#20260905_kpopmodder: Keep routed-response UI presentation in the established responsibility-split boundary.
#20260907_kpopmodder: Own a bounded instance-local FIFO for asynchronous Chat UI messages.
from __future__ import annotations

from collections import deque
import threading


class RoutedResponseUiPresentationQueue:
    def __init__(self, *, capacity: int = 256) -> None:
        if type(capacity) is not int or capacity <= 0:
            raise ValueError("capacity must be a positive exact int")
        self._capacity = capacity
        self._items = deque()
        self._lock = threading.Lock()
        self._epoch = 0

    def enqueue(self, message: object) -> bool:
        with self._lock:
            if len(self._items) >= self._capacity:
                return False
            self._items.append(message)
        return True

    def drain(self) -> tuple:
        with self._lock:
            items = tuple(self._items)
            self._items.clear()
            self._epoch += 1
        return items

    def snapshot(self) -> tuple[int, tuple]:
        with self._lock:
            return self._epoch, tuple(self._items)

    def acknowledge_presented(
        self,
        *,
        epoch: int,
        items: tuple,
    ) -> bool:
        if type(epoch) is not int or type(items) is not tuple or not items:
            return False
        with self._lock:
            if epoch != self._epoch or len(items) > len(self._items):
                return False
            if tuple(self._items)[: len(items)] != items:
                return False
            for _ in items:
                self._items.popleft()
        return True

    def is_epoch_current(self, epoch: object) -> bool:
        with self._lock:
            return type(epoch) is int and epoch == self._epoch

    def clear(self) -> None:
        with self._lock:
            self._items.clear()
            self._epoch += 1


__all__ = ("RoutedResponseUiPresentationQueue",)
