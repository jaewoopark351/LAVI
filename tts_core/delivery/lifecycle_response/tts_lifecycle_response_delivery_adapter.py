#20260907_kpopmodder: Coordinate lifecycle TTS dedupe and independent enqueue/playback facts.
from __future__ import annotations

import threading

from .receipt import (
    TtsLifecycleResponseDeferredReceiptBuffer,
    TtsLifecycleResponseReceiptFactory,
)
from .state import (
    TtsLifecycleResponsePlaybackStateRegistry,
)
from .tts_lifecycle_response_enqueue_receipt import (
    TtsLifecycleResponseEnqueueReceipt,
)
from .tts_lifecycle_response_event_deduplicator import (
    TtsLifecycleResponseEventDeduplicator,
)
from .tts_lifecycle_response_playback_receipt import (
    TtsLifecycleResponsePlaybackReceipt,
)


class TtsLifecycleResponseDeliveryAdapter:
    def __init__(self, *, deduplicator=None) -> None:
        if deduplicator is not None and type(deduplicator) is not (
            TtsLifecycleResponseEventDeduplicator
        ):
            raise TypeError("deduplicator must be exact")
        self._deduplicator = (
            deduplicator or TtsLifecycleResponseEventDeduplicator()
        )
        self._playback_registry = TtsLifecycleResponsePlaybackStateRegistry(
            capacity=self._deduplicator.capacity
        )
        self._deferred_playback_receipts = (
            TtsLifecycleResponseDeferredReceiptBuffer(
                capacity=self._deduplicator.capacity
            )
        )
        self._receipt_factory = TtsLifecycleResponseReceiptFactory()
        self._lock = threading.Lock()

    def enqueue(
        self,
        *,
        event_id: object,
        route_kind: object,
        response_kind: object,
        items: object,
        enqueue_callback,
        response_generation: object = None,
        delivery_mode: object = "current_input",
    ) -> TtsLifecycleResponseEnqueueReceipt:
        event_id, route_kind, response_kind = self._validate_identity(
            event_id,
            route_kind,
            response_kind,
        )
        if response_generation is not None and (
            type(response_generation) is not int or response_generation < 0
        ):
            raise ValueError(
                "response_generation must be a non-negative exact int or None"
            )
        if delivery_mode not in {"current_input", "non_preempting"}:
            raise ValueError("delivery_mode must be a registered exact str")
        if not callable(enqueue_callback):
            raise TypeError("enqueue_callback must be callable")
        normalized_items = tuple(
            item
            for item in tuple(items or ())
            if type(item) is str and item.strip()
        )
        if not normalized_items:
            return self._receipt_factory.enqueue(
                event_id=event_id,
                route_kind=route_kind,
                response_kind=response_kind,
                response_generation=response_generation,
                delivery_mode=delivery_mode,
                accepted=False,
                item_count=0,
                reason="skipped_empty",
            )
        identity = (event_id, route_kind, response_kind)
        playback_state = None
        with self._lock:
            if self._playback_registry.contains(
                identity
            ) or self._deduplicator.contains(
                event_id,
                route_kind=route_kind,
                response_kind=response_kind,
            ):
                reservation_reason = "duplicate_event"
            elif self._playback_registry.is_full():
                reservation_reason = "delivery_failed"
            elif not self._deduplicator.claim(
                event_id,
                route_kind=route_kind,
                response_kind=response_kind,
            ):
                reservation_reason = "duplicate_event"
            else:
                reservation_reason = None
                playback_state = self._playback_registry.reserve(
                    identity,
                    response_generation=response_generation,
                    delivery_mode=delivery_mode,
                    item_count=len(normalized_items),
                )
        if reservation_reason is not None:
            return self._receipt_factory.enqueue(
                event_id=event_id,
                route_kind=route_kind,
                response_kind=response_kind,
                response_generation=response_generation,
                delivery_mode=delivery_mode,
                accepted=False,
                item_count=0,
                reason=reservation_reason,
            )
        try:
            enqueued_count = enqueue_callback(
                normalized_items,
                event_id=event_id,
                route_kind=route_kind,
                response_kind=response_kind,
                response_generation=response_generation,
                delivery_mode=delivery_mode,
                delivery_token=playback_state.delivery_token,
            )
        except Exception:
            self._rollback_playback_reservation(identity, playback_state)
            return self._receipt_factory.enqueue(
                event_id=event_id,
                route_kind=route_kind,
                response_kind=response_kind,
                response_generation=response_generation,
                delivery_mode=delivery_mode,
                accepted=False,
                item_count=0,
                reason="delivery_failed",
            )
        if type(enqueued_count) is not int or enqueued_count != len(
            normalized_items
        ):
            self._rollback_playback_reservation(identity, playback_state)
            return self._receipt_factory.enqueue(
                event_id=event_id,
                route_kind=route_kind,
                response_kind=response_kind,
                response_generation=response_generation,
                delivery_mode=delivery_mode,
                accepted=False,
                item_count=0,
                reason="delivery_failed",
            )
        with self._lock:
            if not self._playback_registry.is_current(identity, playback_state):
                reservation_committed = False
            else:
                outcome = playback_state.commit_enqueue()
                if outcome is not None:
                    self._playback_registry.remove_if_current(
                        identity,
                        playback_state,
                    )
                    self._deferred_playback_receipts.append(
                        self._playback_receipt(playback_state, outcome)
                    )
                reservation_committed = True
        if not reservation_committed:
            return self._receipt_factory.enqueue(
                event_id=event_id,
                route_kind=route_kind,
                response_kind=response_kind,
                response_generation=response_generation,
                delivery_mode=delivery_mode,
                accepted=False,
                item_count=0,
                reason="delivery_failed",
            )
        return self._receipt_factory.enqueue(
            event_id=event_id,
            route_kind=route_kind,
            response_kind=response_kind,
            response_generation=response_generation,
            delivery_mode=delivery_mode,
            accepted=True,
            item_count=enqueued_count,
            reason="enqueued",
        )

    def _rollback_playback_reservation(self, identity, playback_state) -> None:
        with self._lock:
            self._playback_registry.remove_if_current(identity, playback_state)

    def observe_playback(
        self,
        *,
        event_id: object,
        item_index: object,
        played: object,
        reason: object,
        route_kind: object = None,
        response_kind: object = None,
        delivery_token: object = None,
    ) -> TtsLifecycleResponsePlaybackReceipt | None:
        if type(event_id) is not str or not event_id:
            return None
        if type(item_index) is not int or item_index < 0:
            return None
        if type(played) is not bool:
            return None
        if delivery_token is not None and (
            type(delivery_token) is not int or delivery_token <= 0
        ):
            return None
        bounded_reason = self._bounded_reason(reason)
        with self._lock:
            identity = self._playback_registry.resolve_identity(
                event_id,
                route_kind=route_kind,
                response_kind=response_kind,
            )
            if identity is None:
                return None
            state = self._playback_registry.get(identity)
            if state is None or item_index >= state.item_count:
                return None
            if (
                delivery_token is not None
                and delivery_token != state.delivery_token
            ):
                return None
            #20260907_kpopmodder: The callback may only expose queue items;
            # playback cannot become a fact until its exact enqueue count has
            # committed successfully.
            outcome = state.record_observation(
                item_index=item_index,
                played=played,
                reason=bounded_reason,
            )
            if outcome is None:
                return None
            self._playback_registry.remove_if_current(identity, state)
            return self._playback_receipt(state, outcome)

    def drain_deferred_playback_receipts(self) -> tuple:
        with self._lock:
            return self._deferred_playback_receipts.drain()

    def clear(self) -> None:
        with self._lock:
            self._playback_registry.clear()
            self._deferred_playback_receipts.clear()
            self._deduplicator.clear()

    @staticmethod
    def _bounded_reason(reason: object) -> str:
        if type(reason) is str and reason and len(reason) <= 160:
            return reason
        return "delivery_failed"

    def _playback_receipt(self, state, outcome):
        observed, reason, played_item_count = outcome
        return self._receipt_factory.playback(
            state,
            observed=observed,
            reason=reason,
            played_item_count=played_item_count,
        )

    @staticmethod
    def _validate_identity(event_id, route_kind, response_kind):
        values = (event_id, route_kind, response_kind)
        if any(
            type(value) is not str or not value or len(value) > 160
            for value in values
        ):
            raise ValueError("lifecycle TTS identity must be bounded exact strings")
        return values

__all__ = ("TtsLifecycleResponseDeliveryAdapter",)
