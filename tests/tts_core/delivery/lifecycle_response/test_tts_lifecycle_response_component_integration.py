#20260907_kpopmodder: Verify routed lifecycle payload metadata reaches the TTS receipt boundary.
from __future__ import annotations

import threading
import unittest
from queue import Queue
from types import SimpleNamespace

from tts_core import TTS
from tts_core.delivery.lifecycle_response import (
    TtsLifecycleResponseDeliveryAdapter,
)
from tts_core.tts_interrupt_controller import TTSInterruptController
from tts_core.tts_queue_worker import TTSQueueWorker


class TtsLifecycleResponseComponentIntegrationTests(unittest.TestCase):
    def test_current_input_status_preserves_response_generation(self):
        tts = self._tts()
        payload = {
            "text": "다이아 곡괭이 만드는 중이야",
            "response_generation": 10,
            "event_id": "6" * 32,
            "source": "minecraft_chatclef",
            "route_kind": "command_status_query",
            "response_kind": "command_status",
            "delivery_mode": "current_input",
            "presentation": {
                "source_kind": "minecraft",
                "badge_label": "Minecraft",
                "log": (
                    '{"command_name":"gamma",'
                    '"form_kind":"explicit_value"}'
                ),
            },
        }

        receipt = tts.receive_input(payload)

        queued = tts.input_queue.get_nowait()
        self.assertTrue(receipt.accepted)
        self.assertEqual(10, receipt.response_generation)
        self.assertEqual(10, queued["response_generation"])
        self.assertIs(type(queued["lifecycle_delivery_token"]), int)
        self.assertGreater(queued["lifecycle_delivery_token"], 0)
        self.assertEqual(10, tts.latest_response_generation)
        self.assertNotIn("presentation", queued)
        self.assertNotIn("command_name", queued["text"])

    def test_current_input_status_enqueues_and_observes_one_exact_playback(self):
        tts = self._tts()
        enqueue_receipts = []
        playback_receipts = []
        tts.add_lifecycle_response_enqueue_receipt_listener(
            enqueue_receipts.append
        )
        tts.add_lifecycle_response_playback_receipt_listener(
            playback_receipts.append
        )
        payload = {
            "text": "다이아 곡괭이 만드는 중이야",
            "response_generation": 11,
            "event_id": "9" * 32,
            "source": "minecraft_chatclef",
            "route_kind": "command_status_query",
            "response_kind": "command_status",
            "delivery_mode": "current_input",
            "presentation": {
                "source_kind": "minecraft",
                "badge_label": "Minecraft",
                "log": '{"command_name":"get"}',
            },
        }

        receipt = tts.receive_input(payload)
        queued = tts.input_queue.get_nowait()
        playback = tts.observe_lifecycle_response_playback(
            event_id=queued["lifecycle_event_id"],
            route_kind=queued["lifecycle_route_kind"],
            response_kind=queued["lifecycle_response_kind"],
            delivery_token=queued["lifecycle_delivery_token"],
            item_index=queued["lifecycle_item_index"],
            played=True,
            reason="played",
        )

        self.assertTrue(receipt.accepted)
        self.assertEqual(1, receipt.item_count)
        self.assertEqual(1, len(enqueue_receipts))
        self.assertIs(receipt, enqueue_receipts[0])
        self.assertEqual(1, len(playback_receipts))
        self.assertIs(playback, playback_receipts[0])
        self.assertTrue(playback.observed)
        self.assertEqual(receipt.item_count, playback.played_item_count)
        self.assertEqual("다이아 곡괭이 만드는 중이야", queued["text"])
        self.assertNotIn("Minecraft", queued["text"])
        self.assertNotIn("command_name", queued["text"])

    def test_non_preempting_payload_enqueues_once_without_speaking_badge(self):
        tts = self._tts()
        tts.latest_response_generation = 9
        receipts = []
        tts.add_lifecycle_response_enqueue_receipt_listener(receipts.append)
        payload = {
            "text": "다이아 곡괭이 다 만들었어",
            "response_generation": None,
            "event_id": "7" * 32,
            "source": "minecraft_chatclef",
            "route_kind": "crafting_lifecycle",
            "response_kind": "crafting_terminal",
            "delivery_mode": "non_preempting",
            "presentation": {
                "source_kind": "minecraft",
                "badge_label": "Minecraft",
            },
        }

        first = tts.receive_input(payload)
        duplicate = tts.receive_input(payload)

        self.assertTrue(first.accepted)
        self.assertFalse(duplicate.accepted)
        self.assertEqual("duplicate_event", duplicate.reason)
        self.assertEqual(2, len(receipts))
        self.assertEqual(1, len(tts.process_calls))
        queued = tts.input_queue.get_nowait()
        self.assertEqual("다이아 곡괭이 다 만들었어", queued["text"])
        self.assertNotIn("Minecraft", queued["text"])
        self.assertNotIn("presentation", queued)
        self.assertIsNone(queued["response_generation"])

    def test_stop_terminal_has_separate_non_preempting_receipt(self):
        tts = self._tts()
        payload = {
            "text": "멈췄어",
            "source": "minecraft_chatclef",
            "event_id": "7" * 32,
            "route_kind": "stop_control",
            "response_kind": "stop_terminal",
            "delivery_mode": "non_preempting",
            "response_generation": None,
        }

        receipt = tts.receive_input(payload)

        self.assertTrue(receipt.accepted)
        self.assertEqual("stop_control", receipt.route_kind)
        self.assertEqual("stop_terminal", receipt.response_kind)
        self.assertIsNone(receipt.response_generation)

    def test_coalesced_response_has_one_enqueue_and_one_playback_receipt(self):
        tts = self._tts()
        enqueue_receipts = []
        playback_receipts = []
        tts.add_lifecycle_response_enqueue_receipt_listener(
            enqueue_receipts.append
        )
        tts.add_lifecycle_response_playback_receipt_listener(
            playback_receipts.append
        )
        payload = {
            "text": "감마를 바꿨어",
            "response_generation": 12,
            "event_id": "8" * 32,
            "source": "minecraft_chatclef",
            "route_kind": "command_lifecycle",
            "response_kind": "command_coalesced",
            "delivery_mode": "current_input",
            "presentation": {
                "source_kind": "minecraft",
                "badge_label": "Minecraft",
                "log": (
                    '{"command_name":"gamma",'
                    '"form_kind":"explicit_value"}'
                ),
            },
        }

        receipt = tts.receive_input(payload)
        queued = tts.input_queue.get_nowait()
        playback = tts.observe_lifecycle_response_playback(
            event_id=queued["lifecycle_event_id"],
            route_kind=queued["lifecycle_route_kind"],
            response_kind=queued["lifecycle_response_kind"],
            delivery_token=queued["lifecycle_delivery_token"],
            item_index=queued["lifecycle_item_index"],
            played=True,
            reason="played",
        )

        self.assertTrue(receipt.accepted)
        self.assertEqual(1, len(enqueue_receipts))
        self.assertEqual("command_coalesced", enqueue_receipts[0].response_kind)
        self.assertEqual(1, len(playback_receipts))
        self.assertIs(playback, playback_receipts[0])
        self.assertTrue(playback.observed)
        self.assertEqual("command_coalesced", playback.response_kind)
        self.assertEqual(1, len(tts.process_calls))
        self.assertTrue(tts.input_queue.empty())
        self.assertNotIn("presentation", queued)
        self.assertNotIn("command_name", queued["text"])

    def test_terminal_with_current_input_mode_is_not_treated_as_lifecycle_receipt(self):
        tts = self._tts()
        payload = {
            "text": "완료했어",
            "source": "minecraft_chatclef",
            "event_id": "8" * 32,
            "route_kind": "command_lifecycle",
            "response_kind": "command_terminal",
            "delivery_mode": "current_input",
            "response_generation": 3,
        }

        self.assertIsNone(tts.get_lifecycle_response_identity(payload))

    def test_new_response_retires_dropped_lifecycle_item_as_stale(self):
        tts = self._tts()
        playback_receipts = []
        tts.add_lifecycle_response_playback_receipt_listener(
            playback_receipts.append
        )

        first = tts.receive_input(
            self._current_input_payload(event_id="a" * 32, generation=10)
        )
        second = tts.receive_input(
            self._current_input_payload(event_id="b" * 32, generation=11)
        )

        self.assertTrue(first.accepted)
        self.assertTrue(second.accepted)
        self.assertEqual(1, len(playback_receipts))
        dropped = playback_receipts[0]
        self.assertEqual("a" * 32, dropped.event_id)
        self.assertFalse(dropped.observed)
        self.assertEqual("stale", dropped.reason)
        self.assertIsNone(
            tts.observe_lifecycle_response_playback(
                event_id="a" * 32,
                route_kind="command_lifecycle",
                response_kind="command_start",
                item_index=0,
                played=False,
                reason="stale",
            )
        )

    def test_worker_stale_paths_retire_lifecycle_item_as_stale(self):
        for stale_kind in ("queue_generation", "response_generation"):
            with self.subTest(stale_kind=stale_kind):
                tts = self._tts()
                playback_receipts = []
                tts.add_lifecycle_response_playback_receipt_listener(
                    playback_receipts.append
                )
                receipt = tts.receive_input(
                    self._current_input_payload(
                        event_id="c" * 32,
                        generation=10,
                    )
                )
                self.assertTrue(receipt.accepted)
                if stale_kind == "queue_generation":
                    tts.bump_queue_generation()
                else:
                    tts.update_latest_response_generation(11)

                queued_item = TTSQueueWorker(tts).get_next_input_item(
                    tts.get_queue_generation()
                )

                self.assertIsNone(queued_item)
                self.assertEqual(1, len(playback_receipts))
                dropped = playback_receipts[0]
                self.assertEqual("c" * 32, dropped.event_id)
                self.assertFalse(dropped.observed)
                self.assertEqual("stale", dropped.reason)

    def test_interrupt_clear_retires_lifecycle_item_as_interrupted(self):
        tts = self._tts()
        playback_receipts = []
        tts.add_lifecycle_response_playback_receipt_listener(
            playback_receipts.append
        )
        receipt = tts.receive_input(
            self._current_input_payload(event_id="d" * 32, generation=10)
        )
        self.assertTrue(receipt.accepted)

        TTSInterruptController(tts).clear_queue(tts.input_queue)

        self.assertTrue(tts.input_queue.empty())
        self.assertEqual(1, len(playback_receipts))
        dropped = playback_receipts[0]
        self.assertEqual("d" * 32, dropped.event_id)
        self.assertFalse(dropped.observed)
        self.assertEqual("interrupted", dropped.reason)
        tts.input_queue.put((tts.get_queue_generation(), "legacy"))
        TTSInterruptController(tts).clear_queue(tts.input_queue)
        self.assertEqual(1, len(playback_receipts))

    def test_pending_playback_fact_is_notified_after_enqueue_commit(self):
        tts = self._tts()
        playback_receipts = []
        tts.add_lifecycle_response_playback_receipt_listener(
            playback_receipts.append
        )

        def enqueue(items, **identity):
            self.assertIsNone(
                tts.observe_lifecycle_response_playback(
                    event_id=identity["event_id"],
                    route_kind=identity["route_kind"],
                    response_kind=identity["response_kind"],
                    delivery_token=identity["delivery_token"],
                    item_index=0,
                    played=False,
                    reason="interrupted",
                )
            )
            return len(items)

        tts.enqueue_lifecycle_response_items = enqueue

        receipt = tts.receive_input(
            self._current_input_payload(event_id="e" * 32, generation=10)
        )

        self.assertTrue(receipt.accepted)
        self.assertEqual(1, len(playback_receipts))
        self.assertFalse(playback_receipts[0].observed)
        self.assertEqual("interrupted", playback_receipts[0].reason)

    def test_lifecycle_items_are_not_reclassified_during_enqueue(self):
        tts = self._tts()
        calls = []

        def changes_answer(_text):
            calls.append(1)
            return len(calls) > 1

        tts.text_processor.is_tts_skippable = changes_answer

        receipt = tts.receive_input(
            self._current_input_payload(event_id="f" * 32, generation=10)
        )

        self.assertTrue(receipt.accepted)
        self.assertEqual(1, receipt.item_count)
        self.assertEqual(1, len(calls))
        self.assertEqual(1, tts.input_queue.qsize())

    def test_queue_display_failure_does_not_relabel_successful_enqueue(self):
        tts = self._tts()
        tts.process_queue_live_textbox = _ThrowingLiveTextbox()

        receipt = tts.receive_input(
            self._current_input_payload(event_id="1" * 32, generation=10)
        )

        self.assertTrue(receipt.accepted)
        self.assertEqual(1, receipt.item_count)
        self.assertEqual(1, tts.input_queue.qsize())

    @staticmethod
    def _current_input_payload(*, event_id, generation):
        return {
            "text": "lifecycle response",
            "response_generation": generation,
            "event_id": event_id,
            "source": "minecraft_chatclef",
            "route_kind": "command_lifecycle",
            "response_kind": "command_start",
            "delivery_mode": "current_input",
        }

    @staticmethod
    def _tts():
        tts = TTS.__new__(TTS)
        tts.input_queue = Queue()
        tts.queue_lock = threading.Lock()
        tts.queue_generation_lock = threading.Lock()
        tts.queue_generation = 1
        tts.response_generation_lock = threading.Lock()
        tts.latest_response_generation = None
        tts.text_processor = _TextProcessor()
        tts.process_queue_live_textbox = _LiveTextbox()
        tts.lifecycle_response_delivery_adapter = (
            TtsLifecycleResponseDeliveryAdapter()
        )
        tts.lifecycle_response_enqueue_receipt_listeners = []
        tts.lifecycle_response_playback_receipt_listeners = []
        tts.current_plugin = SimpleNamespace(synthesize=lambda _text: b"audio")
        tts.prepare_input_items = lambda text: [text]
        tts.process_calls = []
        tts.process_input_queue = tts.process_calls.append
        return tts


class _TextProcessor:
    def is_tts_skippable(self, _text):
        return False


class _LiveTextbox:
    def set(self, _value):
        return None


class _ThrowingLiveTextbox:
    def set(self, _value):
        raise RuntimeError("display unavailable")


if __name__ == "__main__":
    unittest.main()
