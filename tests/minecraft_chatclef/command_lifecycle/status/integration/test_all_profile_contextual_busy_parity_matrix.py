#20260909_kpopmodder: Verify contextual busy parity across every active lifecycle profile.
from __future__ import annotations

import unittest

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean import (
    TrustedKoreanProofValidator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response.contextual_busy import (
    ContextualBusyResponseCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptor,
    CommandFeedbackDescriptorFactory,
)

from .contextual_status_route_harness import ContextualStatusRouteHarness
from .test_all_profile_contextual_status_route_matrix import (
    PROFILE_FIXTURES,
    _accepted_submission_result,
    _active_identity,
    _reconciled_running_graph,
)
from .trusted_status_input_fixture import issue_trusted_status_input


class AllProfileContextualBusyParityMatrixTests(unittest.TestCase):
    def test_all_25_active_profiles_match_independent_explicit_status_inspection(self):
        proof_owner = object()
        _registry, status_event, proof = issue_trusted_status_input(
            voice=False,
            event_id="a" * 32,
            proof_owner=proof_owner,
        )
        busy_event = _busy_event()
        descriptors = CommandFeedbackDescriptorFactory()
        routed = 0
        try:
            for command_name, (command, expected_text) in PROFILE_FIXTURES.items():
                if command_name == "stop":
                    continue
                with self.subTest(command_name=command_name):
                    descriptor = descriptors.decode_registered_command_name_only(
                        command,
                        command_source="lavi_gui",
                        event_id="b" * 32,
                        provider_id="minecraft_fabric_chatclef_ui",
                        event_kind="minecraft_raw_gui_submit",
                    )
                    self.assertIs(type(descriptor), CommandFeedbackDescriptor)
                    graph, active, deferred_start = _reconciled_running_graph(
                        descriptor=descriptor,
                        descriptor_factory=descriptors,
                    )
                    active_identity = _active_identity(active)
                    harness = ContextualStatusRouteHarness(
                        proof_validator=TrustedKoreanProofValidator(
                            owner=proof_owner,
                        ).is_live,
                        status_inspector=graph.command_feedback_api.inspect_status,
                    )
                    explicit_status = harness.route(
                        event=status_event,
                        proof=proof,
                        descriptor=descriptor,
                    )
                    busy_snapshots = []
                    coordinator = ContextualBusyResponseCoordinator(
                        inspect_busy_status_callback=lambda identity: (
                            busy_snapshots.append(
                                graph.command_feedback_api.inspect_busy_status(identity)
                            )
                            or busy_snapshots[-1]
                        ),
                        response_renderer=harness.renderer,
                    )
                    rejected = _busy_decision(active)
                    original_result = rejected.result
                    original_translation = rejected.translation

                    preparation = coordinator.prepare(
                        decision=rejected,
                        event=busy_event,
                        proof=proof,
                        live_proof_validator=lambda value, event: (
                            value is proof and event is busy_event
                        ),
                    )
                    contextual_busy = preparation.decision

                    self.assertEqual(expected_text, explicit_status.response_text)
                    self.assertEqual(explicit_status.response_text, contextual_busy.response_text)
                    self.assertEqual("minecraft_command_busy", contextual_busy.reason)
                    self.assertIs(original_result, contextual_busy.result)
                    self.assertIs(original_translation, contextual_busy.translation)
                    self.assertEqual(
                        "command_busy_current_work",
                        contextual_busy.route_kind,
                    )
                    self.assertEqual("command_status", contextual_busy.response_kind)
                    self.assertEqual(1, len(busy_snapshots))
                    self.assertIsNot(harness.last_snapshot, busy_snapshots[0])
                    self.assertIs(descriptor, harness.last_snapshot.descriptor)
                    self.assertIs(descriptor, busy_snapshots[0].descriptor)
                    self.assertEqual(harness.last_snapshot.state, busy_snapshots[0].state)
                    status_ack = explicit_status.response_publication_acknowledgement
                    busy_ack = contextual_busy.response_publication_acknowledgement
                    self.assertIsNot(status_ack, busy_ack)
                    self.assertIs(busy_ack, preparation.local_handoff_token)
                    self.assertNotIn("pumpkin_pie", contextual_busy.response_text)
                    self.assertNotIn(
                        "pumpkin_pie",
                        contextual_busy.presentation_detail_log,
                    )
                    self.assertEqual(
                        active_identity,
                        _active_identity(
                            graph.connection_ownership.active_command_owner
                        ),
                    )

                    if deferred_start is not None:
                        start_ack = graph.command_feedback_api.claim_start(
                            deferred_start,
                            _accepted_submission_result(active),
                        )
                        self.assertIsNotNone(start_ack)
                        self.assertTrue(start_ack.acknowledge(published=True))
                    self.assertTrue(status_ack.acknowledge(published=True))
                    self.assertTrue(busy_ack.acknowledge(published=True))
                    routed += 1
        finally:
            proof.close()

        self.assertEqual(25, routed)


def _busy_decision(active):
    return MinecraftChatClefInputRouteDecision.handled_result(
        reason="minecraft_command_busy",
        response_text="generic busy",
        result={
            "ok": False,
            "error": "active_command",
            "status": {
                "details": {
                    "commands": {
                        "active_session_id": active.session_id,
                        "active_generation": active.generation,
                        "active_request_id": active.request_id,
                        "active_command_message_id": active.command_message_id,
                    }
                }
            },
        },
        translation={"command": "get pumpkin_pie 99"},
        route_kind="minecraft_command",
    )


def _busy_event():
    return LaviInputEvent(
        text="호박 파이 99개 만들어줘",
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id="c" * 32,
        fallback_payload="호박 파이 99개 만들어줘",
    )


if __name__ == "__main__":
    unittest.main()
