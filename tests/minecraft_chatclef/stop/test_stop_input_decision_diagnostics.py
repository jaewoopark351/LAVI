#20260905_kpopmodder: Verify canonical STOP decision logs exist only after common admission.
from __future__ import annotations

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
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_router import (
    MinecraftChatClefInputRouter,
)
from plugins.Minecraft.fabric.chatclef.input.stop import (
    StopInputDecisionFormatter,
    StopInputDecisionKind,
)


class StopInputDecisionDiagnosticsTests(unittest.TestCase):
    def test_common_admitted_tri_state_decisions_use_exact_canonical_fields(self):
        cases = (
            (
                "멈춰",
                StopInputDecisionKind.VALID_EXACT_STOP.value,
                "stop_bare",
                "exact_stop_phrase",
                False,
            ),
            (
                "멈춰?",
                StopInputDecisionKind.GUARDED_STOP_LIKE_REJECTION.value,
                "none",
                "unsafe_raw_character",
                True,
            ),
            (
                "오늘 날씨 알려줘",
                StopInputDecisionKind.UNRELATED.value,
                "none",
                "not_exact_whole_utterance",
                False,
            ),
        )
        for index, (text, decision, phrase_rule_id, reason, voice) in enumerate(cases):
            with self.subTest(decision=decision):
                event_id = f"{index + 1:032x}"
                logs, event = _route_trusted_input(
                    text,
                    event_id=event_id,
                    voice=voice,
                )
                messages = _stop_input_messages(logs)

                self.assertEqual(len(messages), 1)
                fields = _parse_stop_input_decision(messages[0])
                self.assertEqual(
                    tuple(fields),
                    StopInputDecisionFormatter.CANONICAL_FIELDS,
                )
                self.assertEqual(fields["event_id"], event.event_id)
                self.assertEqual(fields["source"], event.source)
                self.assertEqual(fields["provider_id"], event.provider_id)
                self.assertEqual(fields["event_kind"], event.event_kind)
                self.assertEqual(fields["final"], "true")
                self.assertEqual(fields["phrase_rule_id"], phrase_rule_id)
                self.assertEqual(fields["decision"], decision)
                self.assertEqual(fields["reason"], reason)
                self.assertNotIn(text, messages[0])

    def test_untrusted_and_non_korean_paths_emit_no_stop_decision(self):
        logs = []
        router = MinecraftChatClefInputRouter(log_callback=logs.append)
        untrusted = LaviInputEvent(
            text="멈춰",
            source="voice_input_partial",
            event_id="a" * 32,
            event_kind="partial_transcript",
            final=False,
            provider_id="VoiceInput",
            fallback_payload="멈춰",
        )

        router.route(untrusted)
        self.assertEqual(_stop_input_messages(logs), [])

        non_korean_logs, _event = _route_trusted_input(
            "stop ai",
            event_id="b" * 32,
        )
        self.assertEqual(_stop_input_messages(non_korean_logs), [])


def _route_trusted_input(text: str, *, event_id: str, voice: bool = False):
    ingress = TrustedUserInputIngressClaimRegistry()
    factory = TrustedIngressProducerRegistrarFactory(ingress)
    if voice:
        adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
            event_id_factory=lambda: event_id,
        )
        factory._bind_voice_input_final_event_adapter(adapter)
    else:
        adapter = LocalChatInputEventAdapter(event_id_factory=lambda: event_id)
        factory._bind_local_chat_input_event_adapter(adapter)
    delivery = factory.begin_invocation(
        input_event_adapter=adapter,
        source_policy=adapter.policy,
    ).create_registered_delivery(text)
    evidence = delivery.accept_for_dispatch().consume_for_eligibility()
    logs = []
    router = MinecraftChatClefInputRouter(
        korean_eligibility_admission=KoreanChatMicrophoneEligibilityAdmission(
            ingress.validate_consumed_evidence
        ),
        log_callback=logs.append,
    )
    router.route_trusted_user_input(delivery.event, evidence)
    return logs, delivery.event


def _voice_provider():
    descriptor = SimpleNamespace(id="VoiceInput")
    return SimpleNamespace(handle=SimpleNamespace(descriptor=descriptor))


def _stop_input_messages(logs):
    return [
        message
        for message in logs
        if type(message) is str
        and message.startswith("event=stop_input_decision ")
    ]


def _parse_stop_input_decision(message: str) -> dict[str, str]:
    parts = message.split(" ")
    if not parts or parts[0] != "event=stop_input_decision":
        raise AssertionError("not a STOP input-decision record")
    if len(parts) != len(StopInputDecisionFormatter.CANONICAL_FIELDS) + 1:
        raise AssertionError("STOP input-decision field count changed")
    fields = {}
    ordered_names = []
    for part in parts[1:]:
        if part.count("=") != 1:
            raise AssertionError("STOP input-decision field is malformed")
        name, value = part.split("=", 1)
        if not name or not value or name in fields:
            raise AssertionError("STOP input-decision field is duplicated or empty")
        ordered_names.append(name)
        fields[name] = value
    if tuple(ordered_names) != StopInputDecisionFormatter.CANONICAL_FIELDS:
        raise AssertionError("STOP input-decision canonical fields changed")
    return fields


if __name__ == "__main__":
    unittest.main()
