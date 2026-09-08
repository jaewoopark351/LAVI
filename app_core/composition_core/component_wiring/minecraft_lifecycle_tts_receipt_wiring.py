#20260907_kpopmodder: Join Minecraft lifecycle TTS receipts to bounded delivery diagnostics.
from __future__ import annotations

from tts_core.delivery.lifecycle_response import (
    TtsLifecycleResponseEnqueueReceipt,
    TtsLifecycleResponsePlaybackReceipt,
)


class MinecraftLifecycleTtsReceiptWiring:
    def __init__(self) -> None:
        self._callbacks = {}

    def wire(self, *, llm, tts) -> tuple:
        logger = getattr(llm, "log_routed_response_sink_delivery", None)
        add_enqueue = getattr(
            tts,
            "add_lifecycle_response_enqueue_receipt_listener",
            None,
        )
        add_playback = getattr(
            tts,
            "add_lifecycle_response_playback_receipt_listener",
            None,
        )
        if not all(callable(value) for value in (logger, add_enqueue, add_playback)):
            return ()
        key = (id(llm), id(tts))
        callbacks = self._callbacks.get(key)
        if callbacks is None:

            def on_enqueue(receipt):
                if type(receipt) is not TtsLifecycleResponseEnqueueReceipt:
                    return False
                return logger(
                    source="minecraft_chatclef",
                    event_id=receipt.event_id,
                    route_kind=receipt.route_kind,
                    response_kind=receipt.response_kind,
                    sink="tts_queue",
                    response_generation=receipt.response_generation,
                    delivered=receipt.accepted,
                    reason=receipt.reason,
                    delivery_mode=receipt.delivery_mode,
                )

            def on_playback(receipt):
                if type(receipt) is not TtsLifecycleResponsePlaybackReceipt:
                    return False
                return logger(
                    source="minecraft_chatclef",
                    event_id=receipt.event_id,
                    route_kind=receipt.route_kind,
                    response_kind=receipt.response_kind,
                    sink="tts_playback",
                    response_generation=receipt.response_generation,
                    delivered=receipt.observed,
                    reason=receipt.reason,
                    delivery_mode=receipt.delivery_mode,
                )

            callbacks = (on_enqueue, on_playback)
            self._callbacks[key] = callbacks
        add_enqueue(callbacks[0])
        add_playback(callbacks[1])
        return callbacks


__all__ = ("MinecraftLifecycleTtsReceiptWiring",)
