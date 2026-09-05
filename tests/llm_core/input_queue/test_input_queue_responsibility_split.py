# 20260905_kpopmodder: Verifies queue commit and post-accept effects are isolated.
from __future__ import annotations

import unittest

from input_core.input_event.adapters import LocalChatInputEventAdapter
from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from llm_core.input_queue import (
    ClaimAwareLlmInputQueueCommitter,
    LlmInputQueueAcceptanceEffects,
    LlmInputQueueDrainer,
    LlmInputQueuePendingInputClearer,
    LlmInputQueueSubmissionCoordinator,
    LlmInputQueueThreadStarter,
)
from llm_core.input_queue_worker import LLMInputQueueWorker


class InputQueueResponsibilitySplitTests(unittest.TestCase):
    def test_claim_commit_has_no_ui_or_worker_start_effect(self):
        delivery = _registered_delivery("a" * 32)
        updates = []
        starts = []
        worker = _worker(updates)
        worker.process_input_queue = lambda: starts.append("started")
        committer = ClaimAwareLlmInputQueueCommitter(
            worker,
            log_callback=lambda _message: None,
        )
        effects = LlmInputQueueAcceptanceEffects(
            worker,
            log_callback=lambda _message: None,
        )

        receipt = committer.commit(delivery)

        self.assertIsNotNone(receipt)
        self.assertEqual([], updates)
        self.assertEqual([], starts)
        self.assertEqual(1, worker.input_queue.qsize())

        effects.run()
        self.assertEqual(["updated"], updates)
        self.assertEqual(["started"], starts)
        worker.clear_pending_inputs()

    def test_worker_and_runtime_are_compatibility_facades_over_focused_components(self):
        worker = _worker([])

        self.assertIsInstance(
            worker.claim_aware_input_queue_sink._committer,
            ClaimAwareLlmInputQueueCommitter,
        )
        self.assertIsInstance(
            worker.claim_aware_input_queue_sink._acceptance_effects,
            LlmInputQueueAcceptanceEffects,
        )
        self.assertIsInstance(
            worker._submission_coordinator,
            LlmInputQueueSubmissionCoordinator,
        )
        self.assertIsInstance(
            worker._pending_input_clearer,
            LlmInputQueuePendingInputClearer,
        )
        self.assertIsInstance(
            worker.queue_runtime._thread_starter,
            LlmInputQueueThreadStarter,
        )
        self.assertIsInstance(
            worker.queue_runtime._drainer,
            LlmInputQueueDrainer,
        )

    def test_commit_failure_log_does_not_include_payload_or_exception_text(self):
        delivery = _registered_delivery("b" * 32)
        logs = []
        worker = _worker([])

        class FailingQueue:
            def put(self, _value):
                raise RuntimeError("SECRET_ERROR")

        worker.input_queue = FailingQueue()
        committer = ClaimAwareLlmInputQueueCommitter(
            worker,
            log_callback=logs.append,
        )

        self.assertIsNone(committer.commit(delivery))
        self.assertEqual(1, len(logs))
        self.assertNotIn("SECRET_ERROR", logs[0])


def _worker(updates):
    return LLMInputQueueWorker(
        response_callback=lambda *_args: iter(()),
        history_callback=list,
        system_prompt_callback=str,
        queue_updated_callback=lambda: updates.append("updated"),
    )


def _registered_delivery(event_id):
    registry = TrustedUserInputIngressClaimRegistry()
    factory = TrustedIngressProducerRegistrarFactory(registry)
    adapter = LocalChatInputEventAdapter(event_id_factory=lambda: event_id)
    factory._bind_local_chat_input_event_adapter(adapter)
    return factory.begin_invocation(
        input_event_adapter=adapter,
        source_policy=adapter.policy,
    ).create_registered_delivery("private input")


if __name__ == "__main__":
    unittest.main()
