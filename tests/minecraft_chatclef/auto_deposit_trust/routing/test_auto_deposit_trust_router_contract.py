#20260905_kpopmodder: Exercise trusted H5 routing, no-fallback rejection, and one-shot submission.
from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest import mock

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
from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


class AutoDepositTrustRouterContractTests(unittest.TestCase):
    def test_trusted_chat_submits_one_prefixless_command_and_spends_event(self):
        adapter = _recording_adapter()
        service = mock.Mock(wraps=ChatClefNaturalLanguageService())
        extension = MinecraftFabricChatClefExtension(
            adapter=adapter,
            natural_language_service=service,
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )
        event = _chat_event(
            "@auto_deposit_trust 반경 16x16",
            "a" * 32,
        )

        first = router.route(event)
        second = router.route(event)

        self.assertTrue(first.handled)
        self.assertEqual("minecraft_command_routed", first.reason)
        self.assertEqual(
            "[Minecraft] 주변 16×16 자동 보관 대상 등록 명령을 제출했어요.",
            first.response_text,
        )
        self.assertEqual(1, service.translate.call_count)
        self.assertEqual(1, len(adapter.requests))
        request = adapter.requests[0]
        self.assertEqual("auto_deposit_trust area 16x16", request.command)
        self.assertEqual("lavi_chat_ui", request.source)
        self.assertEqual(event.event_id, request.metadata["input_event"]["event_id"])
        self.assertEqual(
            event.text,
            request.metadata["natural_language"]["original_text"],
        )
        self.assertEqual(
            "auto_deposit_trust area 16x16",
            request.metadata["natural_language"]["translation_input_text"],
        )
        self.assertNotIn("route_claim", request.metadata)
        self.assertTrue(second.handled)
        self.assertEqual("auto_deposit_trust_duplicate_input_event", second.reason)
        self.assertEqual(0, router.auto_deposit_trust_claim_registry.issued_count)

    def test_untrusted_raw_h5_and_invalid_raw_control_never_translate(self):
        extension = MinecraftFabricChatClefExtension(adapter=_recording_adapter())
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        raw = router.route("자동보관등록 영역 16x16")
        controlled = router.route(
            _chat_event("자동보관등록 영역 16x16\n", "b" * 32)
        )

        self.assertEqual("auto_deposit_trust_input_source_not_allowed", raw.reason)
        self.assertEqual(
            "auto_deposit_trust_input_raw_control_not_allowed",
            controlled.reason,
        )
        self.assertEqual(0, router.auto_deposit_trust_claim_registry.claimed_count)

    def test_control_inside_h5_marker_cannot_reopen_normal_llm_fallback(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )

        for index, text in enumerate(
            (
                "@auto_deposit_\x00trust area 16x16",
                "자동보\x00관등록 영역 16x16",
                "@auto_deposit_\u200btrust area 16x16",
                "자동보\u200b관등록 영역 16x16",
                "auto_deposit_trust\u00a0area 16x16",
                "주변\u200916x16 상자를 자동 보관 대상으로 등록해",
                "주변\u300016x16 상자를 자동 보관 대상으로 등록해",
                "auto_deposit_trust area 16\u00a0x\u00a016",
                "자동보관등록 영역 16\u2009곱하기\u300016",
                "주변 16\u202fX\u205f16 상자 전부 자동보관 등록해 주세요",
                "\u00a0auto_deposit_trust area 16x16\u00a0",
                "\u3000자동보관등록 영역 16x16\u3000",
            )
        ):
            with self.subTest(text=text):
                decision = router.route(
                    _chat_event(text, f"{index + 10:032x}")
                )
                self.assertTrue(decision.handled)
                self.assertEqual(
                    "auto_deposit_trust_input_raw_control_not_allowed",
                    decision.reason,
                )

    def test_extension_unavailable_is_handled_and_ordinary_chat_is_not(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )
        h5 = router.route(_chat_event("자동보관등록 영역 16x16", "c" * 32))
        ordinary = router.route(_chat_event("오늘 뭐 먹지?", "d" * 32))

        self.assertTrue(h5.handled)
        self.assertEqual("auto_deposit_trust_extension_unavailable", h5.reason)
        self.assertFalse(ordinary.handled)
        self.assertEqual(0, router.auto_deposit_trust_claim_registry.issued_count)

    def test_h5_route_rejects_a_validated_non_h5_translation(self):
        adapter = _recording_adapter()
        service = mock.Mock()
        service.translate.return_value = ChatClefTranslationResultDTO.validated(
            command="get stone 1",
            intent=ChatClefIntentDTO(
                intent_type=ChatClefIntentType.GET_ITEM,
                item_phrase="돌",
                quantity=1,
            ),
            resolved_target="stone",
        )
        extension = MinecraftFabricChatClefExtension(
            adapter=adapter,
            natural_language_service=service,
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        decision = router.route(
            _chat_event("자동보관등록 영역 16x16", "e" * 32)
        )

        self.assertTrue(decision.handled)
        self.assertEqual(
            "auto_deposit_trust_translation_mismatch",
            decision.reason,
        )
        self.assertEqual([], adapter.requests)
        self.assertEqual(0, router.auto_deposit_trust_claim_registry.issued_count)

    def test_every_non_exact_at_or_dangerous_form_is_spent_without_submission(self):
        forms = (
            "AUTO_DEPOSIT_TRUST area 16x16",
            "Auto_Deposit_Trust AREA 16X16",
            "auto_deposit_trust AREA 16x16",
            "@auto_deposit_trust  area 16x16",
            "@auto_deposit_trust 반경 16 곱하기 16",
            "@auto_deposit_trust area 16x16; stop",
            "@auto_deposit_#trust area 16x16",
            '"@auto_deposit_trust area 16x16"',
            "자동보\\관등록 영역 16x16",
            "자동보/관등록 영역 16x16",
            "자동보:관등록 영역 16x16",
            "자동보=관등록 영역 16x16",
            "자동보[관]등록 영역 16x16",
            "자동보--관등록 영역 16x16",
            "자동보?관등록 영역 16x16",
            "자동보$관등록 영역 16x16",
            "자동보(관)등록 영역 16x16",
            "@auto_deposit|trust area 16x16",
            "주?변 16x16 상?자를 자동 보?관 대상으로 등록해",
            "자동보™관등록 영역 16x16",
            "자동보℡관등록 영역 16x16",
            "자동보₹관등록 영역 16x16",
            "@auto_™deposit_trust area 16x16",
            "자동보ß관등록 영역 16x16",
            "자동보ſ관등록 영역 16x16",
            "자동보K관등록 영역 16x16",
            "자동보ﬀ관등록 영역 16x16",
            "ａｕｔｏ＿ｄｅｐｏｓｉｔ＿ｔｒｕｓｔ area 16x16",
            "auto_deposit_trust area ⑯x⑯",
            "주변 ⑯x⑯ 상자를 자동 보관 대상으로 등록해",
            "auto_deposit_trust，area 16x16",
            "auto_deposit_trust area １６x１６",
            "auto_deposit_trust area 16x16!",
        )

        for index, text in enumerate(forms, start=32):
            with self.subTest(text=text):
                adapter = _recording_adapter()
                service = mock.Mock(wraps=ChatClefNaturalLanguageService())
                extension = MinecraftFabricChatClefExtension(
                    adapter=adapter,
                    natural_language_service=service,
                )
                router = MinecraftChatClefInputRouter(
                    extension=extension,
                    log_callback=lambda _message: None,
                )
                event = _chat_event(text, f"{index:032x}")

                first = router.route(event)
                replay = router.route(event)

                self.assertTrue(first.handled)
                self.assertEqual([], adapter.requests)
                self.assertEqual(1, service.translate.call_count)
                self.assertTrue(replay.handled)
                self.assertEqual(
                    "auto_deposit_trust_duplicate_input_event",
                    replay.reason,
                )
                self.assertEqual(1, service.translate.call_count)
                self.assertEqual(
                    0,
                    router.auto_deposit_trust_claim_registry.issued_count,
                )


def _recording_adapter():
    adapter = SimpleNamespace(backend_id="fabric_chatclef", requests=[])

    def submit_command(request):
        adapter.requests.append(request)
        return CommandResultDTO(
            request_id=request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            message="accepted",
            data={"command": request.command},
        )

    def get_status():
        return StatusSnapshotDTO(
            backend_id=adapter.backend_id,
            enabled=True,
            connected=True,
            lifecycle_state=BridgeLifecycleState.CONNECTED,
            detail="connected",
            details={"commands": {"active_request_id": None}},
        )

    adapter.submit_command = submit_command
    adapter.get_status = get_status
    return adapter


def _chat_event(text: str, event_id: str) -> LaviInputEvent:
    return LaviInputEvent(
        text=text,
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id=event_id,
        fallback_payload=text,
    )


if __name__ == "__main__":
    unittest.main()
