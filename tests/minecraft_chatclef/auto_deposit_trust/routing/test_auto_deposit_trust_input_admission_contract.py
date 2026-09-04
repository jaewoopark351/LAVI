#20260905_kpopmodder: Lock exact trusted tuples and strict ingress event-ID validation.
from __future__ import annotations

import unittest
from dataclasses import replace

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.admission import (
    AutoDepositTrustInputAdmission,
)


class AutoDepositTrustInputAdmissionContractTests(unittest.TestCase):
    def test_only_exact_chat_and_voice_final_tuples_are_allowed(self):
        admission = AutoDepositTrustInputAdmission()
        chat = _chat_event("a" * 32)
        voice = replace(
            chat,
            source="voice_input_final",
            provider_id="VoiceInput",
            event_kind="final_transcript",
        )

        self.assertTrue(admission.inspect(chat).allowed)
        self.assertTrue(admission.inspect(voice).allowed)

        invalid = (
            replace(chat, source="TwitchChatFetch"),
            replace(chat, provider_id="VoiceInput"),
            replace(chat, event_kind="provider_output"),
            replace(chat, final=False),
            replace(chat, source={"source": "lavi_chat_ui"}),
            replace(chat, final=1),
        )
        for event in invalid:
            with self.subTest(event=event):
                self.assertFalse(admission.inspect(event).allowed)

    def test_event_id_requires_one_exact_builtin_lowercase_hex_string(self):
        admission = AutoDepositTrustInputAdmission()
        invalid_ids = (
            None,
            "",
            " ",
            " " + "a" * 32,
            "a" * 32 + " ",
            "A" * 32,
            "a" * 31,
            "a" * 33,
            "g" * 32,
            "a" * 31 + "\n",
            ["a" * 32],
            {"event_id": "a" * 32},
        )
        for event_id in invalid_ids:
            with self.subTest(event_id=event_id):
                decision = admission.inspect(_chat_event(event_id))
                self.assertFalse(decision.allowed)
                self.assertEqual(
                    "auto_deposit_trust_input_provenance_invalid",
                    decision.reason_code,
                )


def _chat_event(event_id: object) -> LaviInputEvent:
    return LaviInputEvent(
        text="자동보관등록 영역 16x16",
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id=event_id,
        fallback_payload="자동보관등록 영역 16x16",
    )


if __name__ == "__main__":
    unittest.main()
