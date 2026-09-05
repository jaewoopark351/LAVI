#20260905_kpopmodder: Verify trusted consumption, authoritative ownership, and Hangul are all required.
from __future__ import annotations

import pickle
import unittest
from types import SimpleNamespace

from input_core.input_event.adapters import (
    LocalChatInputEventAdapter,
    ProviderBoundInputEventAdapter,
)
from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from input_core.input_event.provenance import InputProviderSourceResolver
from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.input.eligibility import (
    KoreanChatMicrophoneEligibilityAdmission,
)


def _trusted_consumption(*, text="지도 만들어줘", voice=False, event_id="b" * 32):
    registry = TrustedUserInputIngressClaimRegistry()
    factory = TrustedIngressProducerRegistrarFactory(registry)
    if voice:
        adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
            event_id_factory=lambda: event_id,
        )
        factory._bind_voice_input_final_event_adapter(adapter)
    else:
        adapter = LocalChatInputEventAdapter(
            event_id_factory=lambda: event_id,
        )
        factory._bind_local_chat_input_event_adapter(adapter)
    delivery = factory.begin_invocation(
        input_event_adapter=adapter,
        source_policy=adapter.policy,
    ).create_registered_delivery(text)
    evidence = delivery.accept_for_dispatch().consume_for_eligibility()
    return registry, delivery.event, evidence


def _voice_provider():
    descriptor = SimpleNamespace(id="VoiceInput")
    return SimpleNamespace(handle=SimpleNamespace(descriptor=descriptor))


def _untrusted_event(*, source, provider, kind, final):
    return LaviInputEvent(
        text="지도 만들어줘",
        source=source,
        event_id="c" * 32,
        event_kind=kind,
        final=final,
        provider_id=provider,
        fallback_payload="지도 만들어줘",
    )


class KoreanChatMicrophoneEligibilityTests(unittest.TestCase):
    def test_exact_chat_and_voice_final_issue_one_live_proof(self):
        for voice in (False, True):
            with self.subTest(voice=voice):
                registry, event, evidence = _trusted_consumption(voice=voice)
                owner = object()
                proof, reason = KoreanChatMicrophoneEligibilityAdmission(
                    registry.validate_consumed_evidence
                ).issue(
                    event=event,
                    consumed_ingress_evidence=evidence,
                    owner=owner,
                )
                self.assertEqual("eligible", reason)
                self.assertTrue(proof.matches_event(event, owner))
                self.assertRaises(TypeError, pickle.dumps, proof)
                self.assertTrue(proof.close())
                self.assertFalse(proof.matches_event(event, owner))

    def test_non_korean_partial_external_foreign_and_replay_fail_closed(self):
        registry, event, evidence = _trusted_consumption(text="get map 1")
        admission = KoreanChatMicrophoneEligibilityAdmission(
            registry.validate_consumed_evidence
        )
        proof, reason = admission.issue(
            event=event,
            consumed_ingress_evidence=evidence,
            owner=object(),
        )
        self.assertIsNone(proof)
        self.assertEqual("input_language_not_korean", reason)
        evidence.close()

        for event in (
            _untrusted_event(
                source="voice_input_partial",
                provider="VoiceInput",
                kind="partial_transcript",
                final=False,
            ),
            _untrusted_event(
                source="twitch_chat",
                provider="Twitch",
                kind="external_chat",
                final=True,
            ),
        ):
            with self.subTest(source=event.source):
                registry, _canonical_event, evidence = _trusted_consumption()
                proof, _reason = KoreanChatMicrophoneEligibilityAdmission(
                    registry.validate_consumed_evidence
                ).issue(
                    event=event,
                    consumed_ingress_evidence=evidence,
                    owner=object(),
                )
                self.assertIsNone(proof)
                evidence.close()

        registry, event, evidence = _trusted_consumption(event_id="d" * 32)
        owner = object()
        admission = KoreanChatMicrophoneEligibilityAdmission(
            registry.validate_consumed_evidence
        )
        proof, _reason = admission.issue(
            event=event,
            consumed_ingress_evidence=evidence,
            owner=owner,
        )
        replay, reason = admission.issue(
            event=event,
            consumed_ingress_evidence=evidence,
            owner=owner,
        )
        self.assertIsNone(replay)
        self.assertEqual("consumed_ingress_evidence_spent", reason)
        proof.close()

        foreign_registry = TrustedUserInputIngressClaimRegistry()
        _registry, event, evidence = _trusted_consumption(event_id="e" * 32)
        proof, reason = KoreanChatMicrophoneEligibilityAdmission(
            foreign_registry.validate_consumed_evidence
        ).issue(
            event=event,
            consumed_ingress_evidence=evidence,
            owner=object(),
        )
        self.assertIsNone(proof)
        self.assertEqual("consumed_ingress_evidence_foreign", reason)
        evidence.close()

    def test_missing_authoritative_validator_never_issues_proof(self):
        _registry, event, evidence = _trusted_consumption(event_id="f" * 32)
        proof, reason = KoreanChatMicrophoneEligibilityAdmission().issue(
            event=event,
            consumed_ingress_evidence=evidence,
            owner=object(),
        )
        self.assertIsNone(proof)
        self.assertEqual("consumed_ingress_authority_unavailable", reason)
        evidence.close()


if __name__ == "__main__":
    unittest.main()
