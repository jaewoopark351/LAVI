# 20260905_kpopmodder: Verify bounded feature admission at handled, replay, and fallthrough boundaries.
from __future__ import annotations

import unittest
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
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import (
    BridgeLifecycleState,
)
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.extension import (
    MinecraftFabricChatClefExtension,
)
from plugins.Minecraft.fabric.chatclef.input.diagnostics import (
    MinecraftKoreanFeatureAdmissionFormatter,
    MinecraftKoreanFeatureAdmissionLogger,
    MinecraftKoreanFeatureAdmissionRecord,
)
from plugins.Minecraft.fabric.chatclef.input.eligibility import (
    KoreanChatMicrophoneEligibilityAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_router import (
    MinecraftChatClefInputRouter,
)

from .canonical_diagnostic_parser import parse_canonical_diagnostic


class MinecraftKoreanFeatureAdmissionDiagnosticsTests(unittest.TestCase):
    def test_feature_b_and_replayed_evidence_emit_exact_bounded_boundaries(self):
        messages = []
        registry, event, evidence = _trusted_chat(
            text="오늘 기분 SECRET_TRANSCRIPT",
            event_id="a" * 32,
        )
        adapter = _RecordingAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        router = MinecraftChatClefInputRouter(
            extension=extension,
            korean_eligibility_admission=KoreanChatMicrophoneEligibilityAdmission(
                registry.validate_consumed_evidence
            ),
            feature_admission_logger=MinecraftKoreanFeatureAdmissionLogger(
                messages.append
            ),
        )

        first = router.route_trusted_user_input(event, evidence)
        second = router.route_trusted_user_input(event, evidence)

        self.assertFalse(first.handled)
        self.assertTrue(second.handled)
        self.assertEqual([], adapter.requests)
        self.assertEqual(2, len(messages))
        first_fields = _parse_feature(messages[0])
        replay_fields = _parse_feature(messages[1])
        self.assertEqual("none", first_fields["feature_scope"])
        self.assertEqual("fallthrough", first_fields["decision"])
        self.assertEqual("rejected", replay_fields["decision"])
        self.assertEqual("rejected", replay_fields["ingress_claim_status"])
        self.assertEqual("not_issued", replay_fields["eligibility_proof_status"])
        self.assertNotIn("SECRET_TRANSCRIPT", " ".join(messages))

    def test_exact_feature_b_success_records_policy_rule_and_source_without_text(self):
        messages = []
        registry, event, evidence = _trusted_chat(
            text="지도 만들어줘",
            event_id="b" * 32,
        )
        adapter = _RecordingAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        router = MinecraftChatClefInputRouter(
            extension=extension,
            korean_eligibility_admission=KoreanChatMicrophoneEligibilityAdmission(
                registry.validate_consumed_evidence
            ),
            feature_admission_logger=MinecraftKoreanFeatureAdmissionLogger(
                messages.append
            ),
        )

        decision = router.route_trusted_user_input(event, evidence)

        self.assertTrue(decision.handled)
        self.assertEqual(
            ["get map 1"], [request.command for request in adapter.requests]
        )
        fields = _parse_feature(messages[0])
        self.assertEqual("lavi_chat_ui", fields["source"])
        self.assertEqual("B", fields["feature_scope"])
        self.assertEqual("generic_crafting_defaults_v1", fields["feature_policy_id"])
        self.assertEqual("generic_empty_map", fields["phrase_rule_id"])
        self.assertEqual("activated", fields["feature_activation_status"])
        self.assertEqual("handled", fields["decision"])
        self.assertNotIn("지도", messages[0])

    def test_unrelated_korean_conversation_records_none_and_falls_through(self):
        messages = []
        registry, event, evidence = _trusted_chat(
            text="오늘 기분이 어때?",
            event_id="c" * 32,
        )
        router = MinecraftChatClefInputRouter(
            extension=None,
            korean_eligibility_admission=KoreanChatMicrophoneEligibilityAdmission(
                registry.validate_consumed_evidence
            ),
            feature_admission_logger=MinecraftKoreanFeatureAdmissionLogger(
                messages.append
            ),
        )

        decision = router.route_trusted_user_input(event, evidence)

        self.assertFalse(decision.handled)
        fields = _parse_feature(messages[0])
        self.assertEqual("none", fields["feature_scope"])
        self.assertEqual("not_activated", fields["feature_activation_status"])
        self.assertEqual("fallthrough", fields["decision"])
        self.assertEqual("no_minecraft_trigger", fields["reason"])
        self.assertNotIn("기분", messages[0])

    def test_voice_stop_records_feature_c_and_fixed_phrase_rule(self):
        messages = []
        registry, event, evidence = _trusted_voice(
            text="마크 AI 멈춰줘",
            event_id="d" * 32,
        )
        router = MinecraftChatClefInputRouter(
            extension=None,
            korean_eligibility_admission=KoreanChatMicrophoneEligibilityAdmission(
                registry.validate_consumed_evidence
            ),
            feature_admission_logger=MinecraftKoreanFeatureAdmissionLogger(
                messages.append
            ),
            log_callback=lambda _message: None,
        )

        decision = router.route_trusted_user_input(event, evidence)

        self.assertTrue(decision.handled)
        fields = _parse_feature(messages[0])
        self.assertEqual("voice_input_final", fields["source"])
        self.assertEqual("VoiceInput", fields["provider_id"])
        self.assertEqual("C", fields["feature_scope"])
        self.assertEqual("stop_control_v1", fields["feature_policy_id"])
        self.assertEqual("stop_mark_ai_request", fields["phrase_rule_id"])
        self.assertEqual("handled", fields["decision"])
        self.assertNotIn("멈춰", messages[0])

    def test_owned_item_rejection_records_feature_a_without_raw_item_text(self):
        messages = []
        registry, event, evidence = _trusted_chat(
            text="구리 검 하나 만들어",
            event_id="e" * 32,
        )
        extension = MinecraftFabricChatClefExtension(adapter=_RecordingAdapter())
        router = MinecraftChatClefInputRouter(
            extension=extension,
            korean_eligibility_admission=KoreanChatMicrophoneEligibilityAdmission(
                registry.validate_consumed_evidence
            ),
            feature_admission_logger=MinecraftKoreanFeatureAdmissionLogger(
                messages.append
            ),
            log_callback=lambda _message: None,
        )

        decision = router.route_trusted_user_input(event, evidence)

        self.assertTrue(decision.handled)
        fields = _parse_feature(messages[0])
        self.assertEqual("A", fields["feature_scope"])
        self.assertEqual("minecraft_command_feedback_v1", fields["feature_policy_id"])
        self.assertEqual(
            "handled_without_activation", fields["feature_activation_status"]
        )
        self.assertEqual("handled", fields["decision"])
        self.assertNotIn("구리", messages[0])

    def test_strict_parser_rejects_duplicate_or_reordered_fields(self):
        valid = "event=minecraft_korean_feature_admission " + " ".join(
            f"{name}=none"
            for name in MinecraftKoreanFeatureAdmissionFormatter.CANONICAL_FIELDS
        )
        parsed = _parse_feature(valid)
        self.assertEqual(
            set(MinecraftKoreanFeatureAdmissionFormatter.CANONICAL_FIELDS),
            set(parsed),
        )
        duplicate = valid + " event_id=again"
        with self.assertRaises(AssertionError):
            _parse_feature(duplicate)

    def test_formatter_static_value_policy_is_recursively_immutable(self):
        field_values = MinecraftKoreanFeatureAdmissionFormatter._FIELD_VALUES

        with self.assertRaises(TypeError):
            field_values["source"] = frozenset({"changed"})
        with self.assertRaises(AttributeError):
            field_values["source"].add("changed")

    def test_formatter_replaces_uncontrolled_source_and_reason_instead_of_logging_them(
        self,
    ):
        formatter = MinecraftKoreanFeatureAdmissionFormatter()
        message = formatter.format(
            MinecraftKoreanFeatureAdmissionRecord(
                event_id="f" * 32,
                source="SECRET_SOURCE_TOKEN",
                provider_id="SECRET_PROVIDER_TOKEN",
                event_kind="SECRET_TRANSCRIPT",
                final=True,
                ingress_claim_status="rejected",
                korean_eligible=True,
                eligibility_proof_status="not_issued",
                feature_scope="none",
                feature_policy_id="none",
                phrase_rule_id="none",
                feature_activation_status="not_attempted",
                decision="rejected",
                reason="SECRET_TRANSCRIPT",
            )
        )

        fields = _parse_feature(message)
        self.assertEqual("invalid", fields["source"])
        self.assertEqual("invalid", fields["provider_id"])
        self.assertEqual("invalid", fields["event_kind"])
        self.assertEqual("invalid", fields["reason"])
        self.assertNotIn("SECRET", message)


def _trusted_chat(*, text, event_id):
    registry = TrustedUserInputIngressClaimRegistry()
    adapter = LocalChatInputEventAdapter(event_id_factory=lambda: event_id)
    factory = TrustedIngressProducerRegistrarFactory(registry)
    factory._bind_local_chat_input_event_adapter(adapter)
    delivery = factory.begin_invocation(
        input_event_adapter=adapter,
        source_policy=adapter.policy,
    ).create_registered_delivery(text)
    evidence = delivery.accept_for_dispatch().consume_for_eligibility()
    return registry, delivery.event, evidence


def _trusted_voice(*, text, event_id):
    registry = TrustedUserInputIngressClaimRegistry()
    adapter = ProviderBoundInputEventAdapter(
        provider=_voice_provider(),
        output_callback=lambda _event: None,
        source_resolver=InputProviderSourceResolver(),
        event_id_factory=lambda: event_id,
    )
    factory = TrustedIngressProducerRegistrarFactory(registry)
    factory._bind_voice_input_final_event_adapter(adapter)
    delivery = factory.begin_invocation(
        input_event_adapter=adapter,
        source_policy=adapter.policy,
    ).create_registered_delivery(text)
    evidence = delivery.accept_for_dispatch().consume_for_eligibility()
    return registry, delivery.event, evidence


def _voice_provider():
    descriptor = SimpleNamespace(id="VoiceInput")
    return SimpleNamespace(handle=SimpleNamespace(descriptor=descriptor))


def _parse_feature(message):
    return parse_canonical_diagnostic(
        message,
        event_name=MinecraftKoreanFeatureAdmissionFormatter.EVENT_NAME,
        canonical_fields=(MinecraftKoreanFeatureAdmissionFormatter.CANONICAL_FIELDS),
    )


class _RecordingAdapter:
    backend_id = "fabric_chatclef"

    def __init__(self):
        self.requests = []

    def submit_command(self, request):
        self.requests.append(request)
        return CommandResultDTO(
            request_id=request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            error_code=None,
            message="accepted",
            data={"command": request.command},
        )

    def get_status(self):
        return StatusSnapshotDTO(
            backend_id=self.backend_id,
            enabled=True,
            connected=True,
            lifecycle_state=BridgeLifecycleState.CONNECTED,
            detail="connected",
            details={
                "commands": {
                    "active_request_id": None,
                    "active_command": None,
                    "last_result": None,
                }
            },
        )


if __name__ == "__main__":
    unittest.main()
