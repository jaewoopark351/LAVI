#20260905_kpopmodder: Owns one consumed ingress event's live eligibility lifecycle.
from __future__ import annotations

import hashlib
import threading


class ConsumedIngressEvidenceLifecycle:
    def __init__(
        self,
        *,
        event: object,
        event_signature: tuple[object, ...],
    ) -> None:
        self._event = event
        self._event_signature = event_signature
        self._lock = threading.Lock()
        self._eligibility_owner = None
        self._closed = False

    @property
    def event(self):
        return self._event

    @property
    def event_signature(self) -> tuple[object, ...]:
        return self._event_signature

    @property
    def lock(self):
        return self._lock

    @property
    def eligibility_owner(self):
        with self._lock:
            return self._eligibility_owner

    @property
    def closed(self) -> bool:
        with self._lock:
            return self._closed

    def claim_for_eligibility(self, event: object, owner: object) -> bool:
        if not self.matches_event(event) or owner is None:
            return False
        with self._lock:
            if self._closed or self._eligibility_owner is not None:
                return False
            self._eligibility_owner = owner
            return True

    def is_live_for(self, event: object, owner: object = None) -> bool:
        if not self.matches_event(event):
            return False
        with self._lock:
            return self.is_live_for_locked(owner)

    def is_live_for_locked(self, owner: object = None) -> bool:
        if self._closed:
            return False
        return owner is None or owner is self._eligibility_owner

    def close(self) -> bool:
        with self._lock:
            return self.close_locked()

    def close_locked(self) -> bool:
        if self._closed:
            return False
        self._closed = True
        return True

    def matches_event(self, event: object) -> bool:
        return event is self._event and self._signature(event) == self._event_signature

    @staticmethod
    def _signature(event: object) -> tuple[object, ...]:
        try:
            text = event.text
            return (
                event.source,
                event.provider_id,
                event.event_kind,
                event.final,
                event.event_id,
                len(text),
                hashlib.sha256(text.encode("utf-8")).digest(),
            )
        except Exception:
            return ()


__all__ = ("ConsumedIngressEvidenceLifecycle",)
