#20260905_kpopmodder: Verifies trusted Chat handoff and dispatch-scoped evidence closure.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from input_core.input_event.adapters import LocalChatInputEventAdapter
from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from llm_core.llm_component import LLM


class TrustedLocalChatInputDispatchCoordinatorTests(unittest.TestCase):
    def test_llm_entrypoint_consumes_registered_claim_and_closes_evidence_on_close(self):
        llm = self._llm()
        router = _TrustedRouter()
        llm.input_router = router
        llm.local_chat_input_adapter = LocalChatInputEventAdapter(
            event_id_factory=lambda: "0" * 32,
        )

        stream = llm._get_local_chat_prediction_entrypoint().predict(
            "멈춰",
            [],
            "system",
        )
        self.assertEqual("accepted", next(stream))
        self.assertEqual(1, len(router.trusted_calls))
        event, evidence = router.trusted_calls[0]
        self.assertIs(event, evidence.event)
        self.assertTrue(evidence.is_live_for(event, router))
        self.assertEqual(
            0,
            llm._get_trusted_user_input_ingress_claim_registry().live_count,
        )

        stream.close()

        self.assertTrue(evidence.closed)
        self.assertEqual(0, router.legacy_calls)

    def test_registration_capacity_failure_preserves_legacy_dispatch_without_evidence(self):
        llm = self._llm()
        registry = TrustedUserInputIngressClaimRegistry(capacity=0)
        llm.trusted_user_input_ingress_claim_registry = registry
        llm.trusted_ingress_producer_registrar_factory = (
            TrustedIngressProducerRegistrarFactory(registry)
        )
        llm.local_chat_input_adapter = LocalChatInputEventAdapter(
            event_id_factory=lambda: "1" * 32,
        )
        router = _TrustedRouter()
        llm.input_router = router

        output = list(
            llm._get_local_chat_prediction_entrypoint().predict(
                "일반 대화",
                [],
                "system",
            )
        )

        self.assertEqual(["legacy"], output)
        self.assertEqual(0, len(router.trusted_calls))
        self.assertEqual(1, router.legacy_calls)
        self.assertEqual(0, registry.live_count)

    def test_trusted_route_exception_suppresses_llm_and_still_closes_evidence(self):
        llm = self._llm()
        router = _FailingTrustedRouter()
        llm.input_router = router
        llm.local_chat_input_adapter = LocalChatInputEventAdapter(
            event_id_factory=lambda: "2" * 32,
        )

        output = list(
            llm._get_local_chat_prediction_entrypoint().predict(
                "일상 대화",
                [],
                "system",
            )
        )

        self.assertEqual([], output)
        self.assertEqual(1, router.side_effect_count)
        self.assertEqual(0, llm.response_pipeline.call_count)
        self.assertTrue(router.evidence.closed)
        self.assertEqual(
            0,
            llm._get_trusted_user_input_ingress_claim_registry().live_count,
        )

    def _llm(self):
        llm = LLM.__new__(LLM)
        llm.input_router = None
        llm.response_pipeline = _Pipeline()
        llm.build_effective_system_prompt = lambda value: value
        return llm


class _TrustedRouter:
    def __init__(self):
        self.trusted_calls = []
        self.legacy_calls = 0

    def route_trusted_user_input(self, event, evidence):
        self.trusted_calls.append((event, evidence))
        if not evidence.claim_for_eligibility(event, self):
            raise AssertionError("evidence must have one eligibility owner")
        return SimpleNamespace(
            handled=True,
            reason="trusted",
            response_text="accepted",
        )

    def route(self, _event):
        self.legacy_calls += 1
        return SimpleNamespace(
            handled=True,
            reason="legacy",
            response_text="legacy",
        )


class _FailingTrustedRouter:
    def __init__(self):
        self.evidence = None
        self.side_effect_count = 0

    def route_trusted_user_input(self, _event, evidence):
        self.evidence = evidence
        self.side_effect_count += 1
        raise RuntimeError("route failed")

    def route(self, _event):
        raise AssertionError("trusted failure must not call the legacy router again")


class _Pipeline:
    def __init__(self):
        self.call_count = 0

    def predict(self, _message, _history, _system_prompt):
        self.call_count += 1
        yield "pipeline"


if __name__ == "__main__":
    unittest.main()
