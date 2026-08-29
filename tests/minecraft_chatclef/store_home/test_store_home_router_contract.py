#20260827_kpopmodder: Lock STORE_HOME admission, one-submit behavior, and private rollout.
#20260828_kpopmodder: Exercise the user-approved public rollout through the default registry.
from __future__ import annotations

import unittest
from dataclasses import replace

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import BridgeLifecycleState
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    StoreHomeCommandAdmission,
)
from plugins.Minecraft.fabric.chatclef.extension import MinecraftFabricChatClefExtension
from plugins.Minecraft.fabric.chatclef.input import MinecraftChatClefInputRouter


class StoreHomeRouterContractTests(unittest.TestCase):
    def test_public_rollout_reaches_adapter_with_default_registry(self):
        adapter = _RecordingAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("인벤토리 전부 집에 보관해")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_routed", decision.reason)
        self.assertEqual("store_home", decision.translation["command"])
        readiness = extension.korean_command_registry.spec(
            "store_home"
        ).readiness_axes
        self.assertTrue(readiness.gameplay_effect_verifiable)
        self.assertTrue(readiness.public_korean_enabled)
        self.assertEqual(1, len(adapter.requests))

    def test_public_test_gate_submits_prefixless_store_home_exactly_once(self):
        adapter = _RecordingAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("지금 아이템 다 집에 가져다 놔")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_routed", decision.reason)
        self.assertEqual(1, len(adapter.requests))
        request = adapter.requests[0]
        self.assertEqual("store_home", request.command)
        self.assertEqual("lavi_chat_mic_router", request.source)
        self.assertFalse(request.command.startswith("@"))
        self.assertTrue(request.request_id.startswith("lavi-input-ko-"))

    def test_chat_and_microphone_final_transcripts_share_one_canonical_route(self):
        cases = (
            ("chat", "인벤토리 전부 집에 보관해 주세요"),
            ("microphone", "인벤토리전부집에보관해"),
        )
        for physical_source, transcript in cases:
            with self.subTest(physical_source=physical_source):
                adapter = _RecordingAdapter(
                    result_status=CommandResultStatus.COMPLETED,
                    result_data=_completed_store_home_payload(),
                )
                extension = MinecraftFabricChatClefExtension(adapter=adapter)
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )

                decision = router.route(transcript)

                self.assertTrue(decision.handled)
                self.assertEqual("minecraft_command_routed", decision.reason)
                self.assertEqual(1, len(adapter.requests))
                request = adapter.requests[0]
                self.assertEqual("store_home", request.command)
                self.assertEqual("lavi_chat_mic_router", request.source)
                self.assertEqual(
                    transcript,
                    request.metadata["natural_language"]["original_text"],
                )
                self.assertEqual(
                    "COMPLETED",
                    decision.result["details"]["store_home_result"],
                )
                self.assertIn("집 보관 완료", decision.response_text)

    def test_direct_typed_command_preserves_typed_terminal_payload(self):
        adapter = _RecordingAdapter(
            result_status=CommandResultStatus.COMPLETED,
            result_data=_completed_store_home_payload(stored_items=33),
        )
        extension = MinecraftFabricChatClefExtension(adapter=adapter)

        result = extension.handle_command(
            {
                "request_id": "store-home-direct-typed-1",
                "command": "store_home",
                "source": "direct_typed",
            }
        )

        self.assertEqual(1, len(adapter.requests))
        self.assertEqual("store_home", adapter.requests[0].command)
        self.assertEqual("direct_typed", adapter.requests[0].source)
        self.assertEqual(
            _completed_store_home_payload(stored_items=33),
            result["details"],
        )

    def test_guarded_phrases_are_consumed_with_zero_submission(self):
        for phrase in (
            "아이템 정리해",
            "집에 아이템 넣지 마",
            "나중에 아이템 집에 넣어",
            "집에 보관하면 될까",
            "인벤토리 전부 집에 보관해?",
            "집에 보관하고 다이아 캐와",
            "감마 켜고 인벤토리 전부 집에 넣어",
            "다이아 3개 상자에 넣고 인벤토리 전부 집에 보관해",
        ):
            with self.subTest(phrase=phrase):
                adapter = _RecordingAdapter()
                extension = MinecraftFabricChatClefExtension(adapter=adapter)
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )

                decision = router.route(phrase)

                self.assertTrue(decision.handled)
                self.assertEqual("minecraft_translation_rejected", decision.reason)
                self.assertEqual([], adapter.requests)

    def test_store_home_source_admission_is_enforced_before_adapter_submission(self):
        blocked_adapter = _RecordingAdapter()
        blocked_extension = MinecraftFabricChatClefExtension(adapter=blocked_adapter)

        blocked = blocked_extension.handle_natural_language_command(
            {
                "request_id": "store-home-source-blocked",
                "text": "인벤토리 전부 집에 보관해",
                "source": "untrusted_source",
            }
        )

        self.assertFalse(blocked["ok"])
        self.assertEqual(
            "store_home_source_not_allowed",
            blocked["details"]["admission_reason"],
        )
        self.assertEqual([], blocked_adapter.requests)

        admitted_adapter = _RecordingAdapter()
        admitted_extension = MinecraftFabricChatClefExtension(adapter=admitted_adapter)

        admitted = admitted_extension.handle_natural_language_command(
            {
                "request_id": "store-home-source-admitted",
                "text": "인벤토리 전부 집에 보관해",
                "source": "direct_typed",
            }
        )

        self.assertTrue(admitted["ok"])
        self.assertEqual(1, len(admitted_adapter.requests))
        self.assertEqual("store_home", admitted_adapter.requests[0].command)

    def test_busy_and_disconnected_prechecks_never_submit_or_replay(self):
        scenarios = (
            (_RecordingAdapter(active_request_id="active-1"), "minecraft_command_busy"),
            (_RecordingAdapter(connected=False), "minecraft_bridge_disconnected"),
        )
        for adapter, reason in scenarios:
            with self.subTest(reason=reason):
                extension = MinecraftFabricChatClefExtension(adapter=adapter)
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )

                first = router.route("인벤토리 전부 집에 보관해")

                self.assertTrue(first.handled)
                self.assertEqual(reason, first.reason)
                self.assertEqual([], adapter.requests)

                original_active_request_id = adapter.active_request_id
                adapter.connected = True
                adapter.active_request_id = ""

                self.assertEqual([], adapter.requests)
                if reason == "minecraft_command_busy":
                    self.assertEqual("active-1", original_active_request_id)

                fresh = router.route("인벤토리 전부 집에 보관해")

                self.assertTrue(fresh.handled)
                self.assertEqual("minecraft_command_routed", fresh.reason)
                self.assertEqual(1, len(adapter.requests))

    def test_public_flag_cannot_skip_bridge_and_gameplay_readiness(self):
        spec = KoreanChatClefCommandRegistry().spec("store_home")
        incomplete_public_readiness = replace(
            spec,
            readiness_axes=replace(
                spec.readiness_axes,
                gameplay_effect_verifiable=False,
                public_korean_enabled=True,
            ),
        )

        decision = StoreHomeCommandAdmission().inspect(
            "store_home",
            "lavi_chat_mic_router",
            incomplete_public_readiness,
        )

        self.assertFalse(decision.allowed)
        self.assertEqual(
            "store_home_public_readiness_incomplete",
            decision.reason_code,
        )

class _RecordingAdapter:
    backend_id = "fabric_chatclef"

    def __init__(
        self,
        *,
        connected: bool = True,
        active_request_id: str = "",
        result_status: CommandResultStatus = CommandResultStatus.ACCEPTED,
        result_data: dict[str, object] | None = None,
    ):
        self.connected = connected
        self.active_request_id = active_request_id
        self.result_status = result_status
        self.result_data = dict(result_data or {})
        self.requests = []

    def submit_command(self, request):
        self.requests.append(request)
        return CommandResultDTO(
            request_id=request.request_id,
            ok=self.result_status.ok,
            status=self.result_status,
            message=self.result_status.value,
            data=self.result_data or {"command": request.command},
        )

    def get_status(self):
        lifecycle = (
            BridgeLifecycleState.CONNECTED
            if self.connected
            else BridgeLifecycleState.DISCONNECTED
        )
        return StatusSnapshotDTO(
            backend_id=self.backend_id,
            enabled=True,
            connected=self.connected,
            lifecycle_state=lifecycle,
            detail="test",
            details={
                "commands": {
                    "active_request_id": self.active_request_id or None,
                    "active_command": None,
                    "last_result": None,
                }
            },
        )


def _completed_store_home_payload(
    *,
    stored_items: int = 14,
) -> dict[str, object]:
    return {
        "operation": "store_home",
        "store_home_result": "COMPLETED",
        "stored_items": stored_items,
        "remaining_stacks": 0,
        "reason": "paired_delta_confirmed",
        "goal_satisfied": True,
    }


if __name__ == "__main__":
    unittest.main()
