#20260706_kpopmodder: Added focused tests for LLM input queue runtime extraction.
import os
import sys
import threading
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from llm_core.input_queue_worker import LLMInputQueueWorker
from input_core.input_event.adapters import LocalChatInputEventAdapter
from input_core.input_event.provenance.trusted_user_ingress import (
    IngressClaimState,
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)


class LLMInputQueueRuntimeTests(unittest.TestCase):
    def test_claim_aware_queue_handoff_consumes_exact_entry_once(self):
        registry, delivery = self._registered_delivery("8" * 32)
        consumed = []
        updates = []

        def consume_queued(queue_delivery, _history, _system_prompt):
            lease = queue_delivery.accept_for_dispatch()
            evidence = lease.consume_for_eligibility()
            consumed.append(evidence)
            try:
                yield "handled"
            finally:
                evidence.close()
                lease.abandon()

        worker = LLMInputQueueWorker(
            response_callback=lambda *_args: iter(()),
            queued_response_callback=consume_queued,
            history_callback=lambda: [],
            system_prompt_callback=lambda: "system",
            queue_updated_callback=lambda: updates.append("updated"),
        )
        worker.process_input_queue = lambda: None

        receipt = worker.claim_aware_input_queue_sink.offer_registered(delivery)

        self.assertIsNotNone(receipt)
        self.assertEqual(IngressClaimState.QUEUE_OWNED, receipt.queue_delivery.state)
        self.assertEqual(1, worker.input_queue.qsize())
        worker.generate_response()
        self.assertEqual(1, len(consumed))
        self.assertTrue(consumed[0].closed)
        self.assertEqual(0, registry.live_count)
        self.assertEqual(["updated", "updated"], updates)

    def test_claim_aware_queue_put_failure_abandons_queue_owned_claim(self):
        registry, delivery = self._registered_delivery("9" * 32)
        worker = LLMInputQueueWorker(
            response_callback=lambda *_args: iter(()),
            history_callback=lambda: [],
            system_prompt_callback=lambda: "",
            queue_updated_callback=lambda: None,
        )

        class FailingQueue:
            def put(self, _value):
                raise RuntimeError("put failed")

        worker.input_queue = FailingQueue()
        receipt = worker.claim_aware_input_queue_sink.offer_registered(delivery)

        self.assertIsNone(receipt)
        self.assertEqual(0, registry.live_count)

    def test_post_accept_ui_and_worker_start_failures_keep_queue_ownership(self):
        registry, delivery = self._registered_delivery("a" * 32)
        worker = LLMInputQueueWorker(
            response_callback=lambda *_args: iter(()),
            history_callback=lambda: [],
            system_prompt_callback=lambda: "",
            queue_updated_callback=lambda: None,
        )
        worker.queue_updated_callback = lambda: (_ for _ in ()).throw(
            RuntimeError("ui failed")
        )
        worker.process_input_queue = lambda: (_ for _ in ()).throw(
            RuntimeError("start failed")
        )

        receipt = worker.claim_aware_input_queue_sink.offer_registered(delivery)

        self.assertIsNotNone(receipt)
        self.assertEqual(IngressClaimState.QUEUE_OWNED, receipt.queue_delivery.state)
        self.assertEqual(1, registry.live_count)
        worker.queue_updated_callback = lambda: None
        worker.clear_pending_inputs()
        self.assertEqual(0, registry.live_count)

    def test_clear_pending_inputs_abandons_only_removed_claimed_entries(self):
        registry, delivery = self._registered_delivery("b" * 32)
        worker = LLMInputQueueWorker(
            response_callback=lambda *_args: iter(()),
            history_callback=lambda: [],
            system_prompt_callback=lambda: "",
            queue_updated_callback=lambda: None,
        )
        worker.process_input_queue = lambda: None
        receipt = worker.claim_aware_input_queue_sink.offer_registered(delivery)
        worker.input_queue.put("plain")

        worker.clear_pending_inputs()

        self.assertIsNotNone(receipt)
        self.assertTrue(worker.input_queue.empty())
        self.assertEqual(0, registry.live_count)
        self.assertFalse(receipt.queue_delivery.abandon())

    def test_dequeue_and_clear_race_has_no_orphan_queue_entry_or_live_claim(self):
        registry, delivery = self._registered_delivery("c" * 32)
        routed = []

        def consume_queued(queue_delivery, _history, _system_prompt):
            lease = queue_delivery.accept_for_dispatch()
            if lease is None:
                return
            evidence = lease.consume_for_eligibility()
            if evidence is not None:
                routed.append(evidence.event)
                evidence.close()
            lease.abandon()
            yield from ()

        worker = LLMInputQueueWorker(
            response_callback=lambda *_args: iter(()),
            queued_response_callback=consume_queued,
            history_callback=lambda: [],
            system_prompt_callback=lambda: "",
            queue_updated_callback=lambda: None,
        )
        worker.process_input_queue = lambda: None
        receipt = worker.claim_aware_input_queue_sink.offer_registered(delivery)
        barrier = threading.Barrier(3)

        def drain():
            barrier.wait()
            worker.generate_response()

        def clear():
            barrier.wait()
            worker.clear_pending_inputs()

        threads = [threading.Thread(target=drain), threading.Thread(target=clear)]
        for thread in threads:
            thread.start()
        barrier.wait()
        for thread in threads:
            thread.join(timeout=2.0)

        self.assertIsNotNone(receipt)
        self.assertTrue(worker.input_queue.empty())
        self.assertLessEqual(len(routed), 1)
        self.assertEqual(0, registry.live_count)

    def test_queue_accept_and_producer_abandon_race_has_one_owner(self):
        registry, delivery = self._registered_delivery("d" * 32)
        worker = LLMInputQueueWorker(
            response_callback=lambda *_args: iter(()),
            history_callback=lambda: [],
            system_prompt_callback=lambda: "",
            queue_updated_callback=lambda: None,
        )
        worker.process_input_queue = lambda: None
        barrier = threading.Barrier(3)
        outcomes = []

        def offer():
            barrier.wait()
            outcomes.append(
                (
                    "offer",
                    worker.claim_aware_input_queue_sink.offer_registered(delivery),
                )
            )

        def abandon():
            barrier.wait()
            outcomes.append(("abandon", delivery.abandon()))

        threads = [threading.Thread(target=offer), threading.Thread(target=abandon)]
        for thread in threads:
            thread.start()
        barrier.wait()
        for thread in threads:
            thread.join(timeout=2.0)

        receipt = next(value for kind, value in outcomes if kind == "offer")
        abandoned = next(value for kind, value in outcomes if kind == "abandon")
        self.assertEqual(1, int(receipt is not None) + int(abandoned is True))
        if receipt is not None:
            worker.clear_pending_inputs()
        self.assertTrue(worker.input_queue.empty())
        self.assertEqual(0, registry.live_count)

    def test_generate_response_drains_queue_and_updates_ui(self):
        calls = []
        updates = []

        def response_callback(message, history, system_prompt):
            calls.append((message, history, system_prompt))
            yield "partial"
            yield "done"

        worker = LLMInputQueueWorker(
            response_callback=response_callback,
            history_callback=lambda: [["old", "answer"]],
            system_prompt_callback=lambda: "system",
            queue_updated_callback=lambda: updates.append("updated"),
        )
        worker.input_queue.put("hello")

        worker.generate_response()

        self.assertTrue(worker.input_queue.empty())
        self.assertEqual([("hello", [["old", "answer"]], "system")], calls)
        self.assertEqual(["updated"], updates)

    def test_process_input_queue_does_not_start_second_live_thread(self):
        worker = LLMInputQueueWorker(
            response_callback=lambda *_args: iter(()),
            history_callback=lambda: [],
            system_prompt_callback=lambda: "",
            queue_updated_callback=lambda: None,
        )

        class LiveThread:
            def is_alive(self):
                return True

        live_thread = LiveThread()
        worker.input_process_thread = live_thread

        worker.process_input_queue()

        self.assertIs(live_thread, worker.input_process_thread)

    def _registered_delivery(self, event_id):
        registry = TrustedUserInputIngressClaimRegistry()
        factory = TrustedIngressProducerRegistrarFactory(registry)
        adapter = LocalChatInputEventAdapter(
            event_id_factory=lambda: event_id,
        )
        factory._bind_local_chat_input_event_adapter(adapter)
        registrar = factory.begin_invocation(
            input_event_adapter=adapter,
            source_policy=adapter.policy,
        )
        return registry, registrar.create_registered_delivery("마이크 입력")


if __name__ == "__main__":
    unittest.main()
