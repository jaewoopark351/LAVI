#20260815_kpopmodder: Prevent ordinary Korean chat from reaching ChatClef submission.
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


HARD_GATE_FALSE_POSITIVES = [
    "캐나다 여행 얘기하자",
    "캐시 10개 필요해",
    "캐시 확인해줘",
    "다이아몬드 같은 노래 추천해줘",
    "오늘 사과 10개 먹었어",
]

SOFT_TRANSLATION_FALSE_POSITIVES = [
    "캐릭터 만들어줘",
    "캐릭터 10개 만들어줘",
    "채팅창 만들어줘",
]


class KoreanFalsePositiveSubmissionGuardTests(unittest.TestCase):
    def test_hard_gate_false_positives_do_not_call_translator(self):
        for text in HARD_GATE_FALSE_POSITIVES:
            with self.subTest(text=text):
                service = _ExplodingNaturalLanguageService()
                adapter = _RecordingAdapter()
                extension = MinecraftFabricChatClefExtension(
                    adapter=adapter,
                    natural_language_service=service,
                )
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )

                decision = router.route(text)

                self.assertFalse(decision.handled)
                self.assertEqual("no_minecraft_trigger", decision.reason)
                self.assertEqual(0, service.translate_calls)
                self.assertEqual([], adapter.requests)

    def test_soft_false_positives_may_translate_but_never_submit(self):
        for text in SOFT_TRANSLATION_FALSE_POSITIVES:
            with self.subTest(text=text):
                adapter = _RecordingAdapter()
                extension = MinecraftFabricChatClefExtension(adapter=adapter)
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )

                decision = router.route(text)

                self.assertFalse(decision.handled)
                self.assertEqual("unknown_intent", decision.reason)
                self.assertEqual([], adapter.requests)


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
            message="accepted",
            data={"command": request.command},
        )

    def get_status(self):
        return StatusSnapshotDTO(
            backend_id=self.backend_id,
            enabled=True,
            connected=True,
            lifecycle_state=BridgeLifecycleState.CONNECTED,
            detail="Fabric ChatClef bridge is connected.",
            details={"commands": {"active_request_id": None}},
        )


class _ExplodingNaturalLanguageService:
    def __init__(self):
        self.translate_calls = 0

    def translate(self, _text):
        self.translate_calls += 1
        raise AssertionError("translator should not run for hard gate negatives")


if __name__ == "__main__":
    unittest.main()
