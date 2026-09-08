#20260907_kpopmodder: Preserve TTS lifecycle public seams through focused collaborators.
from __future__ import annotations

from .identity import TtsLifecycleResponseIdentityClassifier
from .input import TtsLifecycleResponseInputCoordinator
from .receipt import TtsLifecycleResponseReceiptListenerCoordinator
from .tts_lifecycle_response_delivery_adapter import (
    TtsLifecycleResponseDeliveryAdapter,
)


class TtsLifecycleResponseFacade:
    """Compatibility facade; delivery state remains under the adapter lock."""

    def __init__(self, owner) -> None:
        self._owner = owner
        self._identities = TtsLifecycleResponseIdentityClassifier()
        self._receipt_listeners = (
            TtsLifecycleResponseReceiptListenerCoordinator(owner)
        )
        self._input = TtsLifecycleResponseInputCoordinator(
            owner=owner,
            identity_classifier=self._identities,
            delivery_adapter_callback=self.delivery_adapter,
            receipt_listeners=self._receipt_listeners,
        )

    def receive(self, *, payload, items, response_generation):
        return self._input.receive(
            payload=payload,
            items=items,
            response_generation=response_generation,
        )

    def identity(self, payload: object):
        identity = self._identities.classify(payload)
        return None if identity is None else identity.as_tuple()

    def delivery_adapter(self):
        adapter = getattr(
            self._owner,
            "lifecycle_response_delivery_adapter",
            None,
        )
        if adapter is None:
            adapter = TtsLifecycleResponseDeliveryAdapter()
            self._owner.lifecycle_response_delivery_adapter = adapter
        return adapter

    def add_enqueue_listener(self, callback) -> None:
        self._receipt_listeners.add_enqueue_listener(callback)

    def add_playback_listener(self, callback) -> None:
        self._receipt_listeners.add_playback_listener(callback)

    def notify_enqueue(self, receipt) -> None:
        self._receipt_listeners.notify_enqueue(receipt)

    def notify_playback(self, receipt) -> None:
        self._receipt_listeners.notify_playback(receipt)

    def observe_playback(self, **values):
        receipt = self.delivery_adapter().observe_playback(**values)
        if receipt is not None:
            self._receipt_listeners.notify_playback(receipt)
        return receipt

    def clear_delivery(self) -> None:
        self.delivery_adapter().clear()

    def clear_receipt_listeners(self) -> None:
        self._receipt_listeners.clear()

    def shutdown(self) -> None:
        self.clear_receipt_listeners()
        self.clear_delivery()


__all__ = ("TtsLifecycleResponseFacade",)
