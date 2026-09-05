#20260905_kpopmodder: Verifies bounded factory-bound trusted-ingress ownership.
from __future__ import annotations

import copy
import pickle
import threading
import unittest

from input_core.input_event.adapters import LocalChatInputEventAdapter
from input_core.input_event.provenance.trusted_user_ingress import (
    ConsumedIngressEvidence,
    IngressClaimState,
    RoutedResponseEmissionCapability,
    TrustedIngressProducerRegistrar,
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)


class TrustedUserInputIngressClaimRegistryTests(unittest.TestCase):
    def setUp(self):
        self._bound_factories = {}
        self.registry = TrustedUserInputIngressClaimRegistry()
        self.factory = self._new_bound_factory(self.registry)
        self.policy = self._bound_factories[self.factory][0].policy

    def test_factory_registrar_is_one_shot_and_no_public_event_register_exists(self):
        adapter = self._adapter("1" * 32)
        registrar = self._registrar(adapter)

        delivery = registrar.create_registered_delivery("안녕")

        self.assertIsNotNone(delivery)
        self.assertFalse(hasattr(self.registry, "register"))
        self.assertIsNone(registrar.create_registered_delivery("복제"))
        self.assertEqual(
            "trusted_ingress_producer_capability_spent",
            registrar.last_rejection_reason,
        )
        self.assertEqual("REGISTERED", registrar.creation_ticket.outcome)
        self.assertEqual(IngressClaimState.REGISTERED, delivery.state)
        self.assertTrue(delivery.abandon())
        self.assertEqual(IngressClaimState.ABANDONED, delivery.state)
        self.assertEqual(0, self.registry.live_count)

        with self.assertRaises(TypeError):
            copy.copy(registrar.creation_ticket)
        with self.assertRaises(TypeError):
            pickle.dumps(delivery)

    def test_registered_dispatch_consume_is_exact_object_cas_and_closes_live_slot(self):
        delivery = self._registrar(
            self._adapter("2" * 32)
        ).create_registered_delivery("마크 안녕")

        lease = delivery.accept_for_dispatch()
        self.assertIsNotNone(lease)
        self.assertEqual(IngressClaimState.DISPATCH_OWNED, lease.state)
        self.assertIsNone(delivery.accept_for_dispatch())
        self.assertFalse(delivery.abandon())

        evidence = lease.consume_for_eligibility()
        self.assertIsNotNone(evidence)
        self.assertEqual(IngressClaimState.CONSUMED, lease.state)
        self.assertIs(delivery.event, evidence.event)
        self.assertEqual(0, self.registry.live_count)
        self.assertIsNone(lease.consume_for_eligibility())
        owner = object()
        self.assertTrue(evidence.claim_for_eligibility(delivery.event, owner))
        self.assertFalse(evidence.claim_for_eligibility(delivery.event, object()))
        self.assertTrue(evidence.is_live_for(delivery.event, owner))
        self.assertTrue(evidence.close())
        self.assertFalse(evidence.is_live_for(delivery.event, owner))
        with self.assertRaises(TypeError):
            pickle.dumps(evidence)

    def test_consumed_evidence_cannot_be_caller_issued(self):
        with self.assertRaisesRegex(TypeError, "issued only by the claim registry"):
            ConsumedIngressEvidence(
                event=object(),
                event_signature=(),
                registry_token=object(),
            )
        with self.assertRaisesRegex(TypeError, "issued only by its factory"):
            TrustedIngressProducerRegistrar(
                adapter=object(),
                registry=object(),
                ticket=object(),
                capability=object(),
            )

    def test_factory_rejects_fake_or_shadowed_adapter_before_adaptation(self):
        fake = _FakeAdapter(self.policy)
        with self.assertRaisesRegex(TypeError, "type is not trusted"):
            self.factory.begin_invocation(
                input_event_adapter=fake,
                source_policy=fake.policy,
            )
        self.assertEqual(0, fake.calls)

        alternative = LocalChatInputEventAdapter(
            event_id_factory=lambda: "8" * 32,
        )
        with self.assertRaisesRegex(RuntimeError, "identity is already bound"):
            self.factory._bind_local_chat_input_event_adapter(alternative)
        with self.assertRaisesRegex(ValueError, "not the bound trusted instance"):
            self.factory.begin_invocation(
                input_event_adapter=alternative,
                source_policy=alternative.policy,
            )

        adapter = self._adapter("8" * 32)
        adapter.adapt = lambda _payload: object()
        with self.assertRaisesRegex(TypeError, "binding is not trusted"):
            self.factory.begin_invocation(
                input_event_adapter=adapter,
                source_policy=adapter.policy,
            )

    def test_authoritative_registry_rejects_foreign_consumed_evidence(self):
        local_delivery = self._registrar(
            self._adapter("9" * 32)
        ).create_registered_delivery("한국어")
        local_evidence = (
            local_delivery.accept_for_dispatch().consume_for_eligibility()
        )
        foreign_registry = TrustedUserInputIngressClaimRegistry()
        foreign_factory = self._new_bound_factory(foreign_registry)
        foreign_delivery = self._delivery(foreign_factory, "a" * 32)
        foreign_evidence = (
            foreign_delivery.accept_for_dispatch().consume_for_eligibility()
        )

        self.assertTrue(
            self.registry.validate_consumed_evidence(
                local_delivery.event,
                local_evidence,
            )
        )
        self.assertFalse(
            self.registry.validate_consumed_evidence(
                foreign_delivery.event,
                foreign_evidence,
            )
        )
        local_evidence.close()
        foreign_evidence.close()

    def test_response_capability_is_owner_bound_registry_bound_and_one_shot(self):
        delivery = self._registrar(
            self._adapter("c" * 32)
        ).create_registered_delivery("응답")
        evidence = delivery.accept_for_dispatch().consume_for_eligibility()
        owner = object()
        self.assertTrue(evidence.claim_for_eligibility(delivery.event, owner))
        capability = evidence.issue_routed_response_emission_capability(
            delivery.event,
            owner,
            text="[Minecraft] 응답",
            source="minecraft_chatclef",
        )

        self.assertIsNotNone(capability)
        self.assertIsNone(
            evidence.issue_routed_response_emission_capability(
                delivery.event,
                owner,
                text="[Minecraft] 응답",
                source="minecraft_chatclef",
            )
        )
        self.assertFalse(
            TrustedUserInputIngressClaimRegistry().consume_routed_response_emission_capability(
                capability,
                event=delivery.event,
                text="[Minecraft] 응답",
                source="minecraft_chatclef",
            )
        )
        self.assertFalse(capability.spent)
        self.assertFalse(
            self.registry.consume_routed_response_emission_capability(
                capability,
                event=delivery.event,
                text="다른 응답",
                source="minecraft_chatclef",
            )
        )
        self.assertTrue(
            self.registry.consume_routed_response_emission_capability(
                capability,
                event=delivery.event,
                text="[Minecraft] 응답",
                source="minecraft_chatclef",
            )
        )
        self.assertFalse(
            self.registry.consume_routed_response_emission_capability(
                capability,
                event=delivery.event,
                text="[Minecraft] 응답",
                source="minecraft_chatclef",
            )
        )
        with self.assertRaises(TypeError):
            pickle.dumps(capability)
        with self.assertRaisesRegex(TypeError, "issued only by trusted ingress"):
            RoutedResponseEmissionCapability(
                registry_token=object(),
                event=object(),
                event_signature=(),
                text="forged",
                source="minecraft_chatclef",
                response_kind="immediate",
            )
        evidence.close()

    def test_concurrent_use_of_one_producer_capability_has_one_registration_winner(self):
        registrar = self._registrar(self._adapter("a" * 32))
        barrier = threading.Barrier(3)
        outcomes = []

        def register():
            barrier.wait()
            outcomes.append(registrar.create_registered_delivery("동시 호출"))

        threads = [threading.Thread(target=register) for _ in range(2)]
        for thread in threads:
            thread.start()
        barrier.wait()
        for thread in threads:
            thread.join(timeout=2.0)

        winners = [delivery for delivery in outcomes if delivery is not None]
        self.assertEqual(1, len(winners))
        self.assertEqual(1, self.registry.live_count)
        winners[0].abandon()
        self.assertEqual(0, self.registry.live_count)

    def test_concurrent_distinct_invocations_with_one_event_id_have_one_winner(self):
        registrars = [
            self._registrar(self._adapter("b" * 32))
            for _ in range(2)
        ]
        barrier = threading.Barrier(3)
        outcomes = []

        def register(registrar):
            barrier.wait()
            outcomes.append(registrar.create_registered_delivery("동일 ID"))

        threads = [
            threading.Thread(target=register, args=(registrar,))
            for registrar in registrars
        ]
        for thread in threads:
            thread.start()
        barrier.wait()
        for thread in threads:
            thread.join(timeout=2.0)

        winners = [delivery for delivery in outcomes if delivery is not None]
        self.assertEqual(1, len(winners))
        self.assertEqual(1, self.registry.live_count)
        winners[0].abandon()
        self.assertEqual(0, self.registry.live_count)

    def test_dispatch_accept_and_registered_abandon_race_has_one_terminal_owner(self):
        delivery = self._registrar(
            self._adapter("3" * 32)
        ).create_registered_delivery("경쟁")
        barrier = threading.Barrier(3)
        outcomes = []

        def accept():
            barrier.wait()
            lease = delivery.accept_for_dispatch()
            outcomes.append(("accept", lease))
            if lease is not None:
                lease.abandon()

        def abandon():
            barrier.wait()
            outcomes.append(("abandon", delivery.abandon()))

        threads = [threading.Thread(target=accept), threading.Thread(target=abandon)]
        for thread in threads:
            thread.start()
        barrier.wait()
        for thread in threads:
            thread.join(timeout=2.0)

        accepted = next(value for kind, value in outcomes if kind == "accept")
        abandoned = next(value for kind, value in outcomes if kind == "abandon")
        self.assertEqual(1, int(accepted is not None) + int(abandoned is True))
        self.assertEqual(0, self.registry.live_count)

    def test_capacity_is_concurrent_live_only_and_sequential_use_does_not_accumulate(self):
        registry = TrustedUserInputIngressClaimRegistry(capacity=2)
        factory = self._new_bound_factory(registry)
        first = self._delivery(factory, "4" * 32)
        second = self._delivery(factory, "5" * 32)
        overflow_registrar = factory.begin_invocation(
            input_event_adapter=(
                overflow_adapter := self._factory_adapter(factory, "6" * 32)
            ),
            source_policy=overflow_adapter.policy,
        )
        self.assertIsNone(
            overflow_registrar.create_registered_delivery("overflow")
        )
        self.assertEqual(
            "trusted_ingress_capacity_exhausted",
            overflow_registrar.last_rejection_reason,
        )
        self.assertEqual(2, registry.live_count)
        first.abandon()
        replacement = self._delivery(factory, "7" * 32)
        self.assertIsNotNone(replacement)
        second.abandon()
        replacement.abandon()

        for index in range(4097):
            delivery = self._delivery(factory, f"{index + 32:032x}")
            lease = delivery.accept_for_dispatch()
            evidence = lease.consume_for_eligibility()
            evidence.close()
        self.assertEqual(0, registry.live_count)

    def test_default_capacity_accepts_4096_live_claims_without_eviction(self):
        deliveries = []
        for index in range(self.registry.CAPACITY):
            delivery = self._registrar(
                self._adapter(f"{index:032x}")
            ).create_registered_delivery("동시 요청")
            self.assertIsNotNone(delivery)
            deliveries.append(delivery)
        self.assertEqual(self.registry.CAPACITY, self.registry.live_count)

        overflow = self._registrar(
            self._adapter(f"{self.registry.CAPACITY:032x}")
        )
        self.assertIsNone(overflow.create_registered_delivery("초과 요청"))
        self.assertEqual(
            "trusted_ingress_capacity_exhausted",
            overflow.last_rejection_reason,
        )
        self.assertEqual(self.registry.CAPACITY, self.registry.live_count)
        for delivery in deliveries:
            delivery.abandon()
        self.assertEqual(0, self.registry.live_count)

    def test_event_id_requires_exact_builtin_lowercase_hex(self):
        class EventId(str):
            pass

        invalid_ids = (
            "A" * 32,
            "a" * 31,
            "g" * 32,
            EventId("a" * 32),
        )
        for index, event_id in enumerate(invalid_ids):
            with self.subTest(index=index):
                registrar = self._registrar(self._adapter(event_id))
                self.assertIsNone(
                    registrar.create_registered_delivery("한국어")
                )
                self.assertEqual(0, self.registry.live_count)

    def _adapter(self, event_id):
        return self._factory_adapter(self.factory, event_id)

    def _registrar(self, adapter):
        return self.factory.begin_invocation(
            input_event_adapter=adapter,
            source_policy=adapter.policy,
        )

    def _delivery(self, factory, event_id):
        adapter = self._factory_adapter(factory, event_id)
        return factory.begin_invocation(
            input_event_adapter=adapter,
            source_policy=adapter.policy,
        ).create_registered_delivery("한국어")

    def _new_bound_factory(self, registry):
        event_id_holder = ["0" * 32]
        adapter = LocalChatInputEventAdapter(
            event_id_factory=lambda: event_id_holder[0],
        )
        factory = TrustedIngressProducerRegistrarFactory(registry)
        factory._bind_local_chat_input_event_adapter(adapter)
        self._bound_factories[factory] = (adapter, event_id_holder)
        return factory

    def _factory_adapter(self, factory, event_id):
        adapter, event_id_holder = self._bound_factories[factory]
        event_id_holder[0] = event_id
        return adapter


class _FakeAdapter:
    def __init__(self, policy):
        self.policy = policy
        self.calls = 0

    def adapt(self, _payload):
        self.calls += 1
        return object()


if __name__ == "__main__":
    unittest.main()
