#20260908_kpopmodder: Verify synchronized status inspection composes contextual claim exactly once.
from __future__ import annotations

import unittest
from dataclasses import replace
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQuery,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackStatusCoordinator,
    CommandTerminalEvidenceProfileRegistry,
)


class CommandFeedbackContextualStatusInspectionTests(unittest.TestCase):
    def setUp(self) -> None:
        self.owner = object()
        self.descriptor = _production_descriptor("get iron_ingot 10")
        self.coordinator = CommandFeedbackStatusCoordinator(
            evidence_profiles=CommandTerminalEvidenceProfileRegistry(),
        )

    def test_prefixless_generic_claims_running_exact_owner(self):
        snapshot = self._inspect(CommandStatusQuery("any", False))

        self.assertTrue(snapshot.query_matched)
        self.assertEqual(CommandFeedbackLifecycleSnapshot.RUNNING, snapshot.state)
        self.assertEqual("unclaimed", snapshot.terminal_state)
        self.assertIs(self.descriptor, snapshot.descriptor)

    def test_prefixless_family_mismatch_is_unavailable_and_unclaimed(self):
        snapshot = self._inspect(CommandStatusQuery("movement_goto", False))

        self.assertFalse(snapshot.query_matched)
        self.assertFalse(snapshot.query_family_matched)
        self.assertEqual(CommandFeedbackLifecycleSnapshot.UNAVAILABLE, snapshot.state)
        self.assertEqual("query_mismatch", snapshot.availability_reason)

    def test_addressed_mismatch_retains_actual_descriptor_and_match_facts(self):
        snapshot = self._inspect(CommandStatusQuery("movement_goto", True))

        self.assertTrue(snapshot.query_matched)
        self.assertFalse(snapshot.query_family_matched)
        self.assertTrue(snapshot.query_target_matched)
        self.assertIs(self.descriptor, snapshot.descriptor)
        self.assertEqual(CommandFeedbackLifecycleSnapshot.RUNNING, snapshot.state)

    def test_matched_owner_keeps_claim_while_transport_truth_becomes_cautious(self):
        for values in (
            {"connected": False},
            {"quarantine_active": True},
        ):
            with self.subTest(values=values):
                snapshot = self._inspect(CommandStatusQuery("any", False), **values)
                self.assertTrue(snapshot.query_matched)
                self.assertEqual(CommandFeedbackLifecycleSnapshot.UNAVAILABLE, snapshot.state)
                response = CommandLifecycleResponseRenderer().render_status(snapshot)
                self.assertEqual(
                    "지금 마인크래프트 작업 상태를 확인하지 못했어",
                    response,
                )
                self.assertNotIn("구하는 중이야", response)

    def test_accepted_owner_is_pending_and_never_rendered_as_running(self):
        snapshot = self._inspect(
            CommandStatusQuery("any", False),
            latest_status="accepted",
            latest_result_reason="",
        )

        self.assertTrue(snapshot.query_matched)
        self.assertEqual(CommandFeedbackLifecycleSnapshot.PENDING, snapshot.state)
        self.assertEqual(
            "아이템 구하기 작업이 시작됐는지 확인 중이야",
            CommandLifecycleResponseRenderer().render_status(snapshot),
        )

    def test_no_owner_terminal_stale_and_specialized_contexts_do_not_claim_prefixless(self):
        no_owner = self.coordinator.inspect(
            state=SimpleNamespace(context=None, terminal_claimed=False),
            active_command=None,
            connected=True,
            quarantine_active=False,
            query=CommandStatusQuery("any", False),
        )
        terminal = self._inspect(
            CommandStatusQuery("any", False),
            terminal_claimed=True,
        )
        stale = self._inspect(
            CommandStatusQuery("any", False),
            active_command=object(),
        )
        specialized = self._inspect(
            CommandStatusQuery("any", False),
            descriptor=_production_descriptor("stop"),
        )

        for snapshot in (no_owner, terminal, stale, specialized):
            self.assertFalse(snapshot.query_matched)
            self.assertEqual(CommandFeedbackLifecycleSnapshot.UNAVAILABLE, snapshot.state)

    def test_addressed_no_owner_preserves_bounded_idle_contract(self):
        snapshot = self.coordinator.inspect(
            state=SimpleNamespace(context=None, terminal_claimed=False),
            active_command=None,
            connected=True,
            quarantine_active=False,
            query=CommandStatusQuery("any", True),
        )

        self.assertTrue(snapshot.query_matched)
        self.assertEqual(CommandFeedbackLifecycleSnapshot.IDLE, snapshot.state)
        self.assertEqual("none", snapshot.terminal_state)
        self.assertEqual("no_tracked_owner", snapshot.availability_reason)

    def test_terminal_claim_is_frozen_before_returning_the_idle_snapshot(self):
        snapshot = self._inspect(
            CommandStatusQuery("any", True),
            terminal_claimed=True,
        )

        self.assertEqual(CommandFeedbackLifecycleSnapshot.IDLE, snapshot.state)
        self.assertEqual("claimed", snapshot.terminal_state)

    def test_legacy_query_none_target_filter_remains_compatible(self):
        matching = self._inspect(None, target_item="iron_ingot")
        mismatch = self._inspect(None, target_item="diamond_pickaxe")

        self.assertTrue(matching.query_matched)
        self.assertFalse(mismatch.query_matched)

    def test_invalid_descriptor_identity_is_unavailable_and_never_claimed(self):
        contradictions = (
            {},
            replace(self.descriptor, command_name="unregistered"),
            replace(self.descriptor, requested_family="movement_goto"),
            replace(self.descriptor, evidence_profile_id="wrong_evidence_v1"),
            replace(self.descriptor, response_lifecycle_kind="persistent_task"),
        )
        for descriptor in contradictions:
            with self.subTest(descriptor=descriptor):
                snapshot = self._inspect(
                    CommandStatusQuery("any", False),
                    descriptor=descriptor,
                )
                self.assertFalse(snapshot.query_matched)
                self.assertFalse(snapshot.query_family_matched)
                self.assertFalse(snapshot.query_target_matched)
                self.assertEqual(
                    CommandFeedbackLifecycleSnapshot.UNAVAILABLE,
                    snapshot.state,
                )
                self.assertEqual("query_mismatch", snapshot.availability_reason)

    def test_unavailable_evidence_states_have_explicit_reason(self):
        fixtures = (
            ("running", "wrong_progress_reason"),
            ("completed", "dispatch_started"),
            ("unknown", ""),
        )
        for status, result_reason in fixtures:
            with self.subTest(status=status, result_reason=result_reason):
                snapshot = self._inspect(
                    CommandStatusQuery("any", False),
                    latest_status=status,
                    latest_result_reason=result_reason,
                )
                self.assertTrue(snapshot.query_matched)
                self.assertEqual(
                    CommandFeedbackLifecycleSnapshot.UNAVAILABLE,
                    snapshot.state,
                )
                self.assertEqual(
                    "evidence_unavailable",
                    snapshot.availability_reason,
                )

    def _inspect(
        self,
        query,
        *,
        descriptor=None,
        active_command=None,
        terminal_claimed=False,
        connected=True,
        quarantine_active=False,
        target_item=None,
        latest_status="running",
        latest_result_reason="dispatch_started",
    ):
        selected_descriptor = self.descriptor if descriptor is None else descriptor
        state = SimpleNamespace(
            context=SimpleNamespace(
                descriptor=selected_descriptor,
                owner_token=self.owner,
            ),
            terminal_claimed=terminal_claimed,
            latest_result_reason=latest_result_reason,
            latest_status=latest_status,
        )
        return self.coordinator.inspect(
            state=state,
            active_command=self.owner if active_command is None else active_command,
            connected=connected,
            quarantine_active=quarantine_active,
            query=query,
            target_item=target_item,
        )


def _production_descriptor(command: str):
    descriptor = CommandFeedbackDescriptorFactory().decode_registered_command_name_only(
        command,
        command_source="lavi_gui",
        event_id="1" * 32,
        provider_id="minecraft_fabric_chatclef_ui",
        event_kind="minecraft_raw_gui_submit",
    )
    if descriptor is None:
        raise AssertionError(f"descriptor fixture was not accepted: {command}")
    return descriptor


if __name__ == "__main__":
    unittest.main()
