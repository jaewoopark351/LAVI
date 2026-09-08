#20260909_kpopmodder: Verify atomic busy inspection issues STATUS only for one exact live owner.
from __future__ import annotations

import unittest
from dataclasses import replace
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandBusyObservedIdentity,
    CommandBusyStatusInspector,
    CommandFeedbackAdmissionGrant,
    CommandFeedbackContext,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackLifecycleState,
    CommandFeedbackPublicationCoordinator,
    CommandFeedbackPublicationPermit,
    CommandFeedbackStatusCoordinator,
    CommandTerminalEvidenceProfileRegistry,
)


class CommandBusyStatusInspectorTests(unittest.TestCase):
    def test_running_and_pending_each_issue_one_exact_status_permit(self):
        for lifecycle_status, result_reason, expected_state in (
            ("running", "dispatch_started", CommandFeedbackLifecycleSnapshot.RUNNING),
            ("accepted", "", CommandFeedbackLifecycleSnapshot.PENDING),
        ):
            with self.subTest(lifecycle_status=lifecycle_status):
                runtime = _runtime(
                    lifecycle_status=lifecycle_status,
                    result_reason=result_reason,
                )

                snapshot, permit = runtime.inspect()

                self.assertIs(type(snapshot), CommandFeedbackLifecycleSnapshot)
                self.assertEqual(expected_state, snapshot.state)
                self.assertIs(runtime.descriptor, snapshot.descriptor)
                self.assertIs(type(permit), CommandFeedbackPublicationPermit)
                self.assertEqual(CommandFeedbackPublicationPermit.STATUS, permit.kind)
                self.assertEqual(2, permit.sequence)

    def test_each_observed_and_live_identity_mismatch_issues_no_permit(self):
        mutations = (
            lambda runtime: setattr(
                runtime,
                "identity",
                replace(runtime.identity, active_session_id="session-b"),
            ),
            lambda runtime: setattr(
                runtime,
                "identity",
                replace(runtime.identity, active_generation=2),
            ),
            lambda runtime: setattr(
                runtime,
                "identity",
                replace(runtime.identity, active_request_id="request-b"),
            ),
            lambda runtime: setattr(
                runtime,
                "identity",
                replace(runtime.identity, active_command_message_id="message-b"),
            ),
            lambda runtime: setattr(runtime, "active_session_id", "session-b"),
            lambda runtime: setattr(runtime, "active_generation", 2),
            lambda runtime: setattr(
                runtime,
                "active_command",
                replace(runtime.active_command, request_id="request-b"),
            ),
            lambda runtime: setattr(
                runtime,
                "active_command",
                replace(runtime.active_command, command_message_id="message-b"),
            ),
        )

        for index, mutate in enumerate(mutations):
            with self.subTest(index=index):
                runtime = _runtime()
                mutate(runtime)
                snapshot, permit = runtime.inspect()
                self.assertIsNone(snapshot)
                self.assertIsNone(permit)

    def test_stale_or_untrusted_owner_facts_issue_no_permit(self):
        def stale_context_owner(runtime):
            runtime.state.context = replace(
                runtime.state.context,
                owner_token=object(),
            )

        def stale_context_descriptor(runtime):
            other = _descriptor("get pumpkin_pie 1")
            runtime.state.context = replace(
                runtime.state.context,
                descriptor=other,
            )

        def stale_lifecycle_grant(runtime):
            runtime.state.lifecycle_grant = CommandFeedbackAdmissionGrant._issue(
                descriptor=runtime.descriptor
            )

        def terminal_claimed(runtime):
            runtime.state.terminal_claimed = True

        def duck_typed_active_owner(runtime):
            active = SimpleNamespace(
                websocket=runtime.websocket,
                session_id="session-a",
                generation=1,
                request_id="request-a",
                command_message_id="message-a",
                command=runtime.descriptor.command,
                source=runtime.descriptor.command_source,
            )
            runtime.active_command = active
            runtime.state.context = replace(
                runtime.state.context,
                owner_token=active,
            )

        def boolean_active_generation(runtime):
            active = replace(runtime.active_command, generation=True)
            runtime.active_command = active
            runtime.state.context = replace(
                runtime.state.context,
                owner_token=active,
            )

        mutations = (
            lambda runtime: setattr(runtime, "connected", False),
            lambda runtime: setattr(runtime, "quarantine_active", True),
            lambda runtime: setattr(runtime, "active_generation", True),
            lambda runtime: setattr(runtime, "active_websocket", object()),
            lambda runtime: setattr(runtime, "active_command", None),
            duck_typed_active_owner,
            boolean_active_generation,
            lambda runtime: setattr(runtime.state, "context", None),
            stale_context_owner,
            stale_context_descriptor,
            stale_lifecycle_grant,
            terminal_claimed,
            lambda runtime: setattr(runtime.state, "lifecycle_token", None),
            lambda runtime: setattr(runtime.state, "reservation", object()),
            lambda runtime: setattr(
                runtime.state,
                "start_publication_permit",
                None,
            ),
            lambda runtime: setattr(
                runtime.state,
                "context",
                replace(runtime.state.context, generation=True),
            ),
        )

        for index, mutate in enumerate(mutations):
            with self.subTest(index=index):
                runtime = _runtime()
                mutate(runtime)
                snapshot, permit = runtime.inspect()
                self.assertIsNone(snapshot)
                self.assertIsNone(permit)

    def test_unavailable_evidence_returns_snapshot_without_issuing_permit(self):
        runtime = _runtime(
            lifecycle_status="running",
            result_reason="wrong_progress_reason",
        )

        snapshot, permit = runtime.inspect()

        self.assertIs(type(snapshot), CommandFeedbackLifecycleSnapshot)
        self.assertEqual(CommandFeedbackLifecycleSnapshot.UNAVAILABLE, snapshot.state)
        self.assertEqual("evidence_unavailable", snapshot.availability_reason)
        self.assertIsNone(permit)
        next_permit = runtime.publications.issue(
            CommandFeedbackPublicationPermit.STATUS
        )
        self.assertEqual(2, next_permit.sequence)

    def test_wrong_observed_type_fails_before_reading_live_state(self):
        inspector = CommandBusyStatusInspector(
            state=_ExplodingState(),
            status_coordinator=object(),
            publication_coordinator=object(),
        )

        self.assertEqual(
            (None, None),
            inspector.inspect_for_publication(
                observed_identity={},
                active_websocket=object(),
                active_session_id="session-a",
                active_generation=1,
                active_command=object(),
                connected=True,
                quarantine_active=False,
            ),
        )


class _Runtime:
    def __init__(self, *, lifecycle_status: str, result_reason: str) -> None:
        self.websocket = object()
        self.descriptor = _descriptor("get diamond_pickaxe 1")
        self.grant = CommandFeedbackAdmissionGrant._issue(
            descriptor=self.descriptor
        )
        self.active_command = FabricChatClefActiveCommand(
            websocket=self.websocket,
            session_id="session-a",
            generation=1,
            request_id="request-a",
            command_message_id="message-a",
            command=self.descriptor.command,
            source=self.descriptor.command_source,
            started_at_ms=100,
        )
        self.identity = CommandBusyObservedIdentity(
            active_session_id="session-a",
            active_generation=1,
            active_request_id="request-a",
            active_command_message_id="message-a",
        )
        self.active_websocket = self.websocket
        self.active_session_id = "session-a"
        self.active_generation = 1
        self.connected = True
        self.quarantine_active = False
        self.state = CommandFeedbackLifecycleState()
        self.state.context = CommandFeedbackContext(
            websocket=self.websocket,
            owner_token=self.active_command,
            admission_grant=self.grant,
            descriptor=self.descriptor,
            session_id="session-a",
            generation=1,
            request_id="request-a",
            command_message_id="message-a",
            accepted_at_ms=100,
        )
        self.state.latest_status = lifecycle_status
        self.state.latest_result_reason = result_reason
        self.state.lifecycle_token = object()
        self.state.lifecycle_grant = self.grant
        self.publications = CommandFeedbackPublicationCoordinator()
        self.state.start_publication_permit = self.publications.begin(
            self.state.lifecycle_token
        )
        profiles = CommandTerminalEvidenceProfileRegistry()
        self.inspector = CommandBusyStatusInspector(
            state=self.state,
            status_coordinator=CommandFeedbackStatusCoordinator(
                evidence_profiles=profiles,
            ),
            publication_coordinator=self.publications,
        )

    def inspect(self):
        return self.inspector.inspect_for_publication(
            observed_identity=self.identity,
            active_websocket=self.active_websocket,
            active_session_id=self.active_session_id,
            active_generation=self.active_generation,
            active_command=self.active_command,
            connected=self.connected,
            quarantine_active=self.quarantine_active,
        )


class _ExplodingState:
    @property
    def context(self):
        raise AssertionError("wrong observed types must not inspect state")


def _runtime(
    *,
    lifecycle_status: str = "running",
    result_reason: str = "dispatch_started",
) -> _Runtime:
    return _Runtime(
        lifecycle_status=lifecycle_status,
        result_reason=result_reason,
    )


def _descriptor(command: str):
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
