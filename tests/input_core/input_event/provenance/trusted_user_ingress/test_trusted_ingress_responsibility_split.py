#20260905_kpopmodder: Verifies trusted-ingress ownership stays split by responsibility.
from __future__ import annotations

import unittest

from input_core.input_event.adapters import LocalChatInputEventAdapter
from input_core.input_event.provenance.trusted_user_ingress import (
    ConsumedIngressEvidenceValidator,
    RoutedResponseEmissionCapabilityConsumer,
    TrustedIngressAdapterBindingRegistry,
    TrustedIngressClaimStateStore,
    TrustedIngressClaimTransitionCoordinator,
    TrustedIngressInvocationIssuer,
    TrustedIngressProducerRegistrarFactory,
    TrustedIngressRegistrationCoordinator,
    TrustedIngressRegistrationValidator,
    TrustedUserInputIngressClaimRegistry,
)
from input_core.input_event.provenance.trusted_user_ingress.trusted_ingress_callback_invocation_owner_registry import (
    TrustedIngressCallbackInvocationOwnerRegistry,
)


class TrustedIngressResponsibilitySplitTests(unittest.TestCase):
    def test_registry_facade_composes_focused_claim_collaborators(self):
        registry = TrustedUserInputIngressClaimRegistry()

        self.assertIsInstance(registry._state_store, TrustedIngressClaimStateStore)
        self.assertIs(registry._lock, registry._state_store.lock)
        self.assertIs(registry._records, registry._state_store.records)
        self.assertIsInstance(
            registry._registration_validator,
            TrustedIngressRegistrationValidator,
        )
        self.assertIsInstance(
            registry._registration_coordinator,
            TrustedIngressRegistrationCoordinator,
        )
        self.assertIsInstance(
            registry._transition_coordinator,
            TrustedIngressClaimTransitionCoordinator,
        )
        self.assertIsInstance(
            registry._consumed_evidence_validator,
            ConsumedIngressEvidenceValidator,
        )
        self.assertIsInstance(
            registry._response_emission_capability_consumer,
            RoutedResponseEmissionCapabilityConsumer,
        )

    def test_factory_facade_composes_binding_and_invocation_collaborators(self):
        registry = TrustedUserInputIngressClaimRegistry()
        factory = TrustedIngressProducerRegistrarFactory(registry)
        adapter = LocalChatInputEventAdapter(event_id_factory=lambda: "a" * 32)

        factory._bind_local_chat_input_event_adapter(adapter)
        registrar = factory.begin_invocation(
            input_event_adapter=adapter,
            source_policy=adapter.policy,
        )
        delivery = registrar.create_registered_delivery("private input")

        self.assertIsInstance(
            factory._adapter_binding_registry,
            TrustedIngressAdapterBindingRegistry,
        )
        self.assertIsInstance(
            factory._invocation_issuer,
            TrustedIngressInvocationIssuer,
        )
        self.assertIsInstance(
            factory._callback_invocation_owner_registry,
            TrustedIngressCallbackInvocationOwnerRegistry,
        )
        self.assertIs(factory._adapter_binding_lock, factory._adapter_binding_registry._lock)
        self.assertIsNotNone(delivery)
        self.assertTrue(delivery.abandon())


if __name__ == "__main__":
    unittest.main()
