#20260907_kpopmodder: Own lifecycle receipt listener registration and notification.
from __future__ import annotations


class TtsLifecycleResponseReceiptListenerCoordinator:
    """Instance-owned listener facade; intentionally owns no lock."""

    def __init__(self, owner) -> None:
        self._owner = owner

    def add_enqueue_listener(self, callback) -> None:
        self._add(
            "lifecycle_response_enqueue_receipt_listeners",
            callback,
            "enqueue receipt listener must be callable",
        )

    def add_playback_listener(self, callback) -> None:
        self._add(
            "lifecycle_response_playback_receipt_listeners",
            callback,
            "playback receipt listener must be callable",
        )

    def notify_enqueue(self, receipt) -> None:
        self._notify("lifecycle_response_enqueue_receipt_listeners", receipt)

    def notify_playback(self, receipt) -> None:
        self._notify("lifecycle_response_playback_receipt_listeners", receipt)

    def clear(self) -> None:
        self._listeners("lifecycle_response_enqueue_receipt_listeners").clear()
        self._listeners("lifecycle_response_playback_receipt_listeners").clear()

    def _add(self, attribute: str, callback, error: str) -> None:
        if not callable(callback):
            raise TypeError(error)
        listeners = self._listeners(attribute)
        if callback not in listeners:
            listeners.append(callback)

    def _notify(self, attribute: str, receipt) -> None:
        for callback in tuple(self._listeners(attribute)):
            try:
                callback(receipt)
            except Exception:
                continue

    def _listeners(self, attribute: str) -> list:
        listeners = getattr(self._owner, attribute, None)
        if listeners is None:
            listeners = []
            setattr(self._owner, attribute, listeners)
        return listeners


__all__ = ("TtsLifecycleResponseReceiptListenerCoordinator",)
