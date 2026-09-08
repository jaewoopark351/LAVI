#20260908_kpopmodder: Verify a STATUS permit carries the same-lock frozen unclaimed terminal fact.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQuery,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackStatusCoordinator,
    CommandTerminalEvidenceProfileRegistry,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.status import (
    CommandFeedbackStatusPublicationCoordinator,
)


class CommandFeedbackStatusPublicationTerminalStateTests(unittest.TestCase):
    def test_terminal_state_keeps_the_previous_acknowledgement_position(self):
        acknowledgement = object()

        snapshot = CommandFeedbackLifecycleSnapshot(
            "running",
            None,
            "get",
            "item_get",
            "iron_ingot",
            1,
            "dispatch_started",
            True,
            True,
            True,
            True,
            "",
            acknowledgement,
        )

        self.assertIs(acknowledgement, snapshot.publication_acknowledgement)
        self.assertEqual("none", snapshot.terminal_state)

    def test_issued_permit_has_exact_unclaimed_snapshot_terminal_state(self):
        owner = object()
        state = _state(owner=owner)
        permit = object()
        coordinator = CommandFeedbackStatusPublicationCoordinator(
            state=state,
            status_coordinator=CommandFeedbackStatusCoordinator(
                evidence_profiles=CommandTerminalEvidenceProfileRegistry(),
            ),
            publication_coordinator=SimpleNamespace(issue=lambda _kind: permit),
        )

        snapshot, issued = coordinator.inspect_for_publication(
            active_command=owner,
            connected=True,
            quarantine_active=False,
            query=CommandStatusQuery("any", False),
        )

        self.assertIs(permit, issued)
        self.assertEqual("unclaimed", snapshot.terminal_state)

    def test_no_context_and_terminal_claim_never_issue_a_permit(self):
        owner = object()
        no_context = SimpleNamespace(context=None, terminal_claimed=False)
        terminal = _state(owner=owner, terminal_claimed=True)
        for state, expected_terminal in ((no_context, "none"), (terminal, "claimed")):
            with self.subTest(expected_terminal=expected_terminal):
                coordinator = CommandFeedbackStatusPublicationCoordinator(
                    state=state,
                    status_coordinator=CommandFeedbackStatusCoordinator(
                        evidence_profiles=CommandTerminalEvidenceProfileRegistry(),
                    ),
                    publication_coordinator=SimpleNamespace(
                        issue=lambda _kind: self.fail("permit must not be issued")
                    ),
                )
                snapshot, issued = coordinator.inspect_for_publication(
                    active_command=owner,
                    connected=True,
                    quarantine_active=False,
                    query=CommandStatusQuery("any", True),
                )

                self.assertIsNone(issued)
                self.assertEqual(expected_terminal, snapshot.terminal_state)


def _state(*, owner, terminal_claimed=False):
    descriptor = CommandFeedbackDescriptorFactory().decode_registered_command_name_only(
        "get iron_ingot 1",
        command_source="lavi_gui",
        event_id="2" * 32,
        provider_id="minecraft_fabric_chatclef_ui",
        event_kind="minecraft_raw_gui_submit",
    )
    if descriptor is None:
        raise AssertionError("GET descriptor fixture was not accepted")
    return SimpleNamespace(
        context=SimpleNamespace(descriptor=descriptor, owner_token=owner),
        terminal_claimed=terminal_claimed,
        latest_result_reason="dispatch_started",
        latest_status="running",
    )


if __name__ == "__main__":
    unittest.main()
