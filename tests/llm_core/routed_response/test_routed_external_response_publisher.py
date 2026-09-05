#20260905_kpopmodder: Verifies generation-safe routed output without LLM recursion.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from input_core.input_event.adapters import LocalChatInputEventAdapter
from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from llm_core.event_dispatcher import LLMEventDispatcher
from llm_core.llm_component import LLM
from llm_core.routed_response import RoutedExternalResponsePublisher


class RoutedExternalResponsePublisherTests(unittest.TestCase):
    def test_each_emit_allocates_one_fresh_generation_and_defaults_to_output_only(self):
        generations = []
        outputs = []
        full_outputs = []
        history = []

        def begin_generation():
            generations.append(len(generations) + 1)
            return generations[-1]

        publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=begin_generation,
            build_output_payload_callback=lambda text, generation: {
                "text": text,
                "response_generation": generation,
            },
            send_output_callback=outputs.append,
            send_full_output_callback=full_outputs.append,
            remember_history_callback=lambda *args: history.append(args),
        )

        first = publisher.emit_external_response("첫 응답")
        second = publisher.emit_external_response("둘째 응답")

        self.assertEqual([1, 2], generations)
        self.assertEqual([1, 2], [first.response_generation, second.response_generation])
        self.assertEqual(
            [
                {"text": "첫 응답", "response_generation": 1},
                {"text": "둘째 응답", "response_generation": 2},
            ],
            outputs,
        )
        self.assertEqual([], full_outputs)
        self.assertEqual([], history)
        self.assertTrue(first.output_delivered)
        self.assertFalse(first.full_output_delivered)
        self.assertFalse(first.history_remembered)

    def test_output_failure_is_isolated_from_the_completed_route_decision(self):
        publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=lambda: 7,
            build_output_payload_callback=lambda text, generation: {
                "text": text,
                "response_generation": generation,
            },
            send_output_callback=lambda _payload: (_ for _ in ()).throw(
                RuntimeError("listener failed")
            ),
            send_full_output_callback=lambda _text: None,
            log_callback=lambda _message: None,
        )

        emission = publisher.emit_external_response("이미 결정된 응답")

        self.assertEqual(7, emission.response_generation)
        self.assertFalse(emission.output_delivered)

    def test_capability_response_rejects_foreign_and_replay_without_generation(self):
        registry = TrustedUserInputIngressClaimRegistry()
        capability, evidence, event = _emission_capability(
            registry,
            event_id="3" * 32,
            text="권위 응답",
        )
        foreign_registry = TrustedUserInputIngressClaimRegistry()
        foreign_capability, foreign_evidence, foreign_event = _emission_capability(
            foreign_registry,
            event_id="4" * 32,
            text="권위 응답",
        )
        generations = []
        outputs = []
        publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=lambda: generations.append(
                len(generations) + 1
            ) or generations[-1],
            build_output_payload_callback=lambda text, generation: {
                "text": text,
                "response_generation": generation,
            },
            send_output_callback=outputs.append,
            send_full_output_callback=lambda _text: None,
            emission_capability_consumer=(
                registry.consume_routed_response_emission_capability
            ),
        )
        evidence.close()
        foreign_evidence.close()

        self.assertIsNone(
            publisher.emit_capability_response(
                "권위 응답",
                emission_capability=foreign_capability,
                event=foreign_event,
            )
        )
        self.assertIsNone(
            publisher.emit_capability_response(
                "권위 응답",
                emission_capability=capability,
                event=foreign_event,
            )
        )
        self.assertFalse(capability.spent)
        emission = publisher.emit_capability_response(
            "권위 응답",
            emission_capability=capability,
            event=event,
        )
        self.assertIsNotNone(emission)
        self.assertIsNone(
            publisher.emit_capability_response(
                "권위 응답",
                emission_capability=capability,
                event=event,
            )
        )
        self.assertEqual([1], generations)
        self.assertEqual(
            [{"text": "권위 응답", "response_generation": 1}],
            outputs,
        )

    def test_legacy_boolean_decision_cannot_authorize_routed_publication(self):
        llm = LLM.__new__(LLM)
        llm.response_pipeline = _GenerationPipeline()
        llm.event_dispatcher = LLMEventDispatcher()
        llm.input_router = _PublishingRouter()
        published = []
        full_published = []
        llm.add_output_event_listener(published.append)
        llm.add_output_event_listener(full_published.append, full_response=True)

        chat_output = list(llm.predict_wrapper("멈춰", [], "system"))
        for _ in llm.predict_wrapper("정지", [], "system"):
            pass

        self.assertEqual([], chat_output)
        self.assertEqual([], published)
        self.assertEqual([], full_published)
        self.assertEqual(0, llm.response_pipeline.provider_calls)
        self.assertEqual(0, llm.response_pipeline.response_generation)


class _GenerationPipeline:
    def __init__(self):
        self.response_generation = 0
        self.provider_calls = 0

    def begin_response_generation(self):
        self.response_generation += 1
        return self.response_generation

    def build_stream_payload(self, text, generation):
        return {"text": text, "response_generation": generation}

    def predict(self, *_args):
        self.provider_calls += 1
        raise AssertionError("handled routed responses must not call the provider")


class _PublishingRouter:
    def route(self, _event):
        return SimpleNamespace(
            handled=True,
            reason="minecraft_handled",
            response_text="[Minecraft] 멈춤 요청을 보냈어요.",
            publish_external_response=True,
            response_source="minecraft_chatclef",
        )


def _emission_capability(registry, *, event_id, text):
    adapter = LocalChatInputEventAdapter(
        event_id_factory=lambda: event_id,
    )
    factory = TrustedIngressProducerRegistrarFactory(registry)
    factory._bind_local_chat_input_event_adapter(adapter)
    delivery = factory.begin_invocation(
        input_event_adapter=adapter,
        source_policy=adapter.policy,
    ).create_registered_delivery("한국어")
    evidence = delivery.accept_for_dispatch().consume_for_eligibility()
    owner = object()
    if not evidence.claim_for_eligibility(delivery.event, owner):
        raise AssertionError("eligibility claim failed")
    capability = evidence.issue_routed_response_emission_capability(
        delivery.event,
        owner,
        text=text,
        source="minecraft_chatclef",
    )
    return capability, evidence, delivery.event


if __name__ == "__main__":
    unittest.main()
