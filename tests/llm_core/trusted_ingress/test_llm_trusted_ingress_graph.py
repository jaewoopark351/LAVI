# 20260905_kpopmodder: Verifies trusted-ingress graph ownership and lazy Chat binding.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from input_core.input_event.adapters import LocalChatInputEventAdapter
from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from llm_core.chat_input import TrustedLocalChatInputGraph
from llm_core.trusted_ingress import (
    LlmTrustedIngressGraph,
    TrustedIngressClaimGraph,
    TrustedIngressLeaseDispatchCoordinator,
    TrustedVoiceFinalEnqueueCoordinatorFactory,
)
from llm_core.trusted_ingress.runtime import (
    LlmTrustedIngressEvidenceValidator,
    LlmTrustedIngressLeaseFacade,
    LlmTrustedVoiceEnqueueFacade,
)
from llm_core.llm_component import LLM


class LlmTrustedIngressGraphTests(unittest.TestCase):
    def test_facade_adapter_and_factory_cannot_mint_outside_chat_callback(self):
        llm = LLM.__new__(LLM)
        llm.prediction_dispatch_coordinator = SimpleNamespace(
            predict=lambda *_args, **_kwargs: iter(())
        )
        adapter = llm._get_local_chat_input_adapter()
        factory = llm._get_trusted_ingress_producer_registrar_factory()

        with self.assertRaises(PermissionError):
            factory.begin_invocation(
                input_event_adapter=adapter,
                source_policy=adapter.policy,
            )

        output = list(
            llm._get_local_chat_prediction_entrypoint().predict(
                "한국어 입력",
                [],
                "system",
            )
        )
        self.assertEqual([], output)
        self.assertEqual(0, factory.registry.live_count)

    def test_graph_keeps_claim_dispatch_voice_and_chat_components_separate(self):
        graph = LlmTrustedIngressGraph(
            predict_callback=lambda *_args, **_kwargs: iter(()),
            claim_aware_queue_sink_callback=lambda: object(),
        )

        self.assertIsInstance(graph.claim_graph, TrustedIngressClaimGraph)
        self.assertIsInstance(
            graph.lease_dispatch_coordinator,
            TrustedIngressLeaseDispatchCoordinator,
        )
        self.assertIsInstance(
            graph.voice_enqueue_factory,
            TrustedVoiceFinalEnqueueCoordinatorFactory,
        )
        self.assertIsInstance(
            graph._evidence_validator,
            LlmTrustedIngressEvidenceValidator,
        )
        self.assertIsInstance(graph._lease_facade, LlmTrustedIngressLeaseFacade)
        self.assertIsInstance(
            graph._voice_enqueue_facade,
            LlmTrustedVoiceEnqueueFacade,
        )
        self.assertIsNone(graph._local_chat_graph)
        self.assertIsInstance(graph.local_chat_graph, TrustedLocalChatInputGraph)

    def test_registered_dispatch_does_not_rebind_an_external_chat_adapter(self):
        registry = TrustedUserInputIngressClaimRegistry()
        factory = TrustedIngressProducerRegistrarFactory(registry)
        adapter = LocalChatInputEventAdapter(event_id_factory=lambda: "c" * 32)
        factory._bind_local_chat_input_event_adapter(adapter)
        delivery = factory.begin_invocation(
            input_event_adapter=adapter,
            source_policy=adapter.policy,
        ).create_registered_delivery("private input")
        seen = []

        def predict(event, _history, _system_prompt, *, trusted_ingress_evidence):
            seen.append((event, trusted_ingress_evidence))
            yield "handled"

        graph = LlmTrustedIngressGraph(
            predict_callback=predict,
            claim_aware_queue_sink_callback=lambda: object(),
            registry=registry,
            producer_registrar_factory=factory,
        )

        output = list(graph.accept_registered(delivery, [], "system"))

        self.assertEqual(["handled"], output)
        self.assertEqual(1, len(seen))
        self.assertTrue(seen[0][1].closed)
        self.assertIsNone(graph._local_chat_graph)
        self.assertEqual(0, registry.live_count)


if __name__ == "__main__":
    unittest.main()
