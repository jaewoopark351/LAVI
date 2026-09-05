#20260905_kpopmodder: Verifies Korean proof lifecycle and issuance separation.
from __future__ import annotations

from input_core.input_event.adapters import LocalChatInputEventAdapter
from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.input.eligibility import (
    KoreanChatMicrophoneEligibilityAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.eligibility.proof import (
    KoreanChatMicrophoneProofLifecycle,
    KoreanChatMicrophoneResponseCapabilityIssuer,
)


def test_eligibility_proof_delegates_lifecycle_and_response_issuance():
    registry = TrustedUserInputIngressClaimRegistry()
    factory = TrustedIngressProducerRegistrarFactory(registry)
    adapter = LocalChatInputEventAdapter(event_id_factory=lambda: "c" * 32)
    factory._bind_local_chat_input_event_adapter(adapter)
    delivery = factory.begin_invocation(
        input_event_adapter=adapter,
        source_policy=adapter.policy,
    ).create_registered_delivery("지도 만들어줘")
    event = delivery.event
    evidence = delivery.accept_for_dispatch().consume_for_eligibility()
    owner = object()
    proof, reason = KoreanChatMicrophoneEligibilityAdmission(
        registry.validate_consumed_evidence
    ).issue(
        event=event,
        consumed_ingress_evidence=evidence,
        owner=owner,
    )
    assert reason == "eligible"
    assert type(proof._components.lifecycle) is KoreanChatMicrophoneProofLifecycle
    assert (
        type(proof._components.response_capability_issuer)
        is KoreanChatMicrophoneResponseCapabilityIssuer
    )
    capability = proof.issue_response_emission_capability(
        event,
        owner,
        text="지도를 준비하도록 명령했어요.",
        source="minecraft_chatclef",
    )
    assert capability is not None
    assert proof.close() is True
    assert (
        proof.issue_response_emission_capability(
            event,
            owner,
            text="다시 보낼 수 없어요.",
            source="minecraft_chatclef",
        )
        is None
    )
