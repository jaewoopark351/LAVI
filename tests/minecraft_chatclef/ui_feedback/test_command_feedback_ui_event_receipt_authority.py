#20260907_kpopmodder: Verify direct GUI provenance is immutable, bounded, and one-shot.
import dataclasses
import unittest

from plugins.Minecraft.fabric.chatclef.ui.feedback import (
    CommandFeedbackUiEventReceiptAuthority,
)


class CommandFeedbackUiEventReceiptAuthorityTests(unittest.TestCase):
    def setUp(self):
        event_ids = iter(
            (
                "1" * 32,
                "2" * 32,
                "3" * 32,
            )
        )
        self.authority = CommandFeedbackUiEventReceiptAuthority(
            capacity=2,
            event_id_factory=lambda: next(event_ids),
        )

    def test_matching_korean_receipt_projects_one_final_input_event_once(self):
        receipt = self.authority.issue(
            request_id="lavi-ko-gui-request",
            source="lavi_gui_korean",
            text="다이아 곡괭이 만들어줘",
        )

        event = self.authority.consume(
            receipt,
            request_id="lavi-ko-gui-request",
            source="lavi_gui_korean",
            text="다이아 곡괭이 만들어줘",
        )

        self.assertIsNotNone(event)
        self.assertEqual("1" * 32, event.event_id)
        self.assertEqual("minecraft_korean_gui_submit", event.event_kind)
        self.assertEqual("minecraft_fabric_chatclef_ui", event.provider_id)
        self.assertTrue(event.final)
        self.assertIsNone(event.fallback_payload)
        self.assertIsNone(
            self.authority.consume(
                receipt,
                request_id="lavi-ko-gui-request",
                source="lavi_gui_korean",
                text="다이아 곡괭이 만들어줘",
            )
        )

    def test_tampered_text_spends_receipt_and_fails_closed(self):
        receipt = self.authority.issue(
            request_id="lavi-gui-request",
            source="lavi_gui",
            text="@get oak_log 2",
        )

        self.assertIsNone(
            self.authority.consume(
                receipt,
                request_id="lavi-gui-request",
                source="lavi_gui",
                text="@get diamond 64",
            )
        )
        self.assertIsNone(
            self.authority.consume(
                receipt,
                request_id="lavi-gui-request",
                source="lavi_gui",
                text="@get oak_log 2",
            )
        )

    def test_receipt_is_frozen_and_capacity_does_not_evict_live_receipts(self):
        first = self.authority.issue(
            request_id="one",
            source="lavi_gui",
            text="@scan dirt",
        )
        second = self.authority.issue(
            request_id="two",
            source="lavi_gui",
            text="@gamma 1.0",
        )

        with self.assertRaises(dataclasses.FrozenInstanceError):
            first.source = "lavi_gui_korean"
        self.assertIsNotNone(second)
        self.assertIsNone(
            self.authority.issue(
                request_id="three",
                source="lavi_gui",
                text="@stop",
            )
        )
        self.assertTrue(self.authority.abandon(first))

    def test_unknown_source_and_non_hex_event_id_are_rejected(self):
        self.assertIsNone(
            self.authority.issue(
                request_id="bad-source",
                source="voice_input_final",
                text="get dirt 1",
            )
        )
        authority = CommandFeedbackUiEventReceiptAuthority(
            event_id_factory=lambda: "not-an-event-id",
        )
        self.assertIsNone(
            authority.issue(
                request_id="bad-id",
                source="lavi_gui",
                text="get dirt 1",
            )
        )


if __name__ == "__main__":
    unittest.main()
