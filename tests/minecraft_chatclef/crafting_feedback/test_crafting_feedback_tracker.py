#20260907_kpopmodder: Lock atomic crafting-feedback reservation and status behavior.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackAdmissionGrant,
    CraftingFeedbackStatusSnapshot,
    CraftingFeedbackTracker,
)


class CraftingFeedbackTrackerTests(unittest.TestCase):
    def test_failed_bind_retires_reservation_and_allows_next_reserve(self):
        tracker = CraftingFeedbackTracker()
        first = _grant("a" * 32)

        self.assertTrue(tracker.reserve(first))
        self.assertFalse(
            tracker.bind_reserved(
                _active(),
                {"input_event": {"event_id": "wrong"}},
            )
        )

        self.assertTrue(tracker.reserve(_grant("b" * 32)))

    def test_python_absence_alone_never_claims_authoritative_idle(self):
        snapshot = CraftingFeedbackTracker().inspect(
            active_command=None,
            connected=True,
            quarantine_active=False,
            target_item=None,
        )

        self.assertEqual(CraftingFeedbackStatusSnapshot.UNAVAILABLE, snapshot.state)

    def test_only_latest_exact_dispatch_started_is_renderable_as_running(self):
        tracker, active, grant = _bound_tracker()
        self.assertTrue(tracker.claim_start(grant, _accepted_result()))

        tracker.record_nonterminal(
            status="running",
            result_reason="dispatch_started",
            evidence_sequence=1,
        )
        running = tracker.inspect(
            active_command=active,
            connected=True,
            quarantine_active=False,
            target_item="diamond_pickaxe",
        )
        tracker.record_nonterminal(
            status="running",
            result_reason="finish_callback_observed_nonterminal",
            evidence_sequence=2,
        )
        uncertain = tracker.inspect(
            active_command=active,
            connected=True,
            quarantine_active=False,
            target_item="diamond_pickaxe",
        )

        self.assertEqual(CraftingFeedbackStatusSnapshot.RUNNING, running.state)
        self.assertEqual(CraftingFeedbackStatusSnapshot.UNAVAILABLE, uncertain.state)
        self.assertTrue(tracker.dispatch_started_observed(active))

    def test_unknown_submit_suppresses_start_but_keeps_later_terminal_grant(self):
        tracker, _active_value, grant = _bound_tracker()

        claimed = tracker.claim_start(
            grant,
            {
                "ok": False,
                "status": {
                    "request_id": "request-1",
                    "ok": False,
                    "status": "unknown",
                    "data": {},
                },
            },
        )

        self.assertFalse(claimed)
        self.assertIsNotNone(tracker.context)

    def test_disconnect_or_unknown_quarantine_never_reports_running(self):
        for connected, quarantine_active in ((False, False), (True, True)):
            with self.subTest(
                connected=connected,
                quarantine_active=quarantine_active,
            ):
                tracker, active, _grant_value = _bound_tracker()
                tracker.record_nonterminal(
                    status="running",
                    result_reason="dispatch_started",
                    evidence_sequence=1,
                )

                snapshot = tracker.inspect(
                    active_command=active,
                    connected=connected,
                    quarantine_active=quarantine_active,
                    target_item="diamond_pickaxe",
                )

                self.assertEqual(
                    CraftingFeedbackStatusSnapshot.UNAVAILABLE,
                    snapshot.state,
                )


def _grant(event_id: str) -> CraftingFeedbackAdmissionGrant:
    return CraftingFeedbackAdmissionGrant._issue(
        acquisition_verb_class="craft",
        command="get diamond_pickaxe 1",
        command_source="lavi_chat_ui",
        event_id=event_id,
        event_kind="chat_submit",
        input_source="lavi_chat_ui",
        intent_kind="get_item",
        provider_id="lavi_chat_ui",
        requested_count=1,
        spoken_item_label="다이아 곡괭이",
        target_item="diamond_pickaxe",
    )


def _active() -> FabricChatClefActiveCommand:
    return FabricChatClefActiveCommand(
        websocket=_WEBSOCKET,
        session_id="session-1",
        generation=1,
        request_id="request-1",
        command_message_id="message-1",
        command="get diamond_pickaxe 1",
        source="lavi_chat_ui",
    )


def _bound_tracker():
    tracker = CraftingFeedbackTracker()
    active = _active()
    grant = _grant("c" * 32)
    if not tracker.reserve(grant):
        raise AssertionError("fixture reservation failed")
    if not tracker.bind_reserved(active, _metadata("c" * 32)):
        raise AssertionError("fixture binding failed")
    return tracker, active, grant


def _metadata(event_id: str):
    return {
        "input_event": {
            "event_id": event_id,
            "source": "lavi_chat_ui",
            "provider_id": "lavi_chat_ui",
            "event_kind": "chat_submit",
            "final": True,
        }
    }


def _accepted_result():
    return {
        "ok": True,
        "status": {
            "request_id": "request-1",
            "ok": True,
            "status": "accepted",
            "data": {
                "session_id": "session-1",
                "connection_generation": 1,
                "command_message_id": "message-1",
            },
        },
    }


_WEBSOCKET = object()


if __name__ == "__main__":
    unittest.main()
