#20260905_kpopmodder: Verify proof-gated scoped crafting route integration and legacy isolation.
from __future__ import annotations

import unittest
from unittest.mock import Mock, sentinel

from input_core.input_event.contracts import LaviInputEvent
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
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults import (
    GenericCraftingDefaultsActivationState,
    GenericCraftingDefaultsAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing import (
    GenericCraftingActivationStage,
    GenericCraftingCandidateOwnershipStage,
    GenericCraftingDispatchLifecycle,
    GenericCraftingReconciliationStage,
    GenericCraftingRoutePipeline,
    GenericCraftingSubmissionStage,
    GenericCraftingTranslationStage,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing.activation import (
    GenericCraftingActivationAdmissionStage,
    GenericCraftingScopedCapabilityAvailabilityStage,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing.candidate import (
    GenericCraftingCandidateDetectionStage,
    GenericCraftingCandidateOwnershipClassificationStage,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing.composition import (
    GenericCraftingRouteCompatibilityInstaller,
    GenericCraftingRouteComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing.generic_crafting_route_failure_handler import (
    GenericCraftingRouteFailureHandler,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing.lifecycle import (
    GenericCraftingDispatchCloseLifecycle,
    GenericCraftingReceiptCleanup,
    GenericCraftingRouteExecutionLifecycle,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing.submission import (
    GenericCraftingSubmissionDelivery,
    GenericCraftingSubmissionReadinessStage,
    GenericCraftingSubmissionReconciliationObserver,
    GenericCraftingSubmissionResultProjector,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.routing.translation import (
    GenericCraftingTranslationInvoker,
    GenericCraftingTranslationResultStage,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class GenericCraftingDefaultsRouteTests(unittest.TestCase):
    def test_route_owner_facade_composes_independent_feature_stages(self):
        fixture = _RouteFixture(
            _event("다락문 만들어줘", "chat", "0" * 32)
        )
        owner = fixture.router._generic_crafting_defaults_route_owner

        self.assertIsInstance(
            owner._component_graph,
            GenericCraftingRouteComponentGraph,
        )
        self.assertIsInstance(
            owner._component_graph._compatibility_installer,
            GenericCraftingRouteCompatibilityInstaller,
        )
        self.assertIsInstance(
            owner._candidate_ownership_stage,
            GenericCraftingCandidateOwnershipStage,
        )
        self.assertIsInstance(
            owner._candidate_detection_stage,
            GenericCraftingCandidateDetectionStage,
        )
        self.assertIsInstance(
            owner._candidate_ownership_classification_stage,
            GenericCraftingCandidateOwnershipClassificationStage,
        )
        self.assertIsInstance(
            owner._reconciliation_stage,
            GenericCraftingReconciliationStage,
        )
        self.assertIsInstance(
            owner._activation_stage,
            GenericCraftingActivationStage,
        )
        self.assertIsInstance(
            owner._capability_stage,
            GenericCraftingScopedCapabilityAvailabilityStage,
        )
        self.assertIsInstance(
            owner._activation_admission_stage,
            GenericCraftingActivationAdmissionStage,
        )
        self.assertIsInstance(
            owner._translation_stage,
            GenericCraftingTranslationStage,
        )
        self.assertIsInstance(
            owner._translation_invoker,
            GenericCraftingTranslationInvoker,
        )
        self.assertIsInstance(
            owner._translation_result_stage,
            GenericCraftingTranslationResultStage,
        )
        self.assertIsInstance(
            owner._submission_stage,
            GenericCraftingSubmissionStage,
        )
        self.assertIsInstance(
            owner._submission_readiness_stage,
            GenericCraftingSubmissionReadinessStage,
        )
        self.assertIsInstance(
            owner._submission_delivery,
            GenericCraftingSubmissionDelivery,
        )
        self.assertIsInstance(
            owner._submission_reconciliation_observer,
            GenericCraftingSubmissionReconciliationObserver,
        )
        self.assertIsInstance(
            owner._submission_result_projector,
            GenericCraftingSubmissionResultProjector,
        )
        self.assertIsInstance(
            owner._dispatch_lifecycle,
            GenericCraftingDispatchLifecycle,
        )
        self.assertIsInstance(
            owner._receipt_cleanup,
            GenericCraftingReceiptCleanup,
        )
        self.assertIsInstance(
            owner._dispatch_close_lifecycle,
            GenericCraftingDispatchCloseLifecycle,
        )
        self.assertIsInstance(
            owner._failure_handler,
            GenericCraftingRouteFailureHandler,
        )
        self.assertIsInstance(
            owner._execution_lifecycle,
            GenericCraftingRouteExecutionLifecycle,
        )
        self.assertIsInstance(owner._pipeline, GenericCraftingRoutePipeline)
        self.assertIs(fixture.registry, owner.activation_registry)

    def test_route_execution_lifecycle_projects_failure_and_cleans_receipt_once(self):
        failure_handler = Mock()
        failure_handler.handle.return_value = sentinel.failure_decision
        receipt_cleanup = Mock()
        lifecycle = GenericCraftingRouteExecutionLifecycle(
            failure_handler=failure_handler,
            receipt_cleanup=receipt_cleanup,
        )
        failure = RuntimeError("translation failed")

        def fail_after_activation(capture_receipt):
            capture_receipt(sentinel.receipt)
            raise failure

        result = lifecycle.execute(fail_after_activation)

        self.assertIs(sentinel.failure_decision, result)
        failure_handler.handle.assert_called_once_with(failure)
        receipt_cleanup.abandon_if_live.assert_called_once_with(
            sentinel.receipt
        )

    def test_five_defaults_submit_exact_dsl_for_chat_and_final_voice(self):
        cases = {
            "다락문 만들어줘": "get trapdoor 1",
            "지도 만들어줘": "get map 1",
            "압력판 만들어줘": "get wooden_pressure_plate 1",
            "발판 만들어줘": "get wooden_pressure_plate 1",
            "버튼 만들어줘": "get wooden_button 1",
        }
        index = 1
        for source in ("chat", "voice"):
            for text, expected_command in cases.items():
                with self.subTest(source=source, text=text):
                    event = _event(text, source, f"{index:032x}")
                    index += 1
                    fixture = _RouteFixture(event)

                    decision = fixture.route()

                    self.assertTrue(decision.handled)
                    self.assertEqual("minecraft_command_routed", decision.reason)
                    self.assertEqual(expected_command, decision.translation["command"])
                    self.assertEqual(1, len(fixture.adapter.requests))
                    self.assertEqual(
                        expected_command,
                        fixture.adapter.requests[0].command,
                    )
                    self.assertEqual(event.source, fixture.adapter.requests[0].source)
                    self.assertIn("수집 명령을 제출했어요.", decision.response_text)
                    self.assertNotIn("준비하도록", decision.response_text)
                    self.assertEqual(1, fixture.registry.record_count)
                    receipt = next(
                        record["receipt"]
                        for record in fixture.registry._records.values()
                    )
                    self.assertEqual(
                        GenericCraftingDefaultsActivationState.SPENT.value,
                        fixture.registry.state(receipt),
                    )
                    fixture.close()
                    self.assertEqual(0, fixture.registry.record_count)

    def test_no_proof_preserves_legacy_unhandled_behavior(self):
        event = _event("다락문 만들어줘", "chat", "a" * 32)
        fixture = _RouteFixture(event)

        decision = fixture.router.route(event)

        self.assertFalse(decision.handled)
        self.assertEqual("unknown_intent", decision.reason)
        self.assertEqual([], fixture.adapter.requests)
        self.assertEqual(0, fixture.registry.record_count)

    def test_raw_translation_and_normalized_intent_text_remain_distinct(self):
        raw_text = "  지도, ２개 만들어줘  "
        fixture = _RouteFixture(_event(raw_text, "chat", "f" * 32))

        decision = fixture.route()

        self.assertEqual("get map 2", decision.translation["command"])
        request = fixture.adapter.requests[0]
        natural_language = request.metadata["natural_language"]
        self.assertEqual(raw_text, natural_language["raw_event_text"])
        self.assertEqual(raw_text.strip(), natural_language["translation_input_text"])
        self.assertEqual(raw_text.strip(), natural_language["original_text"])
        self.assertEqual(
            "지도 2개 만들어줘",
            decision.translation["intent"]["original_text"],
        )

    def test_invalid_quantity_is_handled_without_submit_or_llm_downgrade(self):
        texts = [
            "다락문 2개 3개 만들어줘",
            "지도 두 개 세 개 만들어줘",
            "압력판 ٢개 만들어줘",
            "발판 -1개 만들어줘",
            "버튼 2147483648개 만들어줘",
        ]
        for index, text in enumerate(texts, start=30):
            with self.subTest(text=text):
                fixture = _RouteFixture(_event(text, "chat", f"{index:032x}"))

                decision = fixture.route()

                self.assertTrue(decision.handled)
                self.assertEqual(
                    "minecraft_generic_crafting_defaults_rejected",
                    decision.reason,
                )
                self.assertEqual([], fixture.adapter.requests)
                self.assertEqual(0, fixture.registry.record_count)

    def test_forged_proof_and_non_craft_verbs_do_not_activate_profile(self):
        event = _event("지도 만들어줘", "chat", "b" * 32)
        fixture = _RouteFixture(event)

        forged = fixture.router.route(
            event,
            korean_eligibility_proof=object(),
        )
        acquire_fixture = _RouteFixture(
            _event("다락문 구해줘", "chat", "c" * 32)
        )
        acquire = acquire_fixture.route()

        self.assertFalse(forged.handled)
        self.assertFalse(acquire.handled)
        self.assertEqual([], fixture.adapter.requests)
        self.assertEqual([], acquire_fixture.adapter.requests)

    def test_specific_aliases_keep_the_existing_unscoped_translation(self):
        cases = {
            "철 다락문 만들어줘": "get iron_trapdoor 1",
            "참나무 다락문 만들어줘": "get oak_trapdoor 1",
            "빈 지도 만들어줘": "get map 1",
            "지도 제작대 만들어줘": "get cartography_table 1",
            "돌 압력판 만들어줘": "get stone_pressure_plate 1",
            "참나무 압력판 만들어줘": "get oak_pressure_plate 1",
            "돌 버튼 만들어줘": "get stone_button 1",
            "참나무 버튼 만들어줘": "get oak_button 1",
        }
        for index, (text, expected) in enumerate(cases.items(), start=60):
            with self.subTest(text=text):
                fixture = _RouteFixture(_event(text, "chat", f"{index:032x}"))

                decision = fixture.route()

                self.assertTrue(decision.handled)
                self.assertEqual("minecraft_command", decision.route_kind)
                self.assertEqual(expected, fixture.adapter.requests[0].command)
                self.assertEqual(0, fixture.registry.record_count)

    def test_direct_gui_handler_cannot_activate_the_scoped_profile(self):
        adapter = _RecordingAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)

        result = extension.handle_natural_language_command(
            {
                "text": "버튼 만들어줘",
                "source": "lavi_gui_korean",
            }
        )

        self.assertFalse(result["ok"])
        self.assertEqual([], adapter.requests)
        self.assertEqual(0, extension.generic_crafting_defaults_activation_registry.record_count)

    def test_same_activation_replay_does_not_submit_twice(self):
        fixture = _RouteFixture(
            _event("지도 만들어줘", "chat", "d" * 32)
        )

        first = fixture.route()
        second = fixture.route()

        self.assertEqual("minecraft_command_routed", first.reason)
        self.assertEqual(
            "minecraft_generic_crafting_defaults_rejected",
            second.reason,
        )
        self.assertEqual(1, len(fixture.adapter.requests))

    def test_stop_control_owner_runs_before_feature_b_owner(self):
        event = _event("다락문 만들어줘", "chat", "e" * 32)
        proof = object()
        stop_owner = _StopOwner()
        adapter = _RecordingAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        accepted = {proof: event}
        admission = GenericCraftingDefaultsAdmission(
            extension.get_generic_crafting_defaults_activation_registry(),
            lambda candidate_proof, candidate_event: (
                accepted.get(candidate_proof) is candidate_event
            ),
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            stop_control_route_owner=stop_owner,
            generic_crafting_defaults_admission=admission,
            log_callback=lambda _message: None,
        )

        decision = router.route(event, korean_eligibility_proof=proof)

        self.assertEqual("stop_hook_consumed", decision.reason)
        self.assertEqual([(event, proof)], stop_owner.calls)
        self.assertEqual([], adapter.requests)
        self.assertEqual(0, admission.activation_registry.record_count)


class _RouteFixture:
    def __init__(self, event: LaviInputEvent):
        self.event = event
        self.proof = object()
        self.adapter = _RecordingAdapter()
        self.extension = MinecraftFabricChatClefExtension(adapter=self.adapter)
        self.registry = (
            self.extension.get_generic_crafting_defaults_activation_registry()
        )
        self.accepted = {self.proof: self.event}
        self.admission = GenericCraftingDefaultsAdmission(
            self.registry,
            lambda proof, candidate_event: self.accepted.get(proof)
            is candidate_event,
        )
        self.router = MinecraftChatClefInputRouter(
            extension=self.extension,
            generic_crafting_defaults_admission=self.admission,
            log_callback=lambda _message: None,
        )

    def route(self):
        return self.router.route(
            self.event,
            korean_eligibility_proof=self.proof,
        )

    def close(self):
        self.router.close_generic_crafting_defaults_dispatch(self.proof)


class _StopOwner:
    def __init__(self):
        self.calls = []

    def try_route(self, event, proof):
        self.calls.append((event, proof))
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="stop_hook_consumed",
            response_text="stopped",
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


def _event(text: str, source: str, event_id: str) -> LaviInputEvent:
    if source == "voice":
        source_name = "voice_input_final"
        provider_id = "VoiceInput"
        event_kind = "final_transcript"
    else:
        source_name = "lavi_chat_ui"
        provider_id = "lavi_chat_ui"
        event_kind = "chat_submit"
    return LaviInputEvent(
        text=text,
        source=source_name,
        provider_id=provider_id,
        event_kind=event_kind,
        final=True,
        event_id=event_id,
        fallback_payload=text,
    )


if __name__ == "__main__":
    unittest.main()
