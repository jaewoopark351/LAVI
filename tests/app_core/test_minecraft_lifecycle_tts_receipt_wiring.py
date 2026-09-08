#20260907_kpopmodder: Verify app composition logs enqueue and playback as different sinks.
from __future__ import annotations

import unittest

from app_core.composition_core.component_wiring import (
    MinecraftLifecycleTtsReceiptWiring,
)
from tts_core.delivery.lifecycle_response import (
    TtsLifecycleResponseEnqueueReceipt,
    TtsLifecycleResponsePlaybackReceipt,
)


class MinecraftLifecycleTtsReceiptWiringTests(unittest.TestCase):
    def test_receipts_keep_distinct_sink_and_truth_fields(self):
        llm = _LLM()
        tts = _TTS()
        callbacks = MinecraftLifecycleTtsReceiptWiring().wire(
            llm=llm,
            tts=tts,
        )
        enqueue = TtsLifecycleResponseEnqueueReceipt(
            event_id="8" * 32,
            route_kind="crafting_lifecycle",
            response_kind="crafting_terminal",
            accepted=True,
            item_count=1,
            reason="enqueued",
            delivery_mode="non_preempting",
        )
        playback = TtsLifecycleResponsePlaybackReceipt(
            event_id="8" * 32,
            route_kind="crafting_lifecycle",
            response_kind="crafting_terminal",
            observed=True,
            played_item_count=1,
            item_count=1,
            reason="played",
            delivery_mode="non_preempting",
        )

        callbacks[0](enqueue)
        callbacks[1](playback)

        self.assertEqual("tts_queue", llm.values[0]["sink"])
        self.assertTrue(llm.values[0]["delivered"])
        self.assertEqual("tts_playback", llm.values[1]["sink"])
        self.assertTrue(llm.values[1]["delivered"])
        self.assertIsNone(llm.values[0]["response_generation"])
        self.assertEqual("non_preempting", llm.values[0]["delivery_mode"])


class _LLM:
    def __init__(self):
        self.values = []

    def log_routed_response_sink_delivery(self, **values):
        self.values.append(values)
        return True


class _TTS:
    def __init__(self):
        self.enqueue_listeners = []
        self.playback_listeners = []

    def add_lifecycle_response_enqueue_receipt_listener(self, callback):
        self.enqueue_listeners.append(callback)

    def add_lifecycle_response_playback_receipt_listener(self, callback):
        self.playback_listeners.append(callback)


if __name__ == "__main__":
    unittest.main()
