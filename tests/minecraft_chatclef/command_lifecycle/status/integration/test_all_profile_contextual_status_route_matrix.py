#20260908_kpopmodder: Route every ordinary profile from one contextual prefixless STATUS question.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError
from types import SimpleNamespace

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean import (
    TrustedKoreanProofValidator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptor,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackLifecycleKindProfile,
    CommandFeedbackLifecycleKindProfileRegistry,
)
from plugins.Minecraft.fabric.chatclef.session.fabric_chatclef_session_registry import (
    FabricChatClefSessionRegistry,
)
from plugins.Minecraft.fabric.chatclef.transport.server.runtime.fabric_chatclef_server_component_graph import (
    FabricChatClefServerComponentGraph,
)

from .contextual_status_route_harness import ContextualStatusRouteHarness
from .trusted_status_input_fixture import issue_trusted_status_input


PROFILE_FIXTURES = {
    "attack": ("attack zombie 2", "대상을 공격하는 중이야"),
    "auto_deposit_trust": (
        "auto_deposit_trust area 16x16",
        "자동 보관 대상을 등록하는 중이야",
    ),
    "auto_deposit_trusted_list": (
        "auto_deposit_trusted_list",
        "자동 보관 위치를 확인하는 중이야",
    ),
    "auto_deposit_untrust": (
        "auto_deposit_untrust td_aaaaaaaaaaaaaaaaaaaaaaaa",
        "자동 보관 등록을 해제하는 중이야",
    ),
    "chatclef": ("chatclef ON", "ChatClef 상태를 바꾸는 중이야"),
    "deposit": ("deposit stone 2", "돌 2개 보관하는 중이야"),
    "deposit_all": ("deposit_all dirt 2", "흙 2개 보관하는 중이야"),
    "equip": ("equip iron_helmet", "철 투구 장착하는 중이야"),
    "follow": ("follow Steve", "Steve를 따라가는 중이야"),
    "food": ("food 10", "허기를 10만큼 채울 음식을 모으는 중이야"),
    "gamer": ("gamer", "게임을 공략하는 중이야"),
    "gamma": ("gamma 1.25", "밝기를 1.25로 바꾸는 중이야"),
    "get": (
        "get diamond_pickaxe 2",
        "다이아몬드 곡괭이 2개 구하는 중이야",
    ),
    "give": (
        "give Steve diamond_pickaxe 2",
        "Steve에게 다이아몬드 곡괭이 2개 건네는 중이야",
    ),
    "goto": ("goto 1 64 3", "좌표 1, 64, 3으로 가는 중이야"),
    "hero": ("hero", "주변 적을 정리하는 중이야"),
    "idle": ("idle", "가만히 기다리는 중이야"),
    "locate_structure": (
        "locate_structure stronghold",
        "요새를 찾는 중이야",
    ),
    "meat": ("meat 8", "허기를 8만큼 채울 고기를 모으는 중이야"),
    "overlay": ("overlay off", "오버레이 상태를 바꾸는 중이야"),
    "reload_settings": ("reload_settings", "설정을 다시 불러오는 중이야"),
    "resetmemory": (
        "resetmemory",
        "ChatClef의 기억을 초기화하는 중이야",
    ),
    "scan": ("scan STONE", "요청한 대상을 찾는 중이야"),
    "stop": ("stop", "중지 요청을 처리 중이야"),
    "store_home": ("store_home", "아이템을 집에 정리하는 중이야"),
    "자동보관등록": (
        "자동보관등록 영역 16x16",
        "주변 보관함을 등록하는 중이야",
    ),
}


class AllProfileContextualStatusRouteMatrixTests(unittest.TestCase):
    def test_all_25_ordinary_profiles_route_exact_running_phrase_and_stop_bypasses(self):
        commands = KoreanChatClefCommandRegistry().command_names()
        self.assertEqual(commands, tuple(PROFILE_FIXTURES))
        self.assertEqual(26, len(commands))

        proof_owner = object()
        _registry, event, proof = issue_trusted_status_input(
            voice=False,
            event_id="8" * 32,
            proof_owner=proof_owner,
        )
        harness = ContextualStatusRouteHarness(
            proof_validator=TrustedKoreanProofValidator(
                owner=proof_owner,
            ).is_live,
        )
        descriptors = CommandFeedbackDescriptorFactory()
        routed_profiles = 0
        self.assertEqual("지금 뭐 해?", event.text)
        try:
            for command_name, (command, expected_text) in PROFILE_FIXTURES.items():
                with self.subTest(command_name=command_name):
                    descriptor = descriptors.decode_registered_command_name_only(
                        command,
                        command_source="lavi_gui",
                        event_id="9" * 32,
                        provider_id="minecraft_fabric_chatclef_ui",
                        event_kind="minecraft_raw_gui_submit",
                    )
                    self.assertIs(type(descriptor), CommandFeedbackDescriptor)
                    with self.assertRaises(FrozenInstanceError):
                        descriptor.command_name = "changed"
                    self.assertEqual(
                        expected_text,
                        harness.renderer.render_status(
                            SimpleNamespace(
                                state="running",
                                descriptor=descriptor,
                            )
                        ),
                    )

                    decision = harness.route(
                        event=event,
                        proof=proof,
                        descriptor=descriptor,
                    )
                    self.assertIsNotNone(harness.last_snapshot)
                    if command_name == "stop":
                        self.assertFalse(decision.handled)
                        self.assertEqual(
                            "command_status_conversational_fallthrough",
                            decision.reason,
                        )
                        self.assertNotEqual(
                            "command_status_query",
                            decision.route_kind,
                        )
                        self.assertFalse(harness.last_snapshot.query_matched)
                        continue

                    routed_profiles += 1
                    self.assertEqual(
                        expected_text,
                        harness.renderer.render_status(
                            harness.last_snapshot,
                            harness.last_query,
                        ),
                    )
                    self.assertTrue(decision.handled)
                    self.assertEqual("command_status_query", decision.route_kind)
                    self.assertEqual("command_status", decision.response_kind)
                    self.assertEqual(expected_text, decision.response_text)
                    self.assertEqual(
                        CommandFeedbackLifecycleSnapshot.RUNNING,
                        harness.last_snapshot.state,
                    )
                    self.assertIs(
                        descriptor,
                        harness.last_snapshot.descriptor,
                    )
                    self.assertTrue(harness.last_snapshot.owner_present)
                    self.assertTrue(harness.last_snapshot.query_matched)
                    self.assertFalse(decision.result["command_submitted"])
        finally:
            proof.close()

        self.assertEqual(25, routed_profiles)
        self.assertEqual(0, harness.ordinary_route_calls)

    def test_all_25_ordinary_profiles_route_from_reconciled_running_lifecycle(self):
        proof_owner = object()
        _registry, event, proof = issue_trusted_status_input(
            voice=False,
            event_id="7" * 32,
            proof_owner=proof_owner,
        )
        descriptors = CommandFeedbackDescriptorFactory()
        routed_profiles = 0
        try:
            for command_name, (command, expected_text) in PROFILE_FIXTURES.items():
                if command_name == "stop":
                    continue
                with self.subTest(command_name=command_name):
                    descriptor = descriptors.decode_registered_command_name_only(
                        command,
                        command_source="lavi_gui",
                        event_id="6" * 32,
                        provider_id="minecraft_fabric_chatclef_ui",
                        event_kind="minecraft_raw_gui_submit",
                    )
                    self.assertIs(type(descriptor), CommandFeedbackDescriptor)
                    graph, active, deferred_start = _reconciled_running_graph(
                        descriptor=descriptor,
                        descriptor_factory=descriptors,
                    )
                    active_identity = _active_identity(active)
                    harness = ContextualStatusRouteHarness(
                        proof_validator=TrustedKoreanProofValidator(
                            owner=proof_owner,
                        ).is_live,
                        status_inspector=graph.command_feedback_api.inspect_status,
                    )

                    decision = harness.route(
                        event=event,
                        proof=proof,
                        descriptor=descriptor,
                    )

                    routed_profiles += 1
                    self.assertTrue(decision.handled)
                    self.assertEqual("command_status_query", decision.route_kind)
                    self.assertEqual("command_status", decision.response_kind)
                    self.assertEqual(expected_text, decision.response_text)
                    self.assertFalse(decision.result["command_submitted"])
                    self.assertEqual(0, harness.ordinary_route_calls)
                    self.assertIsNotNone(harness.last_snapshot)
                    self.assertEqual(
                        CommandFeedbackLifecycleSnapshot.RUNNING,
                        harness.last_snapshot.state,
                    )
                    self.assertEqual(
                        "dispatch_started",
                        harness.last_snapshot.result_reason,
                    )
                    self.assertIs(descriptor, harness.last_snapshot.descriptor)
                    self.assertTrue(harness.last_snapshot.query_matched)
                    self.assertEqual(
                        active_identity,
                        _active_identity(
                            graph.connection_ownership.active_command_owner
                        ),
                    )
                    acknowledgement = (
                        decision.response_publication_acknowledgement
                    )
                    self.assertIsNotNone(acknowledgement)
                    if deferred_start is not None:
                        start_acknowledgement = (
                            graph.command_feedback_api.claim_start(
                                deferred_start,
                                _accepted_submission_result(active),
                            )
                        )
                        self.assertIsNotNone(start_acknowledgement)
                        self.assertTrue(
                            start_acknowledgement.acknowledge(published=True)
                        )
                    self.assertTrue(acknowledgement.acknowledge(published=True))

        finally:
            proof.close()

        self.assertEqual(25, routed_profiles)


class _Diagnostics:
    def info(self, _message: str) -> None:
        pass

    def warning(self, _message: str) -> None:
        pass


def _reconciled_running_graph(
    *,
    descriptor: CommandFeedbackDescriptor,
    descriptor_factory: CommandFeedbackDescriptorFactory,
):
    graph = FabricChatClefServerComponentGraph(
        config=SimpleNamespace(
            reconcile_stale_deposit_to_unknown_enabled=False,
            startup_timeout_sec=1.0,
        ),
        session_registry=FabricChatClefSessionRegistry(),
        diagnostics=_Diagnostics(),
        lifecycle=SimpleNamespace(
            stopping=False,
            loop=None,
            record_client_error=lambda _error: None,
            bind=lambda **_values: None,
        ),
        message_id_factory=lambda: "generated-message",
        now_ms=lambda: 1,
    )
    grant = CommandFeedbackAdmissionCoordinator(
        live_proof_validator=lambda _proof, _event: False,
        descriptor_factory=descriptor_factory,
    ).issue_descriptor(descriptor)
    websocket = object()
    if (
        grant is None
        or not graph.connection_ownership.try_activate(
            websocket=websocket,
            session_id="all-profile-status-session",
        ).accepted
        or not graph.command_feedback_api.reserve(grant)
    ):
        raise AssertionError("ordinary profile admission fixture failed")
    with graph.command_lock:
        active = graph.connection_ownership.begin_command(
            request_id="all-profile-status-request",
            command_message_id="all-profile-status-message",
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
        raise AssertionError("ordinary profile lifecycle binding failed")
    lifecycle = CommandFeedbackLifecycleKindProfileRegistry().profile(
        descriptor.command_name
    )
    if (
        lifecycle.terminal_trigger
        == CommandFeedbackLifecycleKindProfile.ACCEPTED_SUBMISSION_CAUTION
    ):
        # A callback-less immediate command stages its cautious terminal when
        # START is claimed, so reconcile the real RUNNING evidence first and
        # drain START before the already-issued STATUS permit afterward.
        graph.command_result_handler.handle(
            websocket,
            _running_envelope(active),
        )
        return graph, active, grant
    start_acknowledgement = graph.command_feedback_api.claim_start(
        grant,
        _accepted_submission_result(active),
    )
    if start_acknowledgement is None or not start_acknowledgement.acknowledge(
        published=True
    ):
        raise AssertionError("ordinary profile START publication failed")
    graph.command_result_handler.handle(
        websocket,
        _running_envelope(active),
    )
    return graph, active, None


def _accepted_submission_result(active: object) -> dict[str, object]:
    return {
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
    }


def _running_envelope(active: object) -> BridgeEnvelopeDTO:
    result = CommandResultDTO(
        request_id=active.request_id,
        ok=True,
        status=CommandResultStatus.RUNNING,
        data={
            "result_reason": "dispatch_started",
            "evidence_sequence": 1,
        },
    )
    return BridgeEnvelopeDTO(
        protocol_version=1,
        message_type=BridgeMessageType.COMMAND_RESULT,
        message_id="all-profile-status-running-result",
        correlation_id=active.command_message_id,
        session_id=active.session_id,
        timestamp_ms=1,
        payload=result.to_dict(),
    )


def _active_identity(active: object) -> tuple[object, ...]:
    return (
        active.session_id,
        active.generation,
        active.request_id,
        active.command_message_id,
    )


if __name__ == "__main__":
    unittest.main()
