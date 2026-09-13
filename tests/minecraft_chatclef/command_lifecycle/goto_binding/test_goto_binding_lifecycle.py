#20260913_kpopmodder: Keep running binding immutable, ordered, and confined to one lifecycle.
from dataclasses import replace
import unittest

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.binding.goto.goto_task_binding_coordinator import GotoTaskBindingCoordinator
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.result.command_feedback_result_state_coordinator import CommandFeedbackResultStateCoordinator
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.state.command_feedback_lifecycle_state import CommandFeedbackLifecycleState
from .goto_binding_fixture import context, payload, running_data


class GotoBindingLifecycleTests(unittest.TestCase):
    def test_initial_running_frame_carries_binding_and_dispatch_start_together(self):
        state = CommandFeedbackLifecycleState()
        state.context = context()
        coordinator = CommandFeedbackResultStateCoordinator(state=state)
        self.assertTrue(coordinator.record_nonterminal(
            status="running", result_reason="dispatch_started", evidence_sequence=1,
            result_data=running_data(),
        ))
        self.assertTrue(state.dispatch_started_observed)
        self.assertEqual("64cddc1a", state.context.goto_binding.task_identity)

    def test_first_binding_is_frozen_and_replacement_rejection_is_sticky(self):
        binder = GotoTaskBindingCoordinator()
        original = context()
        bound = binder.bind(original, running_data())
        self.assertIsNone(original.goto_binding)
        self.assertIs(bound, binder.bind(bound, running_data()))
        rejected = binder.bind(bound, running_data(goto_binding=payload(task_identity="other")))
        self.assertTrue(rejected.goto_binding_rejected)
        self.assertEqual(bound.goto_binding, rejected.goto_binding)
        self.assertIs(rejected, binder.bind(rejected, running_data()))

    def test_invalid_first_binding_cannot_be_repaired_by_later_payload(self):
        binder = GotoTaskBindingCoordinator()
        for wire in (None, {}, running_data(goto_profile_version=True),
                     running_data(goto_binding=payload(session_id="other"))):
            with self.subTest(wire=wire):
                rejected = binder.bind(context(), wire)
                self.assertTrue(rejected.goto_binding_rejected)
                self.assertIsNone(rejected.goto_binding)
                self.assertIs(rejected, binder.bind(rejected, running_data()))

    def test_stale_duplicate_sequence_cannot_rebind_or_invalidate_task(self):
        state = CommandFeedbackLifecycleState()
        state.context = context()
        coordinator = CommandFeedbackResultStateCoordinator(state=state)
        self.assertTrue(coordinator.record_nonterminal(
            status="running", result_reason="dispatch_started", evidence_sequence=1,
        ))
        self.assertTrue(coordinator.record_nonterminal(
            status="running", result_reason="goto_task_bound", evidence_sequence=2,
            result_data=running_data(),
        ))
        bound = state.context
        self.assertFalse(coordinator.record_nonterminal(
            status="running", result_reason="goto_task_bound", evidence_sequence=2,
            result_data=running_data(goto_binding=payload(task_identity="other")),
        ))
        self.assertIs(bound, state.context)
        state.terminal_claimed = True
        self.assertFalse(coordinator.record_nonterminal(
            status="running", result_reason="goto_task_bound", evidence_sequence=3,
            result_data=running_data(),
        ))
        state.reset_active()
        state.context = context()
        self.assertIsNone(state.context.goto_binding)
        self.assertFalse(state.context.goto_binding_rejected)

    def test_correlation_checks_socket_owner_session_generation_and_request(self):
        state = CommandFeedbackLifecycleState()
        state.context = context()
        coordinator = CommandFeedbackResultStateCoordinator(state=state)
        expected = dict(websocket=state.context.websocket, owner_token=state.context.owner_token,
                        session_id="session-1", generation=1, request_id="request-1",
                        command_message_id="command-1")
        self.assertTrue(coordinator.matches_result(**expected))
        for name in expected:
            value = 2 if name == "generation" else object()
            self.assertFalse(coordinator.matches_result(**(expected | {name: value})))
