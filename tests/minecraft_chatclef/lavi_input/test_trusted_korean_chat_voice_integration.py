#20260905_kpopmodder: Verify trusted Chat/final-Voice routing, replies, and no LLM recursion end to end.
from __future__ import annotations

import threading
import unittest
from dataclasses import replace
from types import SimpleNamespace
from unittest.mock import Mock, patch

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
from app_core.composition_core.component_wiring import MinecraftInputRouterWiring
from llm_core.chat_input import TrustedLocalChatInputDispatchCoordinator
from llm_core.event_dispatcher import LLMEventDispatcher
from llm_core.input_queue_worker import LLMInputQueueWorker
from llm_core.llm_component import LLM
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
from plugins.Minecraft.fabric.chatclef.input.eligibility import (
    KoreanChatMicrophoneEligibilityAdmission,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_natural_language_service import (
    ChatClefNaturalLanguageService,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleFacade,
    CommandFeedbackServerApi,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)


class TrustedKoreanChatVoiceIntegrationTests(unittest.TestCase):
    #20260913_kpopmodder: Exercise the real consumed-ingress route, not source-string-only GOTO fixtures.
    def test_all_nine_goto_forms_preserve_chat_and_final_voice_submission(self):
        inputs = (
            "500 90 -928로 가줘",
            "500,90,-928으로 가줘",
            "500, 90, -928 좌표로 가줘",
            "좌표 500, 90, -928로 이동해줘",
            "x 500 y 90 z -928로 가줘",
            "x=500, y=90, z=-928 좌표로 가줘",
            "엑스 500 와이 90 제트 마이너스 928 좌표로 가줘",
            "500 90 마이너스 928로 가줘",
            "(500, 90, -928)으로 이동해",
        )
        for text in inputs:
            for voice in (False, True):
                with self.subTest(text=text, voice=voice):
                    adapter = _RecordingMinecraftAdapter()
                    service = ChatClefNaturalLanguageService()
                    extension = MinecraftFabricChatClefExtension(
                        adapter=adapter, natural_language_service=service,
                    )
                    llm, pipeline, _outputs = _llm_harness(extension)
                    with patch.object(
                        service, "translate", wraps=service.translate,
                    ) as translate, patch.object(
                        llm.input_router, "route_trusted_user_input",
                        wraps=llm.input_router.route_trusted_user_input,
                    ) as route:
                        _dispatch_goto_input(llm, text, voice=voice)

                    translate.assert_called_once_with(text)
                    self.assertEqual(1, route.call_count)
                    event, evidence = route.call_args.args
                    self.assertIsNotNone(evidence)
                    self.assertEqual(text, event.text)
                    self.assertEqual(1, len(adapter.requests))
                    request = adapter.requests[0]
                    self.assertEqual("goto 500 90 -928", request.command)
                    self.assertEqual(
                        "voice_input_final" if voice else "lavi_chat_ui",
                        request.source,
                    )
                    self.assertEqual(
                        {
                            "source": event.source,
                            "provider_id": event.provider_id,
                            "event_kind": event.event_kind,
                            "final": event.final,
                            "event_id": event.event_id,
                        },
                        request.metadata["input_event"],
                    )
                    self.assertEqual(
                        text, request.metadata["natural_language"]["original_text"],
                    )
                    self.assertEqual(0, pipeline.provider_calls)

    def test_goto_outer_spaces_preserve_raw_event_translation_and_submission(self):
        text = "  500, 90, -928 좌표로 가줘  "
        for voice in (False, True):
            with self.subTest(voice=voice):
                adapter = _RecordingMinecraftAdapter()
                service = ChatClefNaturalLanguageService()
                extension = MinecraftFabricChatClefExtension(
                    adapter=adapter, natural_language_service=service,
                )
                llm, pipeline, _outputs = _llm_harness(extension)
                with patch.object(
                    service, "translate", wraps=service.translate,
                ) as translate, patch.object(
                    extension, "translate_natural_language_command",
                    wraps=extension.translate_natural_language_command,
                ) as extension_translate, patch.object(
                    llm.input_router, "route_trusted_user_input",
                    wraps=llm.input_router.route_trusted_user_input,
                ) as route:
                    yielded, _queued = _dispatch_goto_input(llm, text, voice=voice)

                extension_translate.assert_called_once_with(text)
                translate.assert_called_once_with(text)
                self.assertEqual(1, route.call_count)
                event, _evidence = route.call_args.args
                self.assertEqual(text, event.text)
                self.assertEqual(1, len(adapter.requests))
                request = adapter.requests[0]
                self.assertEqual("goto 500 90 -928", request.command)
                self.assertEqual(
                    text, request.metadata["natural_language"]["translation_input_text"],
                )
                self.assertEqual(
                    text, request.metadata["natural_language"]["original_text"],
                )
                self.assertEqual(event.event_id, request.metadata["input_event"]["event_id"])
                self.assertEqual(event.source, request.source)
                self.assertEqual(1, len(yielded))
                response = getattr(yielded[0], "content", yielded[0])
                for coordinate in ("500", "90", "-928"):
                    self.assertIn(coordinate, response)
                self.assertNotIn("도착했어", response)
                self.assertEqual(0, pipeline.provider_calls)

    def test_goto_logging_enabled_disabled_or_failing_preserves_route_outcome(self):
        for text, valid in (
            ("500, 90, -928 좌표로 가줘", True),
            ("500.5 90 -928로 이동해", False),
        ):
            for voice in (False, True):
                responses = []
                for mode in ("enabled", "disabled", "failing"):
                    with self.subTest(text=text, voice=voice, mode=mode):
                        adapter = _RecordingMinecraftAdapter()
                        service = ChatClefNaturalLanguageService()
                        extension = MinecraftFabricChatClefExtension(
                            adapter=adapter, natural_language_service=service,
                        )
                        llm, pipeline, _outputs = _llm_harness(extension)
                        logs = []
                        callback = Mock(
                            side_effect=(
                                logs.append if mode == "enabled"
                                else RuntimeError("test diagnostic sink failed")
                                if mode == "failing" else None
                            ),
                        )
                        with patch.object(
                            llm.input_router._router_logger, "_log_callback", callback,
                        ), patch.object(
                            service, "translate", wraps=service.translate,
                        ) as translate:
                            yielded, _queued = _dispatch_goto_input(llm, text, voice=voice)

                        self.assertEqual(int(valid), translate.call_count)
                        self.assertEqual(int(valid), len(adapter.requests))
                        self.assertEqual(0, pipeline.provider_calls)
                        self.assertEqual(1, len(yielded))
                        responses.append(getattr(yielded[0], "content", yielded[0]))
                        self.assertGreater(callback.call_count, 0)
                        if mode == "enabled":
                            boundaries = [line for line in logs if "event=korean_goto_input" in line]
                            self.assertEqual(2 if valid else 1, len(boundaries))
                            self.assertTrue(all(text not in line for line in boundaries))
                self.assertEqual([responses[0]] * 3, responses)

    def test_goto_questions_quotes_and_negation_return_to_conversation(self):
        inputs = (
            "500 90 -928로 가지 마",
            "좌표 500 90 -928로 가면 어떻게 돼?",
            '"500 90 -928로 가줘"라고 말했어',
            "500 90 -928로 가줘?",
            "집에 가줘",
        )
        for text in inputs:
            for voice in (False, True):
                with self.subTest(text=text, voice=voice):
                    adapter = _RecordingMinecraftAdapter()
                    service = SimpleNamespace(translate=Mock(
                        side_effect=AssertionError("Minecraft translation must not run"),
                    ))
                    extension = MinecraftFabricChatClefExtension(
                        adapter=adapter, natural_language_service=service,
                    )
                    llm, pipeline, outputs = _llm_harness(extension)

                    yielded, _queued = _dispatch_goto_input(llm, text, voice=voice)

                    self.assertEqual([f"LLM:{text}"], yielded)
                    self.assertEqual(1, pipeline.provider_calls)
                    service.translate.assert_not_called()
                    self.assertEqual([], outputs)
                    self.assertEqual([], adapter.requests)

    def test_invalid_goto_requests_clarify_once_without_translation_or_submission(self):
        inputs = (
            "500 90 좌표로 가줘",
            "500 90 -928 4 좌표로 가줘",
            "500.5 90 -928로 이동해",
            "2147483648 90 -928 좌표로 가줘",
            "x 500 z -928 y 90 좌표로 가줘",
            "x 500 y 90 x -928 좌표로 가줘",
            "500 90 마이너스 -928로 가줘",
            "오백 구십 마이너스 구백이십팔 좌표로 가줘",
            "~500 90 -928 좌표로 가줘",
            "500 90 -928 네더로 이동해줘",
            "500 90 -928로 가고 좀비 공격해",
            "(500, 90, -928으로 이동해",
            "500\t90 -928로 이동해",
            "\n500 90 -928로 이동해",
            "500 90 -928로 이동해\r\n",
        )
        for text in inputs:
            for voice in (False, True):
                with self.subTest(text=text, voice=voice):
                    adapter = _RecordingMinecraftAdapter()
                    service = SimpleNamespace(translate=Mock(
                        side_effect=AssertionError("Invalid GOTO must terminate before translation"),
                    ))
                    extension = MinecraftFabricChatClefExtension(
                        adapter=adapter, natural_language_service=service,
                    )
                    llm, pipeline, outputs = _llm_harness(extension)

                    yielded, _queued = _dispatch_goto_input(llm, text, voice=voice)

                    self.assertEqual(1, len(yielded))
                    self.assertEqual(1, len(outputs))
                    self.assertEqual(0, pipeline.provider_calls)
                    service.translate.assert_not_called()
                    self.assertEqual([], adapter.requests)

    def test_goto_final_voice_queue_replay_cannot_submit_twice(self):
        adapter = _RecordingMinecraftAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, _outputs = _llm_harness(extension)

        _yielded, queued = _dispatch_goto_input(
            llm, "500 90 -928로 가줘", voice=True,
        )
        self.assertEqual(1, len(adapter.requests))
        first_request_id = adapter.requests[0].request_id
        list(llm.accept_queued_input(queued, [], "system"))

        self.assertEqual([first_request_id], [row.request_id for row in adapter.requests])
        self.assertEqual(0, pipeline.provider_calls)

    def test_goto_same_words_in_new_events_are_not_permanently_deduplicated(self):
        adapter = _RecordingMinecraftAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, _outputs = _llm_harness(extension)
        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(), output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(voice_adapter)
        for _attempt in range(2):
            self.assertIsNotNone(voice.enqueue("500 90 -928로 가줘"))
            queued = llm.input_queue_worker.input_queue.get_nowait()
            list(llm.accept_queued_input(queued, [], "system"))
        self.assertEqual(2, len(adapter.requests))
        self.assertNotEqual(adapter.requests[0].request_id, adapter.requests[1].request_id)
        self.assertNotEqual(
            adapter.requests[0].metadata["input_event"]["event_id"],
            adapter.requests[1].metadata["input_event"]["event_id"],
        )
        self.assertEqual(0, pipeline.provider_calls)

    def test_goto_busy_preserves_active_work_and_does_not_submit_or_replay(self):
        adapter = _ActiveStatusLifecycleAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)
        for voice in (False, True):
            yielded, _queued = _dispatch_goto_input(
                llm, "500 90 -928로 가줘", voice=voice,
            )
            self.assertEqual(1, len(yielded))
        self.assertEqual([], adapter.command_requests)
        self.assertEqual(adapter.active_identity, adapter.current_active_identity())
        self.assertEqual(
            ["command_busy_current_work", "command_busy_current_work"],
            [row["route_kind"] for row in outputs],
        )
        self.assertEqual(0, pipeline.provider_calls)

    def test_goto_binding_rejects_consistent_mutated_intent_and_command(self):
        replacements = (
            {"intent_type": "goto", "x": 501, "y": 90, "z": -928},
            {"intent_type": "get_item", "item_phrase": "다이아몬드", "quantity": 1},
        )
        for replacement in replacements:
            for voice in (False, True):
                with self.subTest(replacement=replacement, voice=voice):
                    payload = {
                        "status": "validated", "executable": True,
                        "command": (
                            "goto 501 90 -928" if replacement["intent_type"] == "goto"
                            else "get diamond 1"
                        ),
                        "intent": replacement, "resolved_target": "diamond",
                    }
                    translation = ChatClefTranslationResultDTO.from_mapping(payload)
                    service = SimpleNamespace(translate=Mock(return_value=translation))
                    adapter = _RecordingMinecraftAdapter()
                    extension = MinecraftFabricChatClefExtension(
                        adapter=adapter, natural_language_service=service,
                    )
                    llm, pipeline, _outputs = _llm_harness(extension)

                    _dispatch_goto_input(llm, "500 90 -928로 가줘", voice=voice)

                    service.translate.assert_called_once()
                    self.assertEqual([], adapter.requests)
                    self.assertEqual(0, pipeline.provider_calls)

    def test_translation_cannot_invent_goto_for_non_goto_raw_input(self):
        translation = ChatClefTranslationResultDTO.from_mapping({
            "status": "validated", "executable": True,
            "command": "goto 500 90 -928",
            "intent": {"intent_type": "goto", "x": 500, "y": 90, "z": -928},
        })
        for voice in (False, True):
            with self.subTest(voice=voice):
                adapter = _RecordingMinecraftAdapter()
                service = SimpleNamespace(translate=Mock(return_value=translation))
                extension = MinecraftFabricChatClefExtension(
                    adapter=adapter, natural_language_service=service,
                )
                llm, pipeline, _outputs = _llm_harness(extension)

                _dispatch_goto_input(llm, "다이아몬드 캐줘", voice=voice)

                service.translate.assert_called_once()
                self.assertEqual([], adapter.requests)
                self.assertEqual(0, pipeline.provider_calls)

    def test_goto_missing_foreign_or_closed_proof_never_reaches_translation(self):
        for fault in ("missing", "foreign_owner", "foreign_event", "closed", "interim"):
            with self.subTest(fault=fault):
                adapter = _RecordingMinecraftAdapter()
                service = ChatClefNaturalLanguageService()
                extension = MinecraftFabricChatClefExtension(
                    adapter=adapter, natural_language_service=service,
                )
                llm, _pipeline, _outputs = _llm_harness(extension)
                factory = llm.trusted_ingress_producer_registrar_factory
                chat_adapter = LocalChatInputEventAdapter()
                factory._bind_local_chat_input_event_adapter(chat_adapter)
                delivery = factory.begin_invocation(
                    input_event_adapter=chat_adapter, source_policy=chat_adapter.policy,
                ).create_registered_delivery("좌표 500 90 -928로 이동해줘")
                event = delivery.event
                evidence = delivery.accept_for_dispatch().consume_for_eligibility()
                owner = object() if fault == "foreign_owner" else llm.input_router
                proof, reason = KoreanChatMicrophoneEligibilityAdmission(
                    llm.trusted_user_input_ingress_claim_registry.validate_consumed_evidence,
                ).issue(event=event, consumed_ingress_evidence=evidence, owner=owner)
                self.assertEqual("eligible", reason)
                self.assertTrue(proof.matches_event(event, owner))
                if fault == "foreign_event":
                    event = replace(event, event_id="f" * 32)
                elif fault == "closed":
                    self.assertTrue(proof.close())
                elif fault == "interim":
                    event = replace(
                        event, source="voice_input_partial", provider_id="VoiceInput",
                        event_kind="partial_transcript", final=False,
                    )
                try:
                    with patch.object(service, "translate", wraps=service.translate) as translate:
                        llm.input_router.route(
                            event,
                            korean_eligibility_proof=None if fault == "missing" else proof,
                        )
                    self.assertEqual([], adapter.requests)
                    if fault != "interim":
                        translate.assert_not_called()
                finally:
                    proof.close()

    def test_goto_disconnected_input_is_consumed_without_queueing_retry(self):
        for voice in (False, True):
            with self.subTest(voice=voice):
                adapter = _RecordingMinecraftAdapter()
                extension = MinecraftFabricChatClefExtension(adapter=adapter)
                llm, pipeline, _outputs = _llm_harness(extension)
                disconnected = replace(
                    adapter.get_status(), connected=False,
                    lifecycle_state=BridgeLifecycleState.DISCONNECTED,
                )
                with patch.object(adapter, "get_status", return_value=disconnected):
                    yielded, _queued = _dispatch_goto_input(
                        llm, "500 90 -928로 가줘", voice=voice,
                    )

                self.assertEqual(1, len(yielded))
                self.assertEqual(0, pipeline.provider_calls)
                self.assertEqual([], adapter.requests)
                self.assertTrue(llm.input_queue_worker.input_queue.empty())

    def test_chat_and_final_voice_contextual_status_publish_once_without_llm(self):
        adapter = _ActiveStatusLifecycleAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)

        chat_yields = list(
            _chat_coordinator(llm).dispatch("지금 뭐 해?", [], "system")
        )
        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )
        self.assertIsNotNone(voice.enqueue("지금 뭐 해?"))
        queued = llm.input_queue_worker.input_queue.get_nowait()
        voice_yields = list(llm.accept_queued_input(queued, [], "system"))

        expected = "다이아 곡괭이 만드는 중이야"
        self.assertEqual(1, len(chat_yields))
        self.assertEqual(expected, chat_yields[0].content)
        self.assertEqual("Minecraft", chat_yields[0].metadata["title"])
        self.assertEqual([expected], voice_yields)
        self.assertEqual([expected, expected], [item["text"] for item in outputs])
        self.assertEqual(
            ["command_status_query", "command_status_query"],
            [item["route_kind"] for item in outputs],
        )
        self.assertEqual(
            ["command_status", "command_status"],
            [item["response_kind"] for item in outputs],
        )
        self.assertEqual([("status", True), ("status", True)], adapter.ack_calls)
        self.assertEqual(0, pipeline.provider_calls)
        self.assertEqual(adapter.active_identity, adapter.current_active_identity())
        self.assertEqual(0, len(adapter.command_requests))

    def test_chat_and_final_voice_busy_command_reuse_active_status_without_llm(
        self,
    ):
        adapter = _ActiveStatusLifecycleAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)
        chat = _chat_coordinator(llm)

        status_yields = list(chat.dispatch("지금 뭐 해?", [], "system"))
        busy_chat_yields = list(chat.dispatch("호박 파이 만들어줘", [], "system"))

        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )
        self.assertIsNotNone(voice.enqueue("호박 파이 만들어줘"))
        queued = llm.input_queue_worker.input_queue.get_nowait()
        busy_voice_yields = list(llm.accept_queued_input(queued, [], "system"))

        expected = "다이아 곡괭이 만드는 중이야"
        self.assertEqual(1, len(status_yields))
        self.assertEqual(1, len(busy_chat_yields))
        self.assertEqual(expected, status_yields[0].content)
        self.assertEqual(expected, busy_chat_yields[0].content)
        self.assertEqual([expected], busy_voice_yields)
        self.assertEqual("Minecraft", busy_chat_yields[0].metadata["title"])
        self.assertEqual(
            [expected, expected, expected],
            [item["text"] for item in outputs],
        )
        self.assertEqual(
            [1, 2, 3],
            [item["response_generation"] for item in outputs],
        )
        self.assertEqual(
            [
                "command_status_query",
                "command_busy_current_work",
                "command_busy_current_work",
            ],
            [item["route_kind"] for item in outputs],
        )
        self.assertEqual(
            ["command_status", "command_status", "command_status"],
            [item["response_kind"] for item in outputs],
        )
        self.assertEqual(
            [("status", True), ("status", True), ("status", True)],
            adapter.ack_calls,
        )
        self.assertEqual(
            [adapter.active_identity, adapter.active_identity],
            [
                (
                    identity.active_session_id,
                    identity.active_generation,
                    identity.active_request_id,
                    identity.active_command_message_id,
                )
                for identity in adapter.busy_observed_identities
            ],
        )
        for output in outputs[1:]:
            serialized = repr(output)
            self.assertNotIn("호박", serialized)
            self.assertNotIn("pumpkin_pie", serialized)
        self.assertEqual(0, pipeline.provider_calls)
        self.assertEqual(adapter.active_identity, adapter.current_active_identity())
        self.assertEqual([], adapter.command_requests)

    def test_all_busy_route_origins_reuse_active_status_for_chat_and_final_voice(
        self,
    ):
        adapter = _ActiveStatusLifecycleAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)
        chat = _chat_coordinator(llm)
        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )
        route_inputs = (
            "호박 파이 만들어줘",
            "버튼 만들어줘",
            "@auto_deposit_trust 반경 16x16",
        )
        yielded = []

        with self.assertLogs("LAV", level="INFO") as captured:
            for text in route_inputs:
                with self.subTest(source="chat", text=text):
                    yielded.extend(
                        value.content
                        for value in chat.dispatch(text, [], "system")
                    )
                with self.subTest(source="final_voice", text=text):
                    self.assertIsNotNone(voice.enqueue(text))
                    queued = llm.input_queue_worker.input_queue.get_nowait()
                    yielded.extend(llm.accept_queued_input(queued, [], "system"))

        expected = "다이아 곡괭이 만드는 중이야"
        self.assertEqual([expected] * 6, yielded)
        self.assertEqual([expected] * 6, [item["text"] for item in outputs])
        self.assertEqual(
            ["command_busy_current_work"] * 6,
            [item["route_kind"] for item in outputs],
        )
        self.assertEqual(
            [("status", True)] * 6,
            adapter.ack_calls,
        )
        self.assertEqual(6, len(adapter.busy_observed_identities))
        self.assertEqual([], adapter.command_requests)
        self.assertEqual(0, pipeline.provider_calls)
        self.assertEqual(adapter.active_identity, adapter.current_active_identity())
        admission_logs = [
            line
            for line in captured.output
            if "event=minecraft_korean_feature_admission" in line
        ]
        self.assertEqual(6, len(admission_logs))
        self.assertEqual(
            ["A", "A", "B", "B", "A", "A"],
            [_log_field(line, "feature_scope") for line in admission_logs],
        )
        self.assertEqual(
            [
                "minecraft_command_feedback_v1",
                "minecraft_command_feedback_v1",
                "generic_crafting_defaults_v1",
                "generic_crafting_defaults_v1",
                "minecraft_command_feedback_v1",
                "minecraft_command_feedback_v1",
            ],
            [_log_field(line, "feature_policy_id") for line in admission_logs],
        )
        self.assertTrue(
            all(
                "reason=minecraft_command_busy" in line
                and "feature_activation_status=handled_without_activation" in line
                and "feature_scope=none" not in line
                for line in admission_logs
            )
        )

    def test_chat_and_final_voice_five_crafting_defaults_publish_once_without_llm(
        self,
    ):
        adapter = _RecordingMinecraftAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)
        chat = _chat_coordinator(llm)
        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )
        cases = {
            "다락문 만들어줘": (
                "get trapdoor 1",
                "[Minecraft] 다락문 1개를 준비하도록 명령했어요.",
            ),
            "지도 만들어줘": (
                "get map 1",
                "[Minecraft] 지도 1개를 준비하도록 명령했어요.",
            ),
            "압력판 만들어줘": (
                "get wooden_pressure_plate 1",
                "[Minecraft] 압력판 1개를 준비하도록 명령했어요.",
            ),
            "발판 만들어줘": (
                "get wooden_pressure_plate 1",
                "[Minecraft] 발판 1개를 준비하도록 명령했어요.",
            ),
            "버튼 만들어줘": (
                "get wooden_button 1",
                "[Minecraft] 버튼 1개를 준비하도록 명령했어요.",
            ),
        }

        for text, (expected_command, expected_feedback) in cases.items():
            with self.subTest(source="chat", text=text):
                request_count = len(adapter.requests)
                output_count = len(outputs)

                yielded = list(chat.dispatch(text, [], "system"))

                self.assertEqual([expected_feedback], yielded)
                self.assertEqual(request_count + 1, len(adapter.requests))
                self.assertEqual(expected_command, adapter.requests[-1].command)
                self.assertEqual(output_count + 1, len(outputs))
                self.assertEqual(expected_feedback, outputs[-1]["text"])

            with self.subTest(source="final_voice", text=text):
                request_count = len(adapter.requests)
                output_count = len(outputs)

                receipt = voice.enqueue(text)
                queued = llm.input_queue_worker.input_queue.get_nowait()
                yielded = list(llm.accept_queued_input(queued, [], "system"))

                self.assertIsNotNone(receipt)
                self.assertEqual([expected_feedback], yielded)
                self.assertEqual(request_count + 1, len(adapter.requests))
                self.assertEqual(expected_command, adapter.requests[-1].command)
                self.assertEqual(output_count + 1, len(outputs))
                self.assertEqual(expected_feedback, outputs[-1]["text"])

        self.assertEqual(10, len(adapter.requests))
        self.assertEqual(10, len(outputs))
        self.assertEqual(
            list(range(1, 11)),
            [item["response_generation"] for item in outputs],
        )
        self.assertEqual(0, pipeline.provider_calls)
        self.assertEqual(
            0,
            extension.generic_crafting_defaults_activation_registry.record_count,
        )

    def test_chat_and_final_voice_existing_quantity_command_feedback_without_llm(
        self,
    ):
        adapter = _RecordingMinecraftAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)
        expected_feedback = (
            "[Minecraft] 레드스톤 가루 5개 수집 명령을 제출했어요."
        )

        chat_yields = list(
            _chat_coordinator(llm).dispatch(
                "레드스톤 5개 구해줘",
                [],
                "system",
            )
        )

        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )
        self.assertIsNotNone(voice.enqueue("레드스톤 5개 구해줘"))
        queued = llm.input_queue_worker.input_queue.get_nowait()
        voice_yields = list(llm.accept_queued_input(queued, [], "system"))

        self.assertEqual([expected_feedback], chat_yields)
        self.assertEqual([expected_feedback], voice_yields)
        self.assertEqual(
            ["get redstone 5", "get redstone 5"],
            [request.command for request in adapter.requests],
        )
        self.assertEqual(
            [expected_feedback, expected_feedback],
            [item["text"] for item in outputs],
        )
        self.assertEqual(0, pipeline.provider_calls)
        self.assertEqual(
            0,
            extension.generic_crafting_defaults_activation_registry.record_count,
        )

    def test_chat_and_final_voice_stop_rejection_publish_once_without_llm(self):
        llm, pipeline, outputs = _llm_harness(None)

        chat_yields = list(
            _chat_coordinator(llm).dispatch("멈춰", [], "system")
        )
        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )
        self.assertIsNotNone(voice.enqueue("마크 AI 멈춰줘"))
        queued = llm.input_queue_worker.input_queue.get_nowait()
        voice_yields = list(llm.accept_queued_input(queued, [], "system"))

        expected = "마인크래프트 연결이 끊겨 있어 중지 요청을 보내지 못했어요."
        self.assertEqual(1, len(chat_yields))
        self.assertEqual(expected, chat_yields[0].content)
        self.assertEqual("Minecraft", chat_yields[0].metadata["title"])
        self.assertEqual([expected], voice_yields)
        self.assertEqual([expected, expected], [item["text"] for item in outputs])
        self.assertTrue(
            all(
                item["presentation"]["badge_label"] == "Minecraft"
                for item in outputs
            )
        )
        self.assertEqual(0, pipeline.provider_calls)

    def test_chat_and_final_voice_accepted_stop_publish_no_start_response(self):
        adapter = _RecordingMinecraftAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)

        chat_yields = list(
            _chat_coordinator(llm).dispatch("멈춰", [], "system")
        )
        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )
        self.assertIsNotNone(voice.enqueue("마크 AI 멈춰줘"))
        queued = llm.input_queue_worker.input_queue.get_nowait()
        voice_yields = list(llm.accept_queued_input(queued, [], "system"))

        self.assertEqual([], chat_yields)
        self.assertEqual([], voice_yields)
        self.assertEqual([], outputs)
        self.assertEqual([], adapter.requests)
        self.assertEqual(2, len(adapter.stop_requests))
        self.assertEqual(
            ["lavi_chat_ui", "voice_input_final"],
            [request["event"].source for request in adapter.stop_requests],
        )
        self.assertEqual(0, pipeline.provider_calls)

    def test_chat_and_final_voice_owned_item_rejections_publish_once_without_llm(self):
        adapter = _RecordingMinecraftAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)

        chat = _chat_coordinator(llm)
        chat_yields = list(
            chat.dispatch(
                "구리 검 하나 만들어",
                [],
                "system",
            )
        )
        unknown_chat_yields = list(
            chat.dispatch(
                "루비 검 가져와줘",
                [],
                "system",
            )
        )

        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )
        self.assertIsNotNone(voice.enqueue("다이아몬드 갑옷 가져와"))
        queued = llm.input_queue_worker.input_queue.get_nowait()
        voice_yields = list(llm.accept_queued_input(queued, [], "system"))

        self.assertEqual(
            ["[Minecraft] 요청한 재료나 아이템은 현재 지원하지 않아요."],
            chat_yields,
        )
        self.assertEqual(
            ["[Minecraft] 어떤 아이템을 준비할지 이해하지 못했어요."],
            unknown_chat_yields,
        )
        self.assertEqual(
            ["[Minecraft] 어떤 아이템을 준비할지 하나로 정해 주세요."],
            voice_yields,
        )
        self.assertEqual(
            [chat_yields[0], unknown_chat_yields[0], voice_yields[0]],
            [item["text"] for item in outputs],
        )
        self.assertEqual([], adapter.requests)
        self.assertEqual(0, pipeline.provider_calls)

    def test_arbitrary_unknown_item_chat_and_voice_still_fall_through_to_llm(self):
        adapter = _RecordingMinecraftAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)

        chat_yields = list(
            _chat_coordinator(llm).dispatch("루비 가져와줘", [], "system")
        )

        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )
        self.assertIsNotNone(voice.enqueue("루비 가져와줘"))
        queued = llm.input_queue_worker.input_queue.get_nowait()
        voice_yields = list(llm.accept_queued_input(queued, [], "system"))

        self.assertEqual(["LLM:루비 가져와줘"], chat_yields)
        self.assertEqual(["LLM:루비 가져와줘"], voice_yields)
        self.assertEqual([], outputs)
        self.assertEqual([], adapter.requests)
        self.assertEqual(2, pipeline.provider_calls)

    def test_explicit_minecraft_markers_own_unknown_items_for_chat_and_voice(self):
        adapter = _RecordingMinecraftAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)

        chat_yields = list(
            _chat_coordinator(llm).dispatch(
                "마크 루비 가져와줘",
                [],
                "system",
            )
        )

        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )
        self.assertIsNotNone(voice.enqueue("마인크래프트 루비 만들어줘"))
        queued = llm.input_queue_worker.input_queue.get_nowait()
        voice_yields = list(llm.accept_queued_input(queued, [], "system"))

        expected = "[Minecraft] 어떤 아이템을 준비할지 이해하지 못했어요."
        self.assertEqual([expected], chat_yields)
        self.assertEqual([expected], voice_yields)
        self.assertEqual([expected, expected], [item["text"] for item in outputs])
        self.assertEqual([], adapter.requests)
        self.assertEqual(0, pipeline.provider_calls)

    def test_non_korean_trusted_chat_falls_through_without_new_publication(self):
        llm, pipeline, outputs = _llm_harness(None)

        yielded = list(_chat_coordinator(llm).dispatch("hello", [], "system"))

        self.assertEqual(["LLM:hello"], yielded)
        self.assertEqual(1, pipeline.provider_calls)
        self.assertEqual([], outputs)


#20260913_kpopmodder: Reuse the existing trusted producers for focused GOTO input acceptance.
def _dispatch_goto_input(llm, text, *, voice):
    if not voice:
        return list(_chat_coordinator(llm).dispatch(text, [], "system")), None
    voice_adapter = ProviderBoundInputEventAdapter(
        provider=_voice_provider(),
        output_callback=lambda _event: None,
        source_resolver=InputProviderSourceResolver(),
    )
    coordinator = llm.create_trusted_voice_input_final_enqueue_coordinator(voice_adapter)
    receipt = coordinator.enqueue(text)
    if receipt is None:
        raise AssertionError("trusted final-voice fixture was not queued")
    queued = llm.input_queue_worker.input_queue.get_nowait()
    return list(llm.accept_queued_input(queued, [], "system")), queued


def _llm_harness(extension):
    llm = LLM.__new__(LLM)
    pipeline = _Pipeline()
    outputs = []
    llm.response_pipeline = pipeline
    llm.event_dispatcher = LLMEventDispatcher()
    llm.event_dispatcher.add_output_event_listener(outputs.append)
    llm.input_event_normalizer = LaviInputEventNormalizer()
    llm.speech_style_helper = SimpleNamespace(
        build_prompt=lambda prompt, _mode: prompt,
    )
    llm.speech_style_mode = "polite"
    llm.history = []
    registry = TrustedUserInputIngressClaimRegistry()
    llm.trusted_user_input_ingress_claim_registry = registry
    llm.trusted_ingress_producer_registrar_factory = (
        TrustedIngressProducerRegistrarFactory(registry)
    )
    llm.set_input_router = lambda router: setattr(llm, "input_router", router)
    MinecraftInputRouterWiring().wire(llm=llm, extension=extension)
    llm.input_queue_worker = LLMInputQueueWorker(
        response_callback=llm.predict_wrapper,
        history_callback=lambda: llm.history,
        system_prompt_callback=lambda: "system",
        queue_updated_callback=lambda: None,
        queued_response_callback=llm.accept_queued_input,
    )
    llm.input_queue_worker.process_input_queue = lambda: None
    return llm, pipeline, outputs


def _chat_coordinator(llm):
    return TrustedLocalChatInputDispatchCoordinator(
        input_event_adapter=LocalChatInputEventAdapter(),
        producer_registrar_factory=llm.trusted_ingress_producer_registrar_factory,
        registered_dispatch_callback=llm.accept_registered_input,
        fallback_predict_callback=llm.predict_wrapper,
    )


def _voice_provider():
    descriptor = SimpleNamespace(id="VoiceInput")
    return SimpleNamespace(handle=SimpleNamespace(descriptor=descriptor))


def _log_field(line: str, name: str) -> str:
    prefix = f"{name}="
    for token in line.split():
        if token.startswith(prefix):
            return token[len(prefix) :]
    return ""


class _Pipeline:
    def __init__(self):
        self.response_generation = 0
        self.provider_calls = 0

    def begin_response_generation(self):
        self.response_generation += 1
        return self.response_generation

    def build_stream_payload(self, text, generation):
        return {"text": text, "response_generation": generation}

    def predict(self, payload, _history, _system_prompt):
        self.provider_calls += 1
        yield f"LLM:{payload}"


class _RecordingMinecraftAdapter:
    backend_id = "fabric_chatclef"

    def __init__(self):
        self.requests = []
        self.stop_requests = []

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

    def submit_stop_control(self, **values):
        self.stop_requests.append(values)
        return SimpleNamespace(
            accepted=True,
            reason="accepted",
            result={"ok": True, "status": "accepted"},
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


class _ActiveStatusLifecycleAdapter:
    backend_id = "fabric_chatclef"

    def __init__(self) -> None:
        self.command_requests = []
        self.ack_calls = []
        self.busy_observed_identities = []
        self._tracker = CommandFeedbackLifecycleFacade()
        self._ownership = FabricChatClefConnectionOwnership(
            crafting_feedback_tracker=self._tracker,
        )
        self._bind_running_craft()
        original_acknowledge = (
            self._ownership.acknowledge_command_feedback_publication
        )

        def acknowledge(permit, published):
            self.ack_calls.append((permit.kind, published))
            return original_acknowledge(permit, published)

        self._ownership.acknowledge_command_feedback_publication = acknowledge
        self._api = CommandFeedbackServerApi(
            connection_ownership=self._ownership,
            command_lock=threading.RLock(),
            terminal_listener=SimpleNamespace(set_callback=lambda _callback: None),
            terminal_delivery=SimpleNamespace(publish=lambda _terminal: None),
        )
        self.active_identity = self.current_active_identity()

    def inspect_command_feedback_status(self, query):
        return self._api.inspect_status(query)

    def inspect_command_feedback_busy_status(self, observed_identity):
        self.busy_observed_identities.append(observed_identity)
        return self._api.inspect_busy_status(observed_identity)

    def get_status(self):
        active = self._ownership.active_command_owner
        return StatusSnapshotDTO(
            backend_id=self.backend_id,
            enabled=True,
            connected=True,
            lifecycle_state=BridgeLifecycleState.CONNECTED,
            detail="connected",
            details={
                "commands": {
                    "active_session_id": active.session_id,
                    "active_generation": active.generation,
                    "active_request_id": active.request_id,
                    "active_command_message_id": active.command_message_id,
                    "active_command": active.command,
                    "last_result": None,
                }
            },
        )

    def submit_command(self, request):
        self.command_requests.append(request)
        raise AssertionError("a status/busy response submitted a Minecraft command")

    def current_active_identity(self):
        active = self._ownership.active_command_owner
        return (
            active.session_id,
            active.generation,
            active.request_id,
            active.command_message_id,
        )

    def _bind_running_craft(self) -> None:
        factory = CommandFeedbackDescriptorFactory()
        event = LocalChatInputEventAdapter(
            event_id_factory=lambda: "1" * 32,
        ).adapt("다이아 곡괭이 만들어줘")
        descriptor = factory.from_trusted_translation(
            event=event,
            translation={
                "status": "validated",
                "executable": True,
                "command": "get diamond_pickaxe 1",
                "resolved_target": "diamond_pickaxe",
                "intent": {
                    "language": "ko",
                    "intent_type": "get_item",
                    "original_text": event.text,
                    "item_phrase": "다이아 곡괭이",
                    "quantity": 1,
                },
            },
        )
        grant = CommandFeedbackAdmissionCoordinator(
            live_proof_validator=lambda _proof, _event: False,
            descriptor_factory=factory,
        ).issue_descriptor(descriptor)
        websocket = object()
        if (
            grant is None
            or not self._ownership.try_activate(
                websocket=websocket,
                session_id="status-session",
            ).accepted
            or not self._ownership.reserve_command_feedback(grant)
        ):
            raise AssertionError("STATUS lifecycle fixture admission failed")
        active = self._ownership.begin_command(
            request_id="status-request",
            command_message_id="status-message",
            command=descriptor.command,
            source=descriptor.command_source,
            metadata={
                "input_event": {
                    "source": descriptor.input_source,
                    "provider_id": descriptor.provider_id,
                    "event_kind": descriptor.event_kind,
                    "final": True,
                    "event_id": descriptor.event_id,
                }
            },
        )
        if active is None:
            raise AssertionError("STATUS lifecycle fixture binding failed")
        start_permit = self._tracker.claim_start(
            grant,
            {
                "ok": True,
                "status": {
                    "request_id": active.request_id,
                    "ok": True,
                    "status": "accepted",
                    "data": {
                        "session_id": active.session_id,
                        "connection_generation": active.generation,
                        "command_message_id": active.command_message_id,
                    },
                },
            },
        )
        if start_permit is None:
            raise AssertionError("STATUS lifecycle START fixture failed")
        self._tracker.acknowledge_publication(start_permit, True)
        if not self._tracker.record_nonterminal(
            status="running",
            result_reason="dispatch_started",
            evidence_sequence=1,
        ):
            raise AssertionError("STATUS running evidence fixture failed")


if __name__ == "__main__":
    unittest.main()
