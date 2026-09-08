#20260907_kpopmodder: Fix immediate lifecycle coalescing at the ready-publication boundary.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from llm_core.input_routing import RoutedInputDispatchCoordinator
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandFeedbackReadyDecisionAcknowledgement,
    CommandFeedbackStartDecisionDecorator,
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionGrant,
    CommandFeedbackDescriptor,
    CommandFeedbackLifecycleFacade,
    CommandFeedbackLifecycleKindProfile,
    CommandFeedbackPublicationCoordinator,
    CommandTerminalFact,
)


class CommandFeedbackPublicationCoalescingTests(unittest.TestCase):
    def test_accepted_gamma_without_java_callback_stages_one_cautious_terminal(self):
        descriptor = _gamma_descriptor("a" * 32)
        grant = CommandFeedbackAdmissionGrant._issue(descriptor=descriptor)
        tracker = CommandFeedbackLifecycleFacade()
        active = SimpleNamespace(
            websocket=object(),
            session_id="session-1",
            generation=1,
            request_id="request-1",
            command_message_id="message-1",
            command=descriptor.command,
            source=descriptor.command_source,
            started_at_ms=1,
        )
        self.assertTrue(tracker.reserve(grant))
        self.assertTrue(
            tracker.bind_reserved(
                active,
                {
                    "input_event": {
                        "event_id": descriptor.event_id,
                        "source": descriptor.input_source,
                        "provider_id": descriptor.provider_id,
                        "event_kind": descriptor.event_kind,
                        "final": True,
                    }
                },
            )
        )

        permit = tracker.claim_start(grant, _accepted_submission_result())
        terminal = tracker.select_coalesced_terminal(permit)

        self.assertIsNotNone(terminal)
        self.assertEqual("accepted_without_result_callback", terminal.status)
        self.assertFalse(terminal.verified)
        self.assertFalse(terminal.dispatch_started)
        self.assertEqual(
            "감마 변경 명령은 보냈는데, 실제로 바뀌었는지는 확인하지 못했어",
            CommandLifecycleResponseRenderer().render_terminal(terminal),
        )
        resolution = tracker.acknowledge_publication(permit, True)
        self.assertTrue(resolution.accepted)
        self.assertTrue(resolution.retire_lifecycle)
        self.assertIsNone(tracker.context)

    def test_async_immediate_staged_terminal_is_selected_once_and_retires_on_ack(self):
        publications = CommandFeedbackPublicationCoordinator()
        lifecycle_token = object()
        terminal = object()
        start = publications.begin(
            lifecycle_token,
            CommandFeedbackLifecycleKindProfile.ASYNCHRONOUS_IMMEDIATE,
        )

        self.assertEqual(
            (None, False),
            publications.stage_terminal(lifecycle_token, terminal),
        )
        self.assertIs(
            terminal,
            publications.select_coalesced_terminal(lifecycle_token, start),
        )
        self.assertIsNone(
            publications.select_coalesced_terminal(lifecycle_token, start)
        )

        resolution = publications.acknowledge(start, True)

        self.assertTrue(resolution.accepted)
        self.assertTrue(resolution.retire_lifecycle)
        self.assertIsNone(resolution.terminal_response)

    def test_selection_before_terminal_seals_start_and_preserves_deferred_terminal(self):
        publications = CommandFeedbackPublicationCoordinator()
        lifecycle_token = object()
        terminal = object()
        start = publications.begin(
            lifecycle_token,
            CommandFeedbackLifecycleKindProfile.ASYNCHRONOUS_IMMEDIATE,
        )

        self.assertIsNone(
            publications.select_coalesced_terminal(lifecycle_token, start)
        )
        self.assertEqual(
            (None, False),
            publications.stage_terminal(lifecycle_token, terminal),
        )

        resolution = publications.acknowledge(start, True)

        self.assertTrue(resolution.accepted)
        self.assertTrue(resolution.retire_lifecycle)
        self.assertIs(terminal, resolution.terminal_response)

    def test_finite_or_persistent_lifecycle_never_coalesces_completed_terminal(self):
        for lifecycle_kind in (
            CommandFeedbackLifecycleKindProfile.FINITE_TASK,
            CommandFeedbackLifecycleKindProfile.PERSISTENT_TASK,
        ):
            with self.subTest(lifecycle_kind=lifecycle_kind):
                publications = CommandFeedbackPublicationCoordinator()
                lifecycle_token = object()
                terminal = object()
                start = publications.begin(lifecycle_token, lifecycle_kind)
                publications.stage_terminal(lifecycle_token, terminal)

                self.assertIsNone(
                    publications.select_coalesced_terminal(
                        lifecycle_token,
                        start,
                    )
                )
                resolution = publications.acknowledge(start, True)
                self.assertIs(terminal, resolution.terminal_response)

    def test_failed_start_after_selection_drops_terminal_and_retires(self):
        publications = CommandFeedbackPublicationCoordinator()
        lifecycle_token = object()
        start = publications.begin(
            lifecycle_token,
            CommandFeedbackLifecycleKindProfile.ASYNCHRONOUS_IMMEDIATE,
        )
        publications.stage_terminal(lifecycle_token, object())
        self.assertIsNotNone(
            publications.select_coalesced_terminal(lifecycle_token, start)
        )

        resolution = publications.acknowledge(start, False)

        self.assertTrue(resolution.accepted)
        self.assertTrue(resolution.retire_lifecycle)
        self.assertIsNone(resolution.terminal_response)


class CommandFeedbackReadyDecisionCoalescingTests(unittest.TestCase):
    def test_terminal_arriving_while_waiting_is_not_selected_during_decoration(self):
        renderer = CommandLifecycleResponseRenderer()
        for status in ("completed", "failed"):
            with self.subTest(status=status):
                descriptor = _gamma_descriptor(status[0] * 32)
                fact = CommandTerminalFact(
                    descriptor=descriptor,
                    status=status,
                    verified=False,
                    dispatch_started=True,
                    result_reason="test_terminal",
                    event_id=descriptor.event_id,
                    owner_token=object(),
                )
                acknowledgement = _TerminalDuringWaitAcknowledgement(fact)
                decision = MinecraftChatClefInputRouteDecision.handled_result(
                    reason="submitted",
                    response_text="technical",
                    publish_external_response=True,
                )

                decorated = CommandFeedbackStartDecisionDecorator(
                    renderer
                ).decorate(
                    decision,
                    descriptor=descriptor,
                    start_claimed=True,
                    publication_acknowledgement=acknowledgement,
                )

                self.assertEqual("command_start", decorated.response_kind)
                self.assertEqual([], acknowledgement.calls)
                ready_acknowledgement = (
                    decorated.response_publication_acknowledgement
                )
                self.assertIsInstance(
                    ready_acknowledgement,
                    CommandFeedbackReadyDecisionAcknowledgement,
                )
                self.assertTrue(
                    ready_acknowledgement.wait_until_ready(
                        timeout_seconds=0.1
                    )
                )

                resolved = ready_acknowledgement.resolve_ready_decision(
                    decorated
                )

                self.assertEqual(
                    ["wait", "select"],
                    acknowledgement.calls,
                )
                self.assertEqual("command_lifecycle", resolved.route_kind)
                self.assertEqual("command_coalesced", resolved.response_kind)
                self.assertEqual(
                    renderer.render_terminal(fact),
                    resolved.response_text,
                )
                self.assertNotEqual(
                    renderer.render_start(descriptor),
                    resolved.response_text,
                )
                self.assertIsNone(
                    ready_acknowledgement.resolve_ready_decision(decorated)
                )
                self.assertTrue(
                    ready_acknowledgement.acknowledge(published=True)
                )
                self.assertEqual(
                    ["wait", "select", "ack:True"],
                    acknowledgement.calls,
                )

    def test_ready_resolution_without_staged_terminal_keeps_start_decision(self):
        descriptor = _gamma_descriptor("e" * 32)
        acknowledgement = _TerminalDuringWaitAcknowledgement(None)
        decision = MinecraftChatClefInputRouteDecision.handled_result(
            reason="submitted",
            response_text="technical",
            publish_external_response=True,
        )
        decorated = CommandFeedbackStartDecisionDecorator(
            CommandLifecycleResponseRenderer()
        ).decorate(
            decision,
            descriptor=descriptor,
            start_claimed=True,
            publication_acknowledgement=acknowledgement,
        )
        ready_acknowledgement = decorated.response_publication_acknowledgement
        self.assertTrue(
            ready_acknowledgement.wait_until_ready(timeout_seconds=0.1)
        )

        resolved = ready_acknowledgement.resolve_ready_decision(decorated)

        self.assertIs(decorated, resolved)
        self.assertEqual("command_start", resolved.response_kind)

    def test_current_input_dispatch_coalesces_terminal_arriving_between_decoration_and_ready(self):
        renderer = CommandLifecycleResponseRenderer()
        descriptor = _gamma_descriptor("f" * 32)
        fact = CommandTerminalFact(
            descriptor=descriptor,
            status="failed",
            verified=False,
            dispatch_started=True,
            result_reason="test_failure",
            event_id=descriptor.event_id,
            owner_token=object(),
        )
        acknowledgement = _TerminalDuringWaitAcknowledgement(fact)
        decision = CommandFeedbackStartDecisionDecorator(renderer).decorate(
            MinecraftChatClefInputRouteDecision.handled_result(
                reason="submitted",
                response_text="technical",
                publish_external_response=True,
                response_emission_capability=object(),
            ),
            descriptor=descriptor,
            start_claimed=True,
            publication_acknowledgement=acknowledgement,
        )
        publisher = _RecordingPublisher()
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: publisher,
            router=SimpleNamespace(route=lambda _message: decision),
            log_callback=lambda _message: None,
        )

        outcome = coordinator.dispatch(
            SimpleNamespace(source="lavi_chat_ui", event_id=descriptor.event_id)
        )

        self.assertTrue(outcome.handled)
        self.assertEqual(1, len(publisher.calls))
        text, values = publisher.calls[0]
        self.assertEqual(renderer.render_terminal(fact), text)
        self.assertEqual("command_lifecycle", values["route_kind"])
        self.assertEqual("command_coalesced", values["response_kind"])
        self.assertEqual(
            ["wait", "select", "ack:True"],
            acknowledgement.calls,
        )


class _TerminalDuringWaitAcknowledgement:
    def __init__(self, terminal) -> None:
        self._terminal = terminal
        self.calls = []

    def wait_until_ready(self, *, timeout_seconds: float) -> bool:
        self.calls.append("wait")
        return timeout_seconds > 0

    def select_coalesced_terminal(self):
        self.calls.append("select")
        terminal = self._terminal
        self._terminal = None
        return terminal

    def acknowledge(self, *, published: bool) -> bool:
        self.calls.append(f"ack:{published}")
        return True


class _RecordingPublisher:
    def __init__(self) -> None:
        self.calls = []

    def emit_capability_response(self, text, **values):
        self.calls.append((text, values))
        return SimpleNamespace(output_delivered=True)


def _gamma_descriptor(event_id: str) -> CommandFeedbackDescriptor:
    return CommandFeedbackDescriptor(
        command_name="gamma",
        command="gamma 1.0",
        command_source="lavi_chat_ui",
        lifecycle_kind="immediate",
        phrase_profile_id="gamma_phrase_v1",
        evidence_profile_id="gamma_result_v1",
        rollout_state="cautious",
        event_id=event_id,
        input_source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        requested_family="settings",
        intent_kind="gamma",
        detail_level=CommandFeedbackDescriptor.RAW_TYPED,
        form_kind="explicit_value",
        response_lifecycle_kind=(
            CommandFeedbackLifecycleKindProfile.ASYNCHRONOUS_IMMEDIATE
        ),
        setting_value="1.0",
    )


def _accepted_submission_result():
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


if __name__ == "__main__":
    unittest.main()
