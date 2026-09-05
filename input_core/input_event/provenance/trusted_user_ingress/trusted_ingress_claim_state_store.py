#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

import threading


class TrustedIngressClaimStateStore:
    def __init__(self, *, capacity: int) -> None:
        if type(capacity) is not int or capacity < 0:
            raise ValueError("capacity must be a non-negative exact int")
        self.capacity = capacity
        self.lock = threading.RLock()
        self.registry_token = object()
        self.producer_factory_token = None
        self.records: dict[object, dict[str, object]] = {}
        self.live_event_ids: dict[str, object] = {}
        self.live_event_objects: dict[int, object] = {}

    def bind_producer_factory(self, factory_token: object) -> object:
        if factory_token is None:
            raise ValueError("factory token is required")
        with self.lock:
            if self.producer_factory_token is None:
                self.producer_factory_token = factory_token
            elif self.producer_factory_token is not factory_token:
                raise RuntimeError(
                    "trusted ingress registry already has a producer factory"
                )
            return self.registry_token

    @property
    def live_count(self) -> int:
        with self.lock:
            return len(self.records)


__all__ = ("TrustedIngressClaimStateStore",)
