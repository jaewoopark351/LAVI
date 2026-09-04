#20260905_kpopmodder: Verify trusted Chat/Voice H5 ingress through one-shot canonical request delivery.
from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest import mock

from input_core.input_event.adapters import (
    LocalChatInputEventAdapter,
    ProviderBoundInputEventAdapter,
)
from input_core.input_event.contracts import LaviInputEvent
from llm_core.chat_input import LocalChatPredictionEntrypoint
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
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.delivery import (
    AutoDepositTrustInputEventClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService


class AutoDepositTrustSourceToRequestContractTests(unittest.TestCase):
    PHRASE = "현재 위치 기준 반경 16 곱하기 16 상자를 등록해"
    CANONICAL_COMMAND = "auto_deposit_trust area 16x16"

    def test_trusted_chat_and_voice_reach_one_canonical_request_with_full_metadata(self):
        cases = (
            (
                "chat",
                "1" * 32,
                "lavi_chat_ui",
                "lavi_chat_ui",
                "chat_submit",
            ),
            (
                "voice",
                "2" * 32,
                "voice_input_final",
                "VoiceInput",
                "final_transcript",
            ),
        )

        for lane, event_id, source, provider_id, event_kind in cases:
            with self.subTest(lane=lane):
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

                if lane == "chat":
                    input_event_adapter = LocalChatInputEventAdapter(
                        event_id_factory=lambda: event_id,
                    )
                    ingress = LocalChatPredictionEntrypoint(
                        input_event_adapter=input_event_adapter,
                        predict_callback=lambda event, _history, _system_prompt: iter(
                            (router.route(event),)
                        ),
                    )
                    decision = next(ingress.predict(self.PHRASE, [], "system"))
                else:
                    ingress = ProviderBoundInputEventAdapter(
                        provider=_voice_input_provider(),
                        output_callback=router.route,
                        event_id_factory=lambda: event_id,
                    )
                    decision = ingress(self.PHRASE)

                self.assertTrue(decision.handled)
                self.assertEqual("minecraft_command_routed", decision.reason)
                self.assertEqual(1, service.translate.call_count)
                self.assertEqual(1, len(adapter.requests))
                request = adapter.requests[0]
                self.assertEqual(self.CANONICAL_COMMAND, request.command)
                self.assertEqual(source, request.source)
                self.assertEqual(
                    self._expected_metadata(
                        source=source,
                        provider_id=provider_id,
                        event_kind=event_kind,
                        event_id=event_id,
                    ),
                    request.metadata,
                )
                self.assertNotIn("route_claim", request.metadata)
                self.assertEqual(
                    0,
                    router.auto_deposit_trust_claim_registry.issued_count,
                )

    def test_invalid_event_id_vectors_fail_before_claim_translate_or_submit(self):
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
        invalid_event_ids = (
            None,
            "",
            "a" * 31,
            "a" * 33,
            "A" * 32,
            "g" * 32,
            " " + "a" * 32,
            "a" * 32 + " ",
            "a" * 31 + "\n",
            b"a" * 32,
            ["a" * 32],
            {"event_id": "a" * 32},
            _StringEventId("a" * 32),
        )

        for event_id in invalid_event_ids:
            with self.subTest(event_id=event_id):
                decision = router.route(_chat_event(self.PHRASE, event_id))
                self.assertTrue(decision.handled)
                self.assertEqual(
                    "auto_deposit_trust_input_provenance_invalid",
                    decision.reason,
                )

        self.assertEqual(0, service.translate.call_count)
        self.assertEqual([], adapter.requests)
        self.assertEqual(
            0,
            router.auto_deposit_trust_claim_registry.claimed_count,
        )
        self.assertEqual(
            0,
            router.auto_deposit_trust_claim_registry.issued_count,
        )

    def test_compound_h5_precedes_stop_then_spends_claim_and_blocks_replay(self):
        claim_registry = _CapturingClaimRegistry()
        adapter = _recording_adapter()
        service = mock.Mock(wraps=ChatClefNaturalLanguageService())
        extension = MinecraftFabricChatClefExtension(
            adapter=adapter,
            natural_language_service=service,
            auto_deposit_trust_claim_registry=claim_registry,
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )
        event = _chat_event(
            "자동보관등록 영역 16x16 하고 멈춰",
            "f" * 32,
        )

        first = router.route(event)
        receipt = claim_registry.last_receipt
        replay = router.route(event)

        self.assertTrue(first.handled)
        self.assertEqual("minecraft_translation_rejected", first.reason)
        self.assertEqual(
            "auto_deposit_trust_area_ambiguous_compound",
            first.result["error"],
        )
        self.assertIsNotNone(receipt)
        self.assertEqual("SPENT", claim_registry.state(receipt))
        self.assertEqual([], adapter.requests)
        self.assertEqual(1, service.translate.call_count)
        self.assertTrue(replay.handled)
        self.assertEqual(
            "auto_deposit_trust_duplicate_input_event",
            replay.reason,
        )
        self.assertEqual(1, service.translate.call_count)
        self.assertEqual(0, claim_registry.issued_count)

    def _expected_metadata(
        self,
        *,
        source: str,
        provider_id: str,
        event_kind: str,
        event_id: str,
    ) -> dict[str, object]:
        translation = {
            "status": "validated",
            "executable": True,
            "command": self.CANONICAL_COMMAND,
            "intent": {
                "intent_type": "auto_deposit_trust_area",
                "quantity": None,
                "item_phrase": "",
                "food_units": None,
                "x": None,
                "y": None,
                "z": None,
                "player_name": "",
                "original_text": self.PHRASE,
                "source": "rule",
                "confidence": 1.0,
                "language": "ko",
                "slots": {},
            },
            "resolved_target": None,
            "reason_code": "validated",
            "message": "Korean command was translated to ChatClef DSL.",
            "data": {},
        }
        return {
            "input_route": "minecraft_fabric_chatclef",
            "language": "ko",
            "input_event": {
                "source": source,
                "provider_id": provider_id,
                "event_kind": event_kind,
                "final": True,
                "event_id": event_id,
            },
            "natural_language": {
                "language": "ko",
                "original_text": self.PHRASE,
                "translation_input_text": self.PHRASE,
                "translation": translation,
            },
        }


class _CapturingClaimRegistry(AutoDepositTrustInputEventClaimRegistry):
    def __init__(self):
        super().__init__()
        self.last_receipt = None

    def claim(self, event, *, claim_owner=None):
        receipt, reason = super().claim(event, claim_owner=claim_owner)
        if receipt is not None:
            self.last_receipt = receipt
        return receipt, reason


class _StringEventId(str):
    pass


def _voice_input_provider():
    return SimpleNamespace(
        handle=SimpleNamespace(
            descriptor=SimpleNamespace(id="VoiceInput")
        )
    )


def _chat_event(text: str, event_id: object) -> LaviInputEvent:
    return LaviInputEvent(
        text=text,
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id=event_id,
        fallback_payload=text,
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


if __name__ == "__main__":
    unittest.main()
