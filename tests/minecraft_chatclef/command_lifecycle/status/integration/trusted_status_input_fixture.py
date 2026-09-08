#20260908_kpopmodder: Build STATUS source fixtures through real adapters, registry custody, and eligibility admission.
from __future__ import annotations

from types import SimpleNamespace

from input_core.input_event.adapters import (
    LocalChatInputEventAdapter,
    ProviderBoundInputEventAdapter,
)
from input_core.input_event.provenance import InputProviderSourceResolver
from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.input.eligibility import (
    KoreanChatMicrophoneEligibilityAdmission,
)


def issue_trusted_status_input(
    *,
    voice: bool,
    event_id: str,
    proof_owner: object,
    text: str = "지금 뭐 해?",
):
    registry = TrustedUserInputIngressClaimRegistry()
    producer_factory = TrustedIngressProducerRegistrarFactory(registry)
    if voice:
        adapter = ProviderBoundInputEventAdapter(
            provider=SimpleNamespace(
                handle=SimpleNamespace(
                    descriptor=SimpleNamespace(id="VoiceInput"),
                )
            ),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
            event_id_factory=lambda: event_id,
        )
        producer_factory._bind_voice_input_final_event_adapter(adapter)
    else:
        adapter = LocalChatInputEventAdapter(
            event_id_factory=lambda: event_id,
        )
        producer_factory._bind_local_chat_input_event_adapter(adapter)
    delivery = producer_factory.begin_invocation(
        input_event_adapter=adapter,
        source_policy=adapter.policy,
    ).create_registered_delivery(text)
    evidence = delivery.accept_for_dispatch().consume_for_eligibility()
    proof, reason = KoreanChatMicrophoneEligibilityAdmission(
        registry.validate_consumed_evidence
    ).issue(
        event=delivery.event,
        consumed_ingress_evidence=evidence,
        owner=proof_owner,
    )
    if proof is None or reason != "eligible":
        raise AssertionError(f"trusted STATUS proof was not issued: {reason}")
    return registry, delivery.event, proof


__all__ = ("issue_trusted_status_input",)
