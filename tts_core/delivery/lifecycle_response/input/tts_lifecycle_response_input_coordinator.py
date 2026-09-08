#20260907_kpopmodder: Orchestrate one classified lifecycle TTS enqueue and its receipts.
from __future__ import annotations

from ..identity import TtsLifecycleResponseIdentityClassifier
from .tts_lifecycle_response_input_outcome import (
    TtsLifecycleResponseInputOutcome,
)


class TtsLifecycleResponseInputCoordinator:
    def __init__(
        self,
        *,
        owner,
        identity_classifier,
        delivery_adapter_callback,
        receipt_listeners,
    ) -> None:
        if type(identity_classifier) is not TtsLifecycleResponseIdentityClassifier:
            raise TypeError("identity_classifier must be exact")
        if not callable(delivery_adapter_callback):
            raise TypeError("delivery_adapter_callback must be callable")
        self._owner = owner
        self._identities = identity_classifier
        self._delivery_adapter_callback = delivery_adapter_callback
        self._receipt_listeners = receipt_listeners

    def receive(
        self,
        *,
        payload: object,
        items: object,
        response_generation: object,
    ) -> TtsLifecycleResponseInputOutcome:
        identity = self._identities.classify(payload)
        if identity is None:
            return TtsLifecycleResponseInputOutcome.unrelated()
        speech_items = tuple(
            item
            for item in tuple(items or ())
            if item and not self._owner.text_processor.is_tts_skippable(item)
        )
        lifecycle_generation = (
            None
            if identity.delivery_mode == "non_preempting"
            else response_generation
        )
        adapter = self._delivery_adapter_callback()
        receipt = adapter.enqueue(
            event_id=identity.event_id,
            route_kind=identity.route_kind,
            response_kind=identity.response_kind,
            response_generation=lifecycle_generation,
            delivery_mode=identity.delivery_mode,
            items=speech_items,
            enqueue_callback=self._enqueue,
        )
        self._receipt_listeners.notify_enqueue(receipt)
        for playback_receipt in adapter.drain_deferred_playback_receipts():
            self._receipt_listeners.notify_playback(playback_receipt)
        if receipt.accepted:
            self._owner.process_input_queue(
                self._owner.current_plugin.synthesize
            )
        return TtsLifecycleResponseInputOutcome.handled_receipt(receipt)

    def _enqueue(self, items, **identity):
        return self._owner.enqueue_lifecycle_response_items(
            items,
            **identity,
        )


__all__ = ("TtsLifecycleResponseInputCoordinator",)
