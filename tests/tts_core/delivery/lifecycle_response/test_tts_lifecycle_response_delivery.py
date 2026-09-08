#20260907_kpopmodder: Verify lifecycle TTS enqueue/playback receipts and replay suppression.
from __future__ import annotations

import threading
import unittest

from tts_core.delivery.lifecycle_response import (
    TtsLifecycleResponseDeliveryAdapter,
    TtsLifecycleResponseEnqueueReceipt,
    TtsLifecycleResponseEventDeduplicator,
    TtsLifecycleResponsePlaybackReceipt,
)


class TtsLifecycleResponseDeliveryTests(unittest.TestCase):
    def test_enqueue_and_split_playback_are_distinct_facts(self):
        queued = []
        adapter = TtsLifecycleResponseDeliveryAdapter()

        def enqueue(items, **identity):
            queued.append((items, identity))
            return len(items)

        enqueue_receipt = adapter.enqueue(
            event_id="d" * 32,
            route_kind="command_lifecycle",
            response_kind="command_terminal",
            items=("첫 문장", "둘째 문장"),
            enqueue_callback=enqueue,
        )

        self.assertIsInstance(
            enqueue_receipt,
            TtsLifecycleResponseEnqueueReceipt,
        )
        self.assertTrue(enqueue_receipt.accepted)
        self.assertEqual(2, enqueue_receipt.item_count)
        self.assertIsNone(
            adapter.observe_playback(
                event_id="d" * 32,
                item_index=0,
                played=True,
                reason="played",
            )
        )
        playback_receipt = adapter.observe_playback(
            event_id="d" * 32,
            item_index=1,
            played=True,
            reason="played",
        )
        self.assertIsInstance(
            playback_receipt,
            TtsLifecycleResponsePlaybackReceipt,
        )
        self.assertTrue(playback_receipt.observed)
        self.assertEqual(2, playback_receipt.played_item_count)
        self.assertNotEqual(type(enqueue_receipt), type(playback_receipt))

    def test_duplicate_event_is_not_enqueued_twice(self):
        adapter = TtsLifecycleResponseDeliveryAdapter()
        calls = []

        def enqueue(items, **_identity):
            calls.append(items)
            return len(items)

        first = adapter.enqueue(
            event_id="e" * 32,
            route_kind="crafting_lifecycle",
            response_kind="crafting_terminal",
            items=("완료",),
            enqueue_callback=enqueue,
        )
        duplicate = adapter.enqueue(
            event_id="e" * 32,
            route_kind="crafting_lifecycle",
            response_kind="crafting_terminal",
            items=("완료",),
            enqueue_callback=enqueue,
        )

        self.assertTrue(first.accepted)
        self.assertFalse(duplicate.accepted)
        self.assertEqual("duplicate_event", duplicate.reason)
        self.assertEqual(1, len(calls))

    def test_same_command_event_allows_distinct_start_and_terminal_phases(self):
        adapter = TtsLifecycleResponseDeliveryAdapter()
        calls = []

        def enqueue(items, **identity):
            calls.append((items, identity))
            return len(items)

        start = adapter.enqueue(
            event_id="1" * 32,
            route_kind="command_lifecycle",
            response_kind="command_start",
            items=("시작",),
            enqueue_callback=enqueue,
        )
        terminal = adapter.enqueue(
            event_id="1" * 32,
            route_kind="command_lifecycle",
            response_kind="command_terminal",
            items=("완료",),
            enqueue_callback=enqueue,
            delivery_mode="non_preempting",
        )

        self.assertTrue(start.accepted)
        self.assertTrue(terminal.accepted)
        self.assertEqual(2, len(calls))

    def test_playback_identity_distinguishes_phases_with_same_event_id(self):
        adapter = TtsLifecycleResponseDeliveryAdapter()
        for response_kind in ("command_start", "command_terminal"):
            adapter.enqueue(
                event_id="2" * 32,
                route_kind="command_lifecycle",
                response_kind=response_kind,
                items=(response_kind,),
                enqueue_callback=lambda items, **_identity: len(items),
            )

        self.assertIsNone(
            adapter.observe_playback(
                event_id="2" * 32,
                item_index=0,
                played=True,
                reason="played",
            )
        )
        terminal = adapter.observe_playback(
            event_id="2" * 32,
            route_kind="command_lifecycle",
            response_kind="command_terminal",
            item_index=0,
            played=True,
            reason="played",
        )

        self.assertTrue(terminal.observed)
        self.assertEqual("command_terminal", terminal.response_kind)

    def test_failed_playback_never_reports_observed(self):
        adapter = TtsLifecycleResponseDeliveryAdapter()
        adapter.enqueue(
            event_id="f" * 32,
            route_kind="crafting_lifecycle",
            response_kind="crafting_terminal",
            items=("완료",),
            enqueue_callback=lambda items, **_identity: len(items),
        )

        receipt = adapter.observe_playback(
            event_id="f" * 32,
            item_index=0,
            played=False,
            reason="playback_failed",
        )

        self.assertFalse(receipt.observed)
        self.assertEqual("playback_failed", receipt.reason)

    def test_enqueue_callback_cannot_publish_playback_before_count_commit(self):
        adapter = TtsLifecycleResponseDeliveryAdapter()
        synchronous_receipts = []

        def enqueue(items, **identity):
            synchronous_receipts.append(
                adapter.observe_playback(
                    event_id=identity["event_id"],
                    route_kind=identity["route_kind"],
                    response_kind=identity["response_kind"],
                    item_index=0,
                    played=True,
                    reason="played",
                )
            )
            return len(items)

        enqueue_receipt = adapter.enqueue(
            event_id="3" * 32,
            route_kind="command_lifecycle",
            response_kind="command_start",
            items=("start",),
            enqueue_callback=enqueue,
        )

        self.assertTrue(enqueue_receipt.accepted)
        self.assertEqual(1, len(synchronous_receipts))
        self.assertIsNone(synchronous_receipts[0])
        deferred = adapter.drain_deferred_playback_receipts()
        self.assertEqual(1, len(deferred))
        playback_receipt = deferred[0]
        self.assertTrue(playback_receipt.observed)
        self.assertIsNone(
            adapter.observe_playback(
                event_id="3" * 32,
                route_kind="command_lifecycle",
                response_kind="command_start",
                item_index=0,
                played=True,
                reason="played",
            )
        )

    def test_enqueue_callback_failures_roll_back_playback_reservation(self):
        def raise_after_reservation(_items, **_identity):
            raise RuntimeError("enqueue failed")

        def return_wrong_count(_items, **_identity):
            return 0

        for event_id, callback in (
            ("4" * 32, raise_after_reservation),
            ("5" * 32, return_wrong_count),
        ):
            with self.subTest(event_id=event_id):
                adapter = TtsLifecycleResponseDeliveryAdapter()

                receipt = adapter.enqueue(
                    event_id=event_id,
                    route_kind="command_lifecycle",
                    response_kind="command_start",
                    items=("start",),
                    enqueue_callback=callback,
                )

                self.assertFalse(receipt.accepted)
                self.assertEqual("delivery_failed", receipt.reason)
                self.assertIsNone(
                    adapter.observe_playback(
                        event_id=event_id,
                        route_kind="command_lifecycle",
                        response_kind="command_start",
                        item_index=0,
                        played=False,
                        reason="delivery_failed",
                    )
                )

    def test_synchronous_callback_fact_is_discarded_when_enqueue_fails(self):
        for event_id, failure_kind in (
            ("6" * 32, "raise"),
            ("7" * 32, "wrong_count"),
        ):
            with self.subTest(failure_kind=failure_kind):
                adapter = TtsLifecycleResponseDeliveryAdapter()
                synchronous_receipts = []

                def callback(items, **identity):
                    synchronous_receipts.append(
                        adapter.observe_playback(
                            event_id=identity["event_id"],
                            route_kind=identity["route_kind"],
                            response_kind=identity["response_kind"],
                            item_index=0,
                            played=False,
                            reason="playback_failed",
                        )
                    )
                    if failure_kind == "raise":
                        raise RuntimeError("failed after exposure")
                    return len(items) - 1

                receipt = adapter.enqueue(
                    event_id=event_id,
                    route_kind="command_lifecycle",
                    response_kind="command_start",
                    items=("start",),
                    enqueue_callback=callback,
                )

                self.assertFalse(receipt.accepted)
                self.assertEqual("delivery_failed", receipt.reason)
                self.assertEqual([None], synchronous_receipts)
                self.assertEqual(
                    (),
                    adapter.drain_deferred_playback_receipts(),
                )

    def test_pending_interrupt_fact_commits_after_successful_enqueue(self):
        adapter = TtsLifecycleResponseDeliveryAdapter()

        def enqueue(items, **identity):
            self.assertIsNone(
                adapter.observe_playback(
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

        receipt = adapter.enqueue(
            event_id="0" * 32,
            route_kind="command_lifecycle",
            response_kind="command_start",
            items=("start",),
            enqueue_callback=enqueue,
        )
        deferred = adapter.drain_deferred_playback_receipts()

        self.assertTrue(receipt.accepted)
        self.assertEqual(1, len(deferred))
        self.assertFalse(deferred[0].observed)
        self.assertEqual("interrupted", deferred[0].reason)

    def test_lru_eviction_cannot_overwrite_still_active_identity(self):
        adapter = TtsLifecycleResponseDeliveryAdapter(
            deduplicator=TtsLifecycleResponseEventDeduplicator(capacity=2)
        )
        def enqueue(items, **_identity):
            return len(items)
        first_id = "8" * 32
        adapter.enqueue(
            event_id=first_id,
            route_kind="command_lifecycle",
            response_kind="command_start",
            response_generation=10,
            items=("old",),
            enqueue_callback=enqueue,
        )
        adapter.enqueue(
            event_id="9" * 32,
            route_kind="command_lifecycle",
            response_kind="command_start",
            items=("completed",),
            enqueue_callback=enqueue,
        )
        adapter.observe_playback(
            event_id="9" * 32,
            route_kind="command_lifecycle",
            response_kind="command_start",
            item_index=0,
            played=True,
            reason="played",
        )
        adapter.enqueue(
            event_id="a" * 32,
            route_kind="command_lifecycle",
            response_kind="command_start",
            items=("new",),
            enqueue_callback=enqueue,
        )

        replay = adapter.enqueue(
            event_id=first_id,
            route_kind="command_lifecycle",
            response_kind="command_start",
            response_generation=99,
            items=("replacement",),
            enqueue_callback=enqueue,
        )
        old_playback = adapter.observe_playback(
            event_id=first_id,
            route_kind="command_lifecycle",
            response_kind="command_start",
            item_index=0,
            played=True,
            reason="played",
        )

        self.assertFalse(replay.accepted)
        self.assertEqual("duplicate_event", replay.reason)
        self.assertEqual(10, old_playback.response_generation)
        self.assertEqual(1, old_playback.item_count)

    def test_active_playback_states_are_bounded_by_dedupe_capacity(self):
        adapter = TtsLifecycleResponseDeliveryAdapter(
            deduplicator=TtsLifecycleResponseEventDeduplicator(capacity=2)
        )
        calls = []

        def enqueue(items, **_identity):
            calls.append(items)
            return len(items)

        for event_id in ("b" * 32, "c" * 32):
            self.assertTrue(
                adapter.enqueue(
                    event_id=event_id,
                    route_kind="command_lifecycle",
                    response_kind="command_start",
                    items=(event_id,),
                    enqueue_callback=enqueue,
                ).accepted
            )
        overflow = adapter.enqueue(
            event_id="d" * 32,
            route_kind="command_lifecycle",
            response_kind="command_start",
            items=("overflow",),
            enqueue_callback=enqueue,
        )

        self.assertFalse(overflow.accepted)
        self.assertEqual("delivery_failed", overflow.reason)
        self.assertEqual(2, len(calls))

    def test_stale_delivery_token_cannot_complete_reused_identity(self):
        adapter = TtsLifecycleResponseDeliveryAdapter(
            deduplicator=TtsLifecycleResponseEventDeduplicator(capacity=1)
        )
        delivery_tokens = []

        def enqueue(items, **identity):
            delivery_tokens.append(identity["delivery_token"])
            return len(items)

        values = {
            "event_id": "d" * 32,
            "route_kind": "command_lifecycle",
            "response_kind": "command_start",
            "items": ("start",),
            "enqueue_callback": enqueue,
        }
        self.assertTrue(adapter.enqueue(**values).accepted)
        adapter.clear()
        self.assertTrue(adapter.enqueue(**values).accepted)

        stale = adapter.observe_playback(
            event_id=values["event_id"],
            route_kind=values["route_kind"],
            response_kind=values["response_kind"],
            delivery_token=delivery_tokens[0],
            item_index=0,
            played=True,
            reason="played",
        )
        current = adapter.observe_playback(
            event_id=values["event_id"],
            route_kind=values["route_kind"],
            response_kind=values["response_kind"],
            delivery_token=delivery_tokens[1],
            item_index=0,
            played=True,
            reason="played",
        )

        self.assertNotEqual(delivery_tokens[0], delivery_tokens[1])
        self.assertIsNone(stale)
        self.assertTrue(current.observed)

    def test_clear_during_callback_invalidates_pending_reservation(self):
        adapter = TtsLifecycleResponseDeliveryAdapter()
        callback_started = threading.Event()
        callback_release = threading.Event()
        receipts = []

        def enqueue(items, **_identity):
            callback_started.set()
            self.assertTrue(callback_release.wait(1.0))
            return len(items)

        thread = threading.Thread(
            target=lambda: receipts.append(
                adapter.enqueue(
                    event_id="e" * 32,
                    route_kind="command_lifecycle",
                    response_kind="command_start",
                    items=("start",),
                    enqueue_callback=enqueue,
                )
            )
        )
        thread.start()
        self.assertTrue(callback_started.wait(1.0))

        adapter.clear()
        callback_release.set()
        thread.join(timeout=1.0)

        self.assertFalse(thread.is_alive())
        self.assertEqual(1, len(receipts))
        self.assertFalse(receipts[0].accepted)
        self.assertEqual("delivery_failed", receipts[0].reason)

    def test_concurrent_same_identity_reserves_once_without_overwrite(self):
        adapter = TtsLifecycleResponseDeliveryAdapter()
        callback_started = threading.Event()
        callback_release = threading.Event()
        callback_tokens = []
        receipts = []

        def enqueue(items, **identity):
            callback_tokens.append(identity["delivery_token"])
            callback_started.set()
            self.assertTrue(callback_release.wait(1.0))
            return len(items)

        values = {
            "event_id": "f" * 32,
            "route_kind": "command_lifecycle",
            "response_kind": "command_start",
            "items": ("start",),
            "enqueue_callback": enqueue,
        }
        first = threading.Thread(
            target=lambda: receipts.append(adapter.enqueue(**values))
        )
        first.start()
        self.assertTrue(callback_started.wait(1.0))
        second = threading.Thread(
            target=lambda: receipts.append(adapter.enqueue(**values))
        )
        second.start()
        second.join(timeout=1.0)
        callback_release.set()
        first.join(timeout=1.0)

        self.assertFalse(first.is_alive())
        self.assertFalse(second.is_alive())
        self.assertEqual(1, sum(receipt.accepted for receipt in receipts))
        self.assertEqual(
            1,
            sum(receipt.reason == "duplicate_event" for receipt in receipts),
        )
        self.assertEqual(1, len(callback_tokens))

    def test_interrupted_and_stale_failures_release_state_at_capacity(self):
        adapter = TtsLifecycleResponseDeliveryAdapter(
            deduplicator=TtsLifecycleResponseEventDeduplicator(capacity=2)
        )
        def enqueue(items, **_identity):
            return len(items)

        for index, reason in enumerate(("interrupted", "stale") * 4):
            event_id = f"{index:032x}"
            receipt = adapter.enqueue(
                event_id=event_id,
                route_kind="command_lifecycle",
                response_kind="command_start",
                items=(reason,),
                enqueue_callback=enqueue,
            )
            self.assertTrue(receipt.accepted)
            playback = adapter.observe_playback(
                event_id=event_id,
                route_kind="command_lifecycle",
                response_kind="command_start",
                item_index=0,
                played=False,
                reason=reason,
            )
            self.assertFalse(playback.observed)


if __name__ == "__main__":
    unittest.main()
