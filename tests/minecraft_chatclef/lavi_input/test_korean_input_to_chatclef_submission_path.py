#20260815_kpopmodder: Cover LAVI Korean input reaching Fabric ChatClef submission.
from __future__ import annotations

import unittest

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


KOREAN_INPUT_TO_COMMAND_CASES = [
    ("다이아몬드 가져와줘", "get diamond 1"),
    ("다이아몬드를 가져와줘", "get diamond 1"),
    ("다이아몬드 캐줘", "get diamond 1"),
    ("돌 10개 가져와줘", "get stone 10"),
    ("돌을 10개 캐줘", "get stone 10"),
    ("조약돌 10개 캐줘", "get cobblestone 10"),
    ("레드스톤 5개 구해줘", "get redstone 5"),
    ("석탄 5개 캐와줘", "get coal 5"),
    ("철 10개 캐줘", "get iron_ingot 10"),
    ("다이아몬드 곡괭이 하나 가져와", "get diamond_pickaxe 1"),
]


class KoreanInputToChatClefSubmissionPathTests(unittest.TestCase):
    def test_lavi_korean_input_reaches_adapter_as_prefixless_command(self):
        for text, expected_command in KOREAN_INPUT_TO_COMMAND_CASES:
            with self.subTest(text=text):
                adapter = _RecordingAdapter()
                extension = MinecraftFabricChatClefExtension(adapter=adapter)
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )

                decision = router.route(text)

                self.assertTrue(decision.handled)
                self.assertEqual("minecraft_command_routed", decision.reason)
                self.assertEqual(
                    f"[Minecraft] command sent: {expected_command}",
                    decision.response_text,
                )
                self.assertEqual(1, len(adapter.requests))

                request = adapter.requests[0]
                self.assertEqual(expected_command, request.command)
                self.assertFalse(request.command.startswith("@"))
                self.assertEqual("lavi_chat_mic_router", request.source)
                self.assertTrue(request.request_id.startswith("lavi-input-ko-"))
                self.assertEqual(
                    "minecraft_fabric_chatclef",
                    request.metadata["input_route"],
                )
                self.assertEqual("ko", request.metadata["language"])

                natural_language = request.metadata["natural_language"]
                self.assertEqual(text, natural_language["original_text"])
                self.assertEqual(
                    expected_command,
                    natural_language["translation"]["command"],
                )

    def test_router_and_extension_translate_once_before_submission(self):
        adapter = _RecordingAdapter()
        service = _SingleUseNaturalLanguageService(
            _validated_get_translation()
        )
        extension = MinecraftFabricChatClefExtension(
            adapter=adapter,
            natural_language_service=service,
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 캐줘")

        self.assertTrue(decision.handled)
        self.assertEqual(1, service.translate_calls)
        self.assertEqual(1, len(adapter.requests))
        self.assertEqual("get diamond 1", adapter.requests[0].command)

    def test_non_minecraft_korean_chat_does_not_reach_adapter(self):
        adapter = _RecordingAdapter()
        service = _SingleUseNaturalLanguageService(
            _validated_get_translation()
        )
        extension = MinecraftFabricChatClefExtension(
            adapter=adapter,
            natural_language_service=service,
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("캐나다 여행 얘기하자")

        self.assertFalse(decision.handled)
        self.assertEqual("no_minecraft_trigger", decision.reason)
        self.assertEqual(0, service.translate_calls)
        self.assertEqual([], adapter.requests)

    def test_connected_precheck_blocks_disconnected_adapter_submission(self):
        adapter = _RecordingAdapter(connected=False)
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 캐줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_bridge_disconnected", decision.reason)
        self.assertEqual([], adapter.requests)

    def test_busy_precheck_blocks_second_adapter_submission(self):
        adapter = _RecordingAdapter(active_request_id="active-1")
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 캐줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_busy", decision.reason)
        self.assertEqual([], adapter.requests)

    def test_rejected_adapter_result_is_consumed_without_retry(self):
        adapter = _RecordingAdapter(
            result_status=CommandResultStatus.REJECTED,
            result_message="bridge rejected command",
        )
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 캐줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_rejected", decision.reason)
        self.assertIn("command rejected", decision.response_text)
        self.assertEqual(1, len(adapter.requests))

    def test_unknown_adapter_result_requires_reconciliation_without_retry(self):
        adapter = _RecordingAdapter(
            result_status=CommandResultStatus.UNKNOWN,
            result_message="send outcome unknown",
        )
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route("다이아몬드 캐줘")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_submission_outcome_unknown", decision.reason)
        self.assertTrue(decision.result["details"]["reconciliation_required"])
        self.assertEqual(1, len(adapter.requests))


class _RecordingAdapter:
    backend_id = "fabric_chatclef"

    def __init__(
        self,
        *,
        connected: bool = True,
        active_request_id: str = "",
        result_status: CommandResultStatus = CommandResultStatus.ACCEPTED,
        result_message: str = "accepted",
    ):
        self.connected = connected
        self.active_request_id = active_request_id
        self.result_status = result_status
        self.result_message = result_message
        self.requests = []

    def submit_command(self, request):
        self.requests.append(request)
        return CommandResultDTO(
            request_id=request.request_id,
            ok=self.result_status.ok,
            status=self.result_status,
            error_code=None,
            message=self.result_message,
            data={"command": request.command},
        )

    def get_status(self):
        lifecycle_state = (
            BridgeLifecycleState.CONNECTED
            if self.connected
            else BridgeLifecycleState.DISCONNECTED
        )
        detail = (
            "Fabric ChatClef bridge is connected."
            if self.connected
            else "Fabric ChatClef bridge client is not connected."
        )
        return StatusSnapshotDTO(
            backend_id=self.backend_id,
            enabled=True,
            connected=self.connected,
            lifecycle_state=lifecycle_state,
            detail=detail,
            details={
                "commands": {
                    "active_request_id": self.active_request_id or None,
                    "active_command": None,
                    "last_result": None,
                }
            },
        )


def _validated_get_translation() -> dict[str, object]:
    return {
        "status": "validated",
        "executable": True,
        "command": "get diamond 1",
        "intent": {
            "intent_type": "get_item",
            "item_phrase": "다이아몬드",
            "quantity": 1,
        },
        "resolved_target": "diamond",
        "reason_code": "validated",
        "message": "Korean command was translated to ChatClef DSL.",
        "data": {},
    }


class _SingleUseNaturalLanguageService:
    def __init__(self, translation):
        self.translation = dict(translation)
        self.translate_calls = 0

    def translate(self, _text):
        self.translate_calls += 1
        if self.translate_calls > 1:
            raise AssertionError("translation should run exactly once")
        return _TranslationResult(self.translation)


class _TranslationResult:
    def __init__(self, payload):
        self.payload = dict(payload)

    def to_dict(self):
        return dict(self.payload)


if __name__ == "__main__":
    unittest.main()
