# 20260905_kpopmodder: Verify canonical per-sink feedback delivery and replay evidence.
from __future__ import annotations

import unittest

from types import SimpleNamespace

from input_core.input_event.adapters import (
    LocalChatInputEventAdapter,
    ProviderBoundInputEventAdapter,
)
from input_core.input_event.normalization import LaviInputEventNormalizer
from input_core.input_event.provenance import InputProviderSourceResolver
from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from app_core.composition_core.component_wiring import (
    MinecraftStopTerminalResponseWiring,
)
from llm_core.event_dispatcher import LLMEventDispatcher
from llm_core.llm_component import LLM
from llm_core.routed_response import (
    CommandFeedbackDeliveryFormatter,
    CommandFeedbackDeliveryLogger,
    RoutedExternalResponsePublisher,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.response.stop import (
    StopControlTerminalResponse,
)


class CommandFeedbackDeliveryDiagnosticsTests(unittest.TestCase):
    def test_authorized_delivery_and_replay_have_one_generation_and_exact_fields(self):
        logs = []
        outputs = []
        generations = []
        registry, event, capability, evidence = _capability(
            event_id="d" * 32,
            event_source="lavi_chat_ui",
            response_text="[Minecraft] 지도 1개를 준비하도록 명령했어요.",
        )
        publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=lambda: (
                generations.append(len(generations) + 1) or generations[-1]
            ),
            build_output_payload_callback=lambda text, generation: {
                "text": text,
                "response_generation": generation,
            },
            send_output_callback=outputs.append,
            send_full_output_callback=lambda _text: None,
            emission_capability_consumer=(
                registry.consume_routed_response_emission_capability
            ),
            delivery_logger=CommandFeedbackDeliveryLogger(logs.append),
        )
        evidence.close()

        first = publisher.emit_capability_response(
            "[Minecraft] 지도 1개를 준비하도록 명령했어요.",
            emission_capability=capability,
            event=event,
            route_kind="generic_crafting_defaults",
        )
        replay = publisher.emit_capability_response(
            "[Minecraft] 지도 1개를 준비하도록 명령했어요.",
            emission_capability=capability,
            event=event,
            route_kind="generic_crafting_defaults",
        )

        self.assertIsNotNone(first)
        self.assertIsNone(replay)
        self.assertEqual([1], generations)
        self.assertEqual(1, len(outputs))
        self.assertEqual(2, len(logs))
        delivered = _parse_delivery(logs[0])
        rejected = _parse_delivery(logs[1])
        self.assertEqual("output_listener", delivered["sink"])
        self.assertEqual("1", delivered["response_generation"])
        self.assertEqual("true", delivered["delivered"])
        self.assertEqual("delivered", delivered["reason"])
        self.assertEqual("none", rejected["response_generation"])
        self.assertEqual("false", rejected["delivered"])
        self.assertEqual("authorization_rejected", rejected["reason"])
        self.assertNotIn("지도", " ".join(logs))
        self.assertNotIn("Minecraft", " ".join(logs))

    def test_chat_has_output_and_chat_ui_sinks_while_voice_has_output_only(self):
        chat_logs, chat_yields = _run_llm_feedback(
            event_id="e" * 32,
            event_source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_kind="chat_submit",
        )
        voice_logs, voice_yields = _run_llm_feedback(
            event_id="f" * 32,
            event_source="voice_input_final",
            provider_id="VoiceInput",
            event_kind="final_transcript",
        )

        self.assertEqual(["[Minecraft] 응답"], chat_yields)
        self.assertEqual(["[Minecraft] 응답"], voice_yields)
        self.assertEqual(
            ["output_listener", "chat_ui"],
            [_parse_delivery(message)["sink"] for message in chat_logs],
        )
        self.assertEqual(
            ["output_listener"],
            [_parse_delivery(message)["sink"] for message in voice_logs],
        )
        chat_fields = [_parse_delivery(message) for message in chat_logs]
        self.assertEqual(
            {"1"},
            {fields["response_generation"] for fields in chat_fields},
        )
        self.assertTrue(all(fields["delivered"] == "true" for fields in chat_fields))

    def test_delivery_failure_is_bounded_and_does_not_log_response_or_error_text(self):
        logs = []
        publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=lambda: 9,
            build_output_payload_callback=lambda _text, _generation: {
                "secret": "SECRET_RESPONSE"
            },
            send_output_callback=lambda _payload: (_ for _ in ()).throw(
                RuntimeError("SECRET_ERROR")
            ),
            send_full_output_callback=lambda _text: None,
            delivery_logger=CommandFeedbackDeliveryLogger(logs.append),
            log_callback=lambda _message: None,
        )

        emission = publisher.emit_external_response(
            "SECRET_RESPONSE",
            event_id="1" * 32,
            route_kind="minecraft_command",
            response_kind="immediate",
        )

        self.assertFalse(emission.output_delivered)
        fields = _parse_delivery(logs[0])
        self.assertEqual("false", fields["delivered"])
        self.assertEqual("delivery_failed", fields["reason"])
        self.assertNotIn("SECRET_RESPONSE", logs[0])
        self.assertNotIn("SECRET_ERROR", logs[0])

    def test_verified_stop_terminal_keeps_event_identity_and_emits_canonical_delivery(
        self,
    ):
        logs = []
        outputs = []
        pipeline = _Pipeline()
        llm = LLM.__new__(LLM)
        llm.response_pipeline = pipeline
        llm.event_dispatcher = LLMEventDispatcher()
        llm.event_dispatcher.add_output_event_listener(outputs.append)
        llm.routed_external_response_publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=pipeline.begin_response_generation,
            build_output_payload_callback=pipeline.build_stream_payload,
            send_output_callback=llm.send_output,
            send_full_output_callback=llm.send_full_output,
            delivery_logger=CommandFeedbackDeliveryLogger(logs.append),
        )
        extension = _TerminalExtension()
        callback = MinecraftStopTerminalResponseWiring().wire(
            llm=llm,
            extension=extension,
        )

        response = StopControlTerminalResponse(
            text="[Minecraft] 마크 AI를 멈췄어요.",
            event_id="2" * 32,
        )
        callback(response)

        self.assertIs(callback, extension.callback)
        self.assertEqual(
            [{"text": response.text, "response_generation": 1}],
            outputs,
        )
        fields = _parse_delivery(logs[0])
        self.assertEqual("2" * 32, fields["event_id"])
        self.assertEqual("stop_control", fields["route_kind"])
        self.assertEqual("stop_terminal", fields["response_kind"])
        self.assertEqual("output_listener", fields["sink"])
        self.assertEqual("true", fields["delivered"])
        self.assertNotIn("멈췄어요", logs[0])

    def test_formatter_replaces_uncontrolled_delivery_metadata(self):
        logs = []
        logger = CommandFeedbackDeliveryLogger(logs.append)

        logger.log(
            event_id="SECRET_EVENT_TOKEN",
            route_kind="SECRET_ROUTE_TOKEN",
            response_kind="SECRET_RESPONSE_TOKEN",
            sink="SECRET_SINK_TOKEN",
            response_generation=True,
            delivered="true",
            reason="SECRET_TRANSCRIPT",
        )

        fields = _parse_delivery(logs[0])
        self.assertEqual(
            {"invalid"},
            {
                fields["event_id"],
                fields["route_kind"],
                fields["response_kind"],
                fields["sink"],
                fields["response_generation"],
                fields["delivered"],
                fields["reason"],
            },
        )
        self.assertNotIn("SECRET", logs[0])


def _capability(*, event_id, event_source, response_text):
    registry = TrustedUserInputIngressClaimRegistry()
    factory = TrustedIngressProducerRegistrarFactory(registry)
    if event_source == "lavi_chat_ui":
        adapter = LocalChatInputEventAdapter(event_id_factory=lambda: event_id)
        factory._bind_local_chat_input_event_adapter(adapter)
    elif event_source == "voice_input_final":
        adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
            event_id_factory=lambda: event_id,
        )
        factory._bind_voice_input_final_event_adapter(adapter)
    else:
        raise AssertionError(f"unsupported test source: {event_source!r}")
    delivery = factory.begin_invocation(
        input_event_adapter=adapter,
        source_policy=adapter.policy,
    ).create_registered_delivery("지도 만들어줘")
    evidence = delivery.accept_for_dispatch().consume_for_eligibility()
    owner = object()
    if not evidence.claim_for_eligibility(delivery.event, owner):
        raise AssertionError("eligibility claim failed")
    capability = evidence.issue_routed_response_emission_capability(
        delivery.event,
        owner,
        text=response_text,
        source="minecraft_chatclef",
    )
    return registry, delivery.event, capability, evidence


def _run_llm_feedback(*, event_id, event_source, provider_id, event_kind):
    response_text = "[Minecraft] 응답"
    registry, canonical_event, capability, evidence = _capability(
        event_id=event_id,
        event_source=event_source,
        response_text=response_text,
    )
    event = canonical_event
    if event.provider_id != provider_id or event.event_kind != event_kind:
        raise AssertionError("trusted adapter test tuple mismatch")
    logs = []
    outputs = []
    pipeline = _Pipeline()
    llm = LLM.__new__(LLM)
    llm.response_pipeline = pipeline
    llm.event_dispatcher = LLMEventDispatcher()
    llm.event_dispatcher.add_output_event_listener(outputs.append)
    llm.input_event_normalizer = LaviInputEventNormalizer()
    llm.trusted_user_input_ingress_claim_registry = registry
    llm.routed_external_response_publisher = RoutedExternalResponsePublisher(
        begin_generation_callback=pipeline.begin_response_generation,
        build_output_payload_callback=pipeline.build_stream_payload,
        send_output_callback=llm.send_output,
        send_full_output_callback=llm.send_full_output,
        emission_capability_consumer=(
            registry.consume_routed_response_emission_capability
        ),
        delivery_logger=CommandFeedbackDeliveryLogger(logs.append),
    )
    llm.input_router = _Router(
        MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_command_routed",
            response_text=response_text,
            publish_external_response=True,
            response_emission_capability=capability,
            route_kind="minecraft_command",
        )
    )
    evidence.close()

    yielded = list(
        llm.predict_wrapper(
            event,
            [],
            "system",
            trusted_ingress_evidence=object(),
        )
    )
    self_check = [item["text"] for item in outputs]
    if self_check != [response_text]:
        raise AssertionError(f"unexpected output delivery: {self_check!r}")
    return logs, yielded


def _voice_provider():
    descriptor = SimpleNamespace(id="VoiceInput")
    return SimpleNamespace(handle=SimpleNamespace(descriptor=descriptor))


def _parse_delivery(message):
    parts = message.split(" ")
    expected_event = f"event={CommandFeedbackDeliveryFormatter.EVENT_NAME}"
    if not parts or parts[0] != expected_event:
        raise AssertionError(f"unexpected diagnostic event: {message!r}")
    pairs = parts[1:]
    names = []
    parsed = {}
    for pair in pairs:
        if pair.count("=") != 1:
            raise AssertionError(f"invalid diagnostic atom: {pair!r}")
        name, value = pair.split("=", 1)
        if not name or not value or name in parsed:
            raise AssertionError(f"duplicate or empty diagnostic field: {pair!r}")
        names.append(name)
        parsed[name] = value
    if tuple(names) != CommandFeedbackDeliveryFormatter.CANONICAL_FIELDS:
        raise AssertionError(f"canonical field mismatch: {tuple(names)!r}")
    return parsed


class _Pipeline:
    def __init__(self):
        self.response_generation = 0

    def begin_response_generation(self):
        self.response_generation += 1
        return self.response_generation

    def build_stream_payload(self, text, generation):
        return {"text": text, "response_generation": generation}

    def predict(self, *_args):
        raise AssertionError("command feedback must not call the LLM provider")


class _Router:
    def __init__(self, decision):
        self._decision = decision

    def route_trusted_user_input(self, _event, _evidence):
        return self._decision


class _TerminalExtension:
    def __init__(self):
        self.callback = None

    def set_stop_terminal_response_callback(self, callback):
        self.callback = callback


if __name__ == "__main__":
    unittest.main()
