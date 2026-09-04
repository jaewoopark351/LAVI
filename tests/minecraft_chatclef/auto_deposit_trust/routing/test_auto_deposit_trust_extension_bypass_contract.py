#20260905_kpopmodder: Ensure direct extension calls cannot fabricate H5 route authority.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.extension import (
    MinecraftFabricChatClefExtension,
)
from plugins.Minecraft.fabric.chatclef.input import MinecraftChatClefInputRouter


class AutoDepositTrustExtensionBypassContractTests(unittest.TestCase):
    def test_source_and_metadata_strings_cannot_replace_an_opaque_claim(self):
        adapter = _recording_adapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        result = extension.handle_natural_language_command(
            {
                "request_id": "forged-h5-1",
                "text": "자동보관등록 영역 16x16",
                "source": "lavi_chat_ui",
                "metadata": {
                    "route_claim": "forged",
                    "input_event": {
                        "event_id": "a" * 32,
                        "source": "lavi_chat_ui",
                    },
                },
            }
        )

        self.assertFalse(result["ok"])
        self.assertEqual(
            "auto_deposit_trust_input_claim_required",
            result["details"]["admission_reason"],
        )
        self.assertEqual([], adapter.requests)

    def test_malformed_translated_submission_spends_an_issued_claim(self):
        adapter = _recording_adapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        event = LaviInputEvent(
            text="자동보관등록 영역 16x16",
            source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_kind="chat_submit",
            final=True,
            event_id="f" * 32,
            fallback_payload="자동보관등록 영역 16x16",
        )
        registry = extension.get_auto_deposit_trust_input_claim_registry()
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )
        receipt, reason = registry.claim(event, claim_owner=router)
        self.assertEqual("", reason)
        self.assertEqual("ISSUED", registry.state(receipt))

        result = extension.submit_translated_command(
            {"source": "lavi_chat_ui", "text": event.text},
            {"status": "bogus"},
            route_claim=receipt,
        )

        self.assertFalse(result["ok"])
        self.assertEqual("malformed_translation_result", result["error"])
        self.assertEqual("SPENT", registry.state(receipt))
        self.assertEqual([], adapter.requests)

    def test_direct_registry_access_cannot_self_issue_a_route_claim(self):
        adapter = _recording_adapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )
        registry = extension.get_auto_deposit_trust_input_claim_registry()
        event = LaviInputEvent(
            text="자동보관등록 영역 16x16",
            source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_kind="chat_submit",
            final=True,
            event_id="d" * 32,
            fallback_payload="자동보관등록 영역 16x16",
        )

        receipt, reason = registry.claim(event)

        self.assertIsNone(receipt)
        self.assertEqual("auto_deposit_trust_input_internal_error", reason)
        self.assertEqual(0, registry.claimed_count)

    def test_command_text_extraction_failure_spends_an_issued_claim(self):
        adapter = _recording_adapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )
        registry = extension.get_auto_deposit_trust_input_claim_registry()
        event = LaviInputEvent(
            text="자동보관등록 영역 16x16",
            source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_kind="chat_submit",
            final=True,
            event_id="c" * 32,
            fallback_payload="자동보관등록 영역 16x16",
        )
        receipt, reason = registry.claim(event, claim_owner=router)
        self.assertEqual("", reason)

        class BrokenCommand:
            def __str__(self):
                raise RuntimeError("boom")

        command = BrokenCommand()
        result = extension.submit_translated_command(
            command,
            {"status": "bogus"},
            route_claim=receipt,
        )

        self.assertFalse(result["ok"])
        self.assertEqual("internal_error", result["error"])
        self.assertEqual(
            "auto_deposit_trust_input_internal_error",
            result["details"]["reason_code"],
        )
        self.assertEqual("SPENT", registry.state(receipt))
        self.assertEqual([], adapter.requests)


def _recording_adapter():
    adapter = SimpleNamespace(backend_id="fabric_chatclef", requests=[])

    def submit_command(request):
        adapter.requests.append(request)
        return CommandResultDTO(
            request_id=request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            message="accepted",
        )

    adapter.submit_command = submit_command
    return adapter


if __name__ == "__main__":
    unittest.main()
