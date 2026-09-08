#20260907_kpopmodder: Construct typed lifecycle TTS delivery receipts.
from __future__ import annotations

from ..state import TtsLifecycleResponsePlaybackState
from ..tts_lifecycle_response_enqueue_receipt import (
    TtsLifecycleResponseEnqueueReceipt,
)
from ..tts_lifecycle_response_playback_receipt import (
    TtsLifecycleResponsePlaybackReceipt,
)


class TtsLifecycleResponseReceiptFactory:
    @staticmethod
    def enqueue(
        *,
        event_id: str,
        route_kind: str,
        response_kind: str,
        response_generation: int | None,
        delivery_mode: str,
        accepted: bool,
        item_count: int,
        reason: str,
    ) -> TtsLifecycleResponseEnqueueReceipt:
        return TtsLifecycleResponseEnqueueReceipt(
            event_id=event_id,
            route_kind=route_kind,
            response_kind=response_kind,
            response_generation=response_generation,
            delivery_mode=delivery_mode,
            accepted=accepted,
            item_count=item_count,
            reason=reason,
        )

    @staticmethod
    def playback(
        state: TtsLifecycleResponsePlaybackState,
        *,
        observed: bool,
        reason: str,
        played_item_count: int,
    ) -> TtsLifecycleResponsePlaybackReceipt:
        return TtsLifecycleResponsePlaybackReceipt(
            event_id=state.event_id,
            route_kind=state.route_kind,
            response_kind=state.response_kind,
            response_generation=state.response_generation,
            delivery_mode=state.delivery_mode,
            observed=observed,
            played_item_count=played_item_count,
            item_count=state.item_count,
            reason=reason,
        )


__all__ = ("TtsLifecycleResponseReceiptFactory",)
