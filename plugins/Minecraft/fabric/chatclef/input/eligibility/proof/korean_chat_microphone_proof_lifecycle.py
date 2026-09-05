#20260905_kpopmodder: Owns one Korean eligibility proof's live lifecycle.
from __future__ import annotations

import hashlib
import threading


class KoreanChatMicrophoneProofLifecycle:
    def __init__(
        self,
        *,
        event: object,
        consumed_evidence: object,
        owner: object,
    ) -> None:
        self._event = event
        self._consumed_evidence = consumed_evidence
        self._owner = owner
        self._event_signature = self._signature(event)
        self._lock = threading.Lock()
        self._closed = False

    @property
    def event(self):
        return self._event

    @property
    def consumed_evidence(self):
        return self._consumed_evidence

    @property
    def owner(self):
        return self._owner

    @property
    def event_signature(self) -> tuple[object, ...]:
        return self._event_signature

    @property
    def lock(self):
        return self._lock

    @property
    def closed(self) -> bool:
        with self._lock:
            return self._closed

    def matches_event(self, event: object, owner: object = None) -> bool:
        if event is not self._event:
            return False
        if owner is not None and owner is not self._owner:
            return False
        with self._lock:
            return self.matches_event_locked(event)

    def matches_event_locked(self, event: object) -> bool:
        if self._closed or self._signature(event) != self._event_signature:
            return False
        try:
            return bool(self._consumed_evidence.is_live_for(event, self._owner))
        except Exception:
            return False

    def close(self) -> bool:
        with self._lock:
            if self._closed:
                return False
            self._closed = True
        try:
            self._consumed_evidence.close()
        except Exception:
            pass
        return True

    @staticmethod
    def _signature(event: object) -> tuple[object, ...]:
        text = getattr(event, "text", None)
        digest = (
            hashlib.sha256(text.encode("utf-8")).digest()
            if type(text) is str
            else b""
        )
        return (
            getattr(event, "source", None),
            getattr(event, "provider_id", None),
            getattr(event, "event_kind", None),
            getattr(event, "final", None),
            getattr(event, "event_id", None),
            len(text) if type(text) is str else -1,
            digest,
        )


__all__ = ("KoreanChatMicrophoneProofLifecycle",)
