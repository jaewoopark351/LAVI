#20260905_kpopmodder: Verifies follow-up trusted-ingress responsibility boundaries.
from __future__ import annotations

from input_core.input_event.adapters import LocalChatInputEventAdapter
from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from input_core.input_event.provenance.trusted_user_ingress.claim_lifecycle import (
    TrustedIngressClaimLifecycle,
)
from input_core.input_event.provenance.trusted_user_ingress.evidence import (
    ConsumedIngressEvidenceCloser,
    ConsumedIngressEvidenceLifecycle,
    ConsumedIngressResponseCapabilityIssuer,
    RoutedResponseCapabilityKeyRegistry,
)
from input_core.input_event.provenance.trusted_user_ingress.producer_binding import (
    TrustedIngressProducerBindingAuthority,
)
from input_core.input_event.provenance.trusted_user_ingress.producer_issuance import (
    TrustedIngressProducerRegistrarIssuer,
)
from input_core.input_event.provenance.trusted_user_ingress.response_capability import (
    TrustedConsumedIngressEvidenceAuthority,
    TrustedRoutedResponseCapabilityConsumer,
)


def test_claim_registry_facade_separates_claim_and_response_authorities():
    registry = TrustedUserInputIngressClaimRegistry()

    assert type(registry._claim_lifecycle) is TrustedIngressClaimLifecycle
    assert (
        type(registry._consumed_evidence_authority)
        is TrustedConsumedIngressEvidenceAuthority
    )
    assert (
        type(registry._trusted_response_capability_consumer)
        is TrustedRoutedResponseCapabilityConsumer
    )
    assert registry._state_store is registry._claim_lifecycle.state_store


def test_producer_factory_separates_binding_authority_from_registrar_issuance():
    registry = TrustedUserInputIngressClaimRegistry()
    factory = TrustedIngressProducerRegistrarFactory(registry)

    assert (
        type(factory._binding_authority)
        is TrustedIngressProducerBindingAuthority
    )
    assert (
        type(factory._registrar_issuer)
        is TrustedIngressProducerRegistrarIssuer
    )
    assert (
        factory._adapter_binding_registry
        is factory._binding_authority.adapter_binding_registry
    )


def test_consumed_evidence_separates_lifecycle_keys_issuer_and_close():
    registry = TrustedUserInputIngressClaimRegistry()
    factory = TrustedIngressProducerRegistrarFactory(registry)
    adapter = LocalChatInputEventAdapter(event_id_factory=lambda: "a" * 32)
    factory._bind_local_chat_input_event_adapter(adapter)
    delivery = factory.begin_invocation(
        input_event_adapter=adapter,
        source_policy=adapter.policy,
    ).create_registered_delivery("한국어 입력")
    evidence = delivery.accept_for_dispatch().consume_for_eligibility()

    assert type(evidence._components.lifecycle) is ConsumedIngressEvidenceLifecycle
    assert (
        type(evidence._components.capability_key_registry)
        is RoutedResponseCapabilityKeyRegistry
    )
    assert (
        type(evidence._components.response_capability_issuer)
        is ConsumedIngressResponseCapabilityIssuer
    )
    assert type(evidence._components.closer) is ConsumedIngressEvidenceCloser
    assert evidence.close() is True
    assert evidence.close() is False
