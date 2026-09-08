#20260908_kpopmodder: Prove STATUS ownership only from exact real Chat and final-microphone ingress custody.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from input_core.input_event.adapters import ProviderBoundInputEventAdapter
from input_core.input_event.contracts import LaviInputEvent
from input_core.input_event.provenance import InputProviderSourceResolver
from llm_core.input_routing import RoutedInputDispatchCoordinator
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
from plugins.Minecraft.fabric.chatclef.input import MinecraftChatClefInputRouter
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean import (
    TrustedKoreanProofValidator,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQueryClassifier,
    CommandStatusRouteOwner,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.diagnostics import (
    CommandStatusRouteFailureDiagnostics,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptorFactory,
)

from .contextual_status_route_harness import ContextualStatusRouteHarness
from .trusted_status_input_fixture import issue_trusted_status_input


class ContextualStatusTrustedSourceMatrixTests(unittest.TestCase):
    def test_real_chat_and_final_microphone_sources_own_prefixless_status(self):
        expected_sources = (
            (False, ("lavi_chat_ui", "lavi_chat_ui", "chat_submit", True)),
            (
                True,
                ("voice_input_final", "VoiceInput", "final_transcript", True),
            ),
        )
        for index, (voice, expected_tuple) in enumerate(expected_sources):
            with self.subTest(voice=voice):
                proof_owner = object()
                registry, event, proof = issue_trusted_status_input(
                    voice=voice,
                    event_id=("c" if index == 0 else "d") * 32,
                    proof_owner=proof_owner,
                )
                self.assertEqual(
                    expected_tuple,
                    (event.source, event.provider_id, event.event_kind, event.final),
                )
                self.assertTrue(registry.validate_consumed_evidence(
                    event,
                    proof._consumed_evidence,
                ))
                harness = ContextualStatusRouteHarness(
                    proof_validator=TrustedKoreanProofValidator(
                        owner=proof_owner,
                    ).is_live,
                )
                try:
                    decision = harness.route(
                        event=event,
                        proof=proof,
                        descriptor=self._descriptor(),
                    )
                finally:
                    proof.close()

                self.assertTrue(decision.handled)
                self.assertEqual("command_status_query", decision.route_kind)
                self.assertEqual(
                    "다이아몬드 곡괭이 2개 구하는 중이야",
                    decision.response_text,
                )
                self.assertFalse(decision.result["command_submitted"])
                self.assertEqual(0, harness.ordinary_route_calls)

    def test_out_of_scope_source_rows_preserve_outer_conversation_fallthrough(self):
        rows = (
            (
                "partial_microphone",
                _event(
                    source="voice_input_partial",
                    provider_id="VoiceInput",
                    event_kind="partial_transcript",
                    final=False,
                    event_id="0" * 32,
                ),
            ),
            (
                "interim_exact_voice_tuple",
                _event(
                    source="voice_input_final",
                    provider_id="VoiceInput",
                    event_kind="final_transcript",
                    final=False,
                    event_id="1" * 32,
                ),
            ),
            ("butler", _provider_event("Butler", "2" * 32)),
            (
                "background",
                _provider_event("BackgroundAutomation", "3" * 32),
            ),
            (
                "chat_provider_cross_pair",
                _event(
                    source="lavi_chat_ui",
                    provider_id="VoiceInput",
                    event_kind="chat_submit",
                    final=True,
                    event_id="4" * 32,
                ),
            ),
            (
                "chat_kind_cross_pair",
                _event(
                    source="lavi_chat_ui",
                    provider_id="lavi_chat_ui",
                    event_kind="final_transcript",
                    final=True,
                    event_id="5" * 32,
                ),
            ),
            (
                "chat_voice_cross_pair",
                _event(
                    source="lavi_chat_ui",
                    provider_id="VoiceInput",
                    event_kind="final_transcript",
                    final=True,
                    event_id="6" * 32,
                ),
            ),
            (
                "interim_exact_chat_tuple",
                _event(
                    source="lavi_chat_ui",
                    provider_id="lavi_chat_ui",
                    event_kind="chat_submit",
                    final=False,
                    event_id="7" * 32,
                ),
            ),
            (
                "raw_gui_status_question",
                _event(
                    source="lavi_gui",
                    provider_id="minecraft_fabric_chatclef_ui",
                    event_kind="minecraft_raw_gui_submit",
                    final=True,
                    event_id="8" * 32,
                ),
            ),
            (
                "native_status_question",
                _event(
                    source="minecraft_native_chatclef",
                    provider_id="minecraft_native_chatclef",
                    event_kind="native_command",
                    final=True,
                    event_id="9" * 32,
                ),
            ),
        )
        for label, event in rows:
            with self.subTest(row=label):
                self._assert_outer_conversation_fallthrough(event)

    def test_raw_gui_command_keeps_its_existing_direct_submission_behavior(self):
        adapter = _RecordingCommandAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        request = {
            "request_id": "raw-status-source-matrix",
            "command": "get diamond 1",
            "source": "lavi_gui",
            "metadata": {},
        }
        event = _event(
            text=request["command"],
            source="lavi_gui",
            provider_id="minecraft_fabric_chatclef_ui",
            event_kind="minecraft_raw_gui_submit",
            final=True,
            event_id="a" * 32,
        )

        result = extension.handle_ui_command_with_feedback(
            request,
            input_event=event,
        )

        self.assertTrue(result["ok"])
        self.assertEqual(1, len(adapter.requests))
        self.assertEqual("get diamond 1", adapter.requests[0].command)
        self.assertEqual("lavi_gui", adapter.requests[0].source)

    def test_untrusted_lookalike_preserves_outer_conversation_fallthrough(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )
        _registry, event, proof = issue_trusted_status_input(
            voice=False,
            event_id="a" * 32,
            proof_owner=router,
        )
        lookalike = SimpleNamespace(
            text=event.text,
            source=event.source,
            provider_id=event.provider_id,
            event_kind=event.event_kind,
            final=event.final,
            event_id=event.event_id,
            fallback_payload=event.fallback_payload,
        )
        try:
            decision = router.route(
                lookalike,
                korean_eligibility_proof=proof,
            )
        finally:
            proof.close()

        self._assert_non_status_fallthrough(decision)

    def test_cross_event_cross_owner_and_spent_proofs_preserve_fallthrough(self):
        for index, case in enumerate(("cross_event", "cross_owner", "spent")):
            with self.subTest(case=case):
                router = MinecraftChatClefInputRouter(
                    extension=None,
                    log_callback=lambda _message: None,
                )
                proof_owner = object() if case == "cross_owner" else router
                _registry, event, proof = issue_trusted_status_input(
                    voice=index % 2 == 1,
                    event_id=chr(ord("b") + index) * 32,
                    proof_owner=proof_owner,
                )
                candidate = event
                if case == "cross_event":
                    candidate = _event(
                        text=event.text,
                        source=event.source,
                        provider_id=event.provider_id,
                        event_kind=event.event_kind,
                        final=event.final,
                        event_id="e" * 32,
                    )
                if case == "spent":
                    self.assertTrue(proof.close())
                try:
                    decision = router.route(
                        candidate,
                        korean_eligibility_proof=proof,
                    )
                finally:
                    proof.close()

                self._assert_non_status_fallthrough(decision)

    def test_throwing_current_event_proof_validator_fails_before_status_ownership(self):
        diagnostics = []
        generic_route_logs = []
        inspected = []
        owner = CommandStatusRouteOwner(
            extension=SimpleNamespace(
                inspect_command_feedback_status=lambda _query: inspected.append(
                    "inspected"
                )
            ),
            live_proof_validator=lambda _proof, _event: (_ for _ in ()).throw(
                RuntimeError("opaque")
            ),
            classifier=CommandStatusQueryClassifier(),
            response_renderer=CommandLifecycleResponseRenderer(),
            failure_diagnostics=CommandStatusRouteFailureDiagnostics(
                diagnostics.append,
            ),
        )
        router = MinecraftChatClefInputRouter(
            extension=None,
            command_status_route_owner=owner,
            log_callback=generic_route_logs.append,
        )
        _registry, event, proof = issue_trusted_status_input(
            voice=False,
            event_id="f" * 32,
            proof_owner=router,
        )
        try:
            decision = router.route(
                event,
                korean_eligibility_proof=proof,
            )
        finally:
            proof.close()

        self._assert_non_status_fallthrough(decision)
        self.assertEqual([], inspected)
        self.assertEqual(1, len(diagnostics))
        self.assertIn("stage=proof_validation", diagnostics[0])
        self.assertIn("exception_class=RuntimeError", diagnostics[0])
        self.assertFalse(
            any(
                "command_status_query_failed" in message
                or "route failed" in message
                for message in generic_route_logs
            )
        )

    @staticmethod
    def _descriptor():
        descriptor = CommandFeedbackDescriptorFactory().decode_registered_command_name_only(
            "get diamond_pickaxe 2",
            command_source="lavi_gui",
            event_id="0" * 32,
            provider_id="minecraft_fabric_chatclef_ui",
            event_kind="minecraft_raw_gui_submit",
        )
        if descriptor is None:
            raise AssertionError("GET descriptor fixture was not accepted")
        return descriptor

    def _route_owner(self, proof_validator):
        harness = ContextualStatusRouteHarness(proof_validator=proof_validator)
        harness.current_descriptor = self._descriptor()
        return harness.status_owner

    def _assert_outer_conversation_fallthrough(self, event: object) -> None:
        router = MinecraftChatClefInputRouter(
            extension=None,
            command_status_route_owner=_UnexpectedStatusOwner(),
            log_callback=lambda _message: None,
        )
        decision = router.route(event)
        self._assert_non_status_fallthrough(decision)

        outcome = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: self.fail(
                "outer conversation fallthrough must not publish STATUS"
            ),
            router=router,
            log_callback=lambda _message: None,
        ).dispatch(event)
        self.assertFalse(outcome.handled)
        self.assertFalse(outcome.suppress_response)
        self.assertIsNone(outcome.response)

    def _assert_non_status_fallthrough(self, decision: object) -> None:
        self.assertFalse(decision.handled)
        self.assertEqual("no_minecraft_trigger", decision.reason)
        self.assertNotEqual("command_status_query", decision.route_kind)
        self.assertEqual("", decision.response_text)
        self.assertFalse(decision.suppress_response)


class _UnexpectedStatusOwner:
    @staticmethod
    def try_route(_event: object, _proof: object):
        raise AssertionError("an out-of-scope source reached STATUS ownership")


class _RecordingCommandAdapter:
    backend_id = "fabric_chatclef"

    def __init__(self) -> None:
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


def _event(
    *,
    source: str,
    provider_id: str,
    event_kind: str,
    final: bool,
    event_id: str,
    text: str = "지금 뭐 해?",
) -> LaviInputEvent:
    return LaviInputEvent(
        text=text,
        source=source,
        provider_id=provider_id,
        event_kind=event_kind,
        final=final,
        event_id=event_id,
        fallback_payload=text,
    )


def _provider_event(provider_id: str, event_id: str) -> LaviInputEvent:
    return ProviderBoundInputEventAdapter(
        provider=SimpleNamespace(
            handle=SimpleNamespace(
                descriptor=SimpleNamespace(id=provider_id),
            )
        ),
        output_callback=lambda _event: None,
        source_resolver=InputProviderSourceResolver(),
        event_id_factory=lambda: event_id,
    ).adapt("지금 뭐 해?")


if __name__ == "__main__":
    unittest.main()
