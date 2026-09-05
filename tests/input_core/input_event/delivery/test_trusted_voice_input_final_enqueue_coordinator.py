#20260905_kpopmodder: Verifies VoiceInput-final exact queue acceptance ownership.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from input_core.input_event.adapters import ProviderBoundInputEventAdapter
from input_core.input_event.delivery import (
    TrustedVoiceInputFinalEnqueueCoordinator,
)
from input_core.input_event.provenance.trusted_user_ingress import (
    IngressClaimState,
    QueueAcceptanceReceipt,
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from llm_core.input_queue_worker import LLMInputQueueWorker


class TrustedVoiceInputFinalEnqueueCoordinatorTests(unittest.TestCase):
    def test_exact_voice_adapter_enqueues_once_then_isolates_later_observer_failure(self):
        registry, factory = self._ownership()
        callback_events = []
        adapter = self._voice_adapter(callback_events.append, "c" * 32)
        worker = self._worker()
        worker.process_input_queue = lambda: None
        observed = []

        def fail_observer(_event):
            raise RuntimeError("later listener failed")

        coordinator = TrustedVoiceInputFinalEnqueueCoordinator(
            input_event_adapter=adapter,
            producer_registrar_factory=factory,
            claim_aware_queue_sinks=(worker.claim_aware_input_queue_sink,),
            post_accept_observers=(fail_observer, observed.append),
            log_callback=lambda _message: None,
        )

        receipt = coordinator.enqueue("멈춰")

        self.assertIsNotNone(receipt)
        self.assertEqual(IngressClaimState.QUEUE_OWNED, receipt.queue_delivery.state)
        self.assertEqual("voice_input_final", receipt.event.source)
        self.assertEqual("VoiceInput", receipt.event.provider_id)
        self.assertEqual([], callback_events)
        self.assertEqual([receipt.event], observed)
        self.assertEqual(1, registry.live_count)
        worker.clear_pending_inputs()
        self.assertEqual(0, registry.live_count)

    def test_zero_or_two_designated_sinks_fail_closed_without_calling_them(self):
        for sink_count in (0, 2):
            with self.subTest(sink_count=sink_count):
                registry, factory = self._ownership()
                adapter = self._voice_adapter(lambda _event: None, "d" * 32)
                sinks = tuple(_RecordingSink() for _ in range(sink_count))
                coordinator = TrustedVoiceInputFinalEnqueueCoordinator(
                    input_event_adapter=adapter,
                    producer_registrar_factory=factory,
                    claim_aware_queue_sinks=sinks,
                )

                self.assertIsNone(coordinator.enqueue("한국어"))
                self.assertEqual(0, sum(sink.calls for sink in sinks))
                self.assertEqual(0, registry.live_count)

    def test_generic_true_return_is_not_a_queue_acceptance_receipt(self):
        registry, factory = self._ownership()
        adapter = self._voice_adapter(lambda _event: None, "e" * 32)
        coordinator = TrustedVoiceInputFinalEnqueueCoordinator(
            input_event_adapter=adapter,
            producer_registrar_factory=factory,
            claim_aware_queue_sinks=(_TrueReturningSink(),),
        )

        self.assertIsNone(coordinator.enqueue("한국어"))
        self.assertEqual(0, registry.live_count)

    def test_pre_sink_fanout_failure_abandons_registered_claim_without_offer(self):
        registry, factory = self._ownership()
        adapter = self._voice_adapter(lambda _event: None, "a" * 32)
        sink = _RecordingSink()
        coordinator = TrustedVoiceInputFinalEnqueueCoordinator(
            input_event_adapter=adapter,
            producer_registrar_factory=factory,
            claim_aware_queue_sinks=(sink,),
            pre_accept_observers=(
                lambda _event: (_ for _ in ()).throw(
                    RuntimeError("fanout failed")
                ),
            ),
            log_callback=lambda _message: None,
        )

        self.assertIsNone(coordinator.enqueue("한국어"))
        self.assertEqual(0, sink.calls)
        self.assertEqual(0, registry.live_count)

    def test_queue_failure_diagnostic_excludes_payload_and_exception_text(self):
        registry, factory = self._ownership()
        adapter = self._voice_adapter(lambda _event: None, "b" * 32)
        logs = []
        coordinator = TrustedVoiceInputFinalEnqueueCoordinator(
            input_event_adapter=adapter,
            producer_registrar_factory=factory,
            claim_aware_queue_sinks=(_RaisingSink(),),
            log_callback=logs.append,
        )

        self.assertIsNone(coordinator.enqueue("SECRET_TRANSCRIPT"))
        self.assertEqual(0, registry.live_count)
        self.assertEqual(1, len(logs))
        self.assertIn("boundary=queue_offer", logs[0])
        self.assertNotIn("SECRET_TRANSCRIPT", logs[0])
        self.assertNotIn("SECRET_QUEUE_ERROR", logs[0])

    def test_provider_adapter_call_remains_one_stamp_then_callback(self):
        callback_events = []
        adapter = self._voice_adapter(callback_events.append, "f" * 32)

        result = adapter("최종 음성")

        self.assertIsNone(result)
        self.assertEqual(1, len(callback_events))
        self.assertEqual("최종 음성", callback_events[0].text)
        self.assertEqual("f" * 32, callback_events[0].event_id)

    def test_factory_rejects_replacement_exact_voice_adapter_identity(self):
        _registry, factory = self._ownership()
        first = self._voice_adapter(lambda _event: None, "1" * 32)
        second = self._voice_adapter(lambda _event: None, "2" * 32)
        worker = self._worker()
        TrustedVoiceInputFinalEnqueueCoordinator(
            input_event_adapter=first,
            producer_registrar_factory=factory,
            claim_aware_queue_sinks=(worker.claim_aware_input_queue_sink,),
        )

        with self.assertRaisesRegex(RuntimeError, "identity is already bound"):
            TrustedVoiceInputFinalEnqueueCoordinator(
                input_event_adapter=second,
                producer_registrar_factory=factory,
                claim_aware_queue_sinks=(worker.claim_aware_input_queue_sink,),
            )

    def test_queue_receipt_cannot_be_caller_issued(self):
        with self.assertRaisesRegex(TypeError, "issued only by the queue sink"):
            QueueAcceptanceReceipt(
                queue_delivery=object(),
                queue_entry_token=object(),
            )

    def _ownership(self):
        registry = TrustedUserInputIngressClaimRegistry()
        return registry, TrustedIngressProducerRegistrarFactory(registry)

    def _voice_adapter(self, callback, event_id):
        descriptor = SimpleNamespace(id="VoiceInput")
        provider = SimpleNamespace(handle=SimpleNamespace(descriptor=descriptor))
        return ProviderBoundInputEventAdapter(
            provider=provider,
            output_callback=callback,
            event_id_factory=lambda: event_id,
        )

    def _worker(self):
        return LLMInputQueueWorker(
            response_callback=lambda *_args: iter(()),
            history_callback=lambda: [],
            system_prompt_callback=lambda: "",
            queue_updated_callback=lambda: None,
        )


class _RecordingSink:
    def __init__(self):
        self.calls = 0

    def offer_registered(self, _delivery):
        self.calls += 1
        return None


class _TrueReturningSink:
    def offer_registered(self, _delivery):
        return True


class _RaisingSink:
    def offer_registered(self, _delivery):
        raise RuntimeError("SECRET_QUEUE_ERROR")


if __name__ == "__main__":
    unittest.main()
