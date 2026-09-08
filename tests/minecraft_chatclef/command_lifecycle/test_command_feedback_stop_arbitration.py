#20260907_kpopmodder: Ensure STOP owns the only user-visible cancellation terminal.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackTracker,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackResultCoordinator,
    CommandTerminalEvidenceEvaluator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.terminal import (
    CommandStopTerminalArbitrator,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop import (
    StopControlIdentity,
    StopControlTargetSnapshot,
    StopControlTrackerRegistry,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_tracker import (
    StopControlTracker,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling import (
    StopControlTerminalResponsePublisher,
)
from plugins.Minecraft.fabric.chatclef.response.stop import StopControlResponseRenderer
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.active_command_reconciliation_outcome import (
    ActiveCommandReconciliationOutcome,
)


class CommandFeedbackStopArbitrationTests(unittest.TestCase):
    def test_persistent_task_completed_exit_is_retired_without_success_fact(self):
        tracker, ownership, websocket, active = _bound_follow()
        coordinator = CommandFeedbackResultCoordinator(
            tracker=tracker,
            evidence_evaluator=CommandTerminalEvidenceEvaluator(),
        )
        result = CommandResultDTO(
            request_id="request-follow",
            ok=True,
            status=CommandResultStatus.COMPLETED,
            data={"result_reason": "matching_task_finished"},
        )

        fact = coordinator.accept_fact(
            websocket=websocket,
            envelope=_envelope(result),
            result=result,
            outcome=_accepted_outcome(active),
            expected_active=active,
        )

        self.assertIsNone(fact)
        self.assertIsNone(tracker.context)
        self.assertTrue(tracker.reserve(_grant("f" * 32)))

    def test_original_cancellation_without_live_stop_keeps_terminal(self):
        tracker, ownership, websocket, active = _bound_follow()
        coordinator = CommandFeedbackResultCoordinator(
            tracker=tracker,
            evidence_evaluator=CommandTerminalEvidenceEvaluator(),
        )
        result = CommandResultDTO(
            request_id="request-follow",
            ok=False,
            status=CommandResultStatus.CANCELLED,
            data={"result_reason": "user_stop_requested"},
        )

        fact = coordinator.accept_fact(
            websocket=websocket,
            envelope=_envelope(result),
            result=result,
            outcome=_accepted_outcome(active),
            expected_active=active,
        )
        duplicate = coordinator.accept_fact(
            websocket=websocket,
            envelope=_envelope(result),
            result=result,
            outcome=_accepted_outcome(active),
            expected_active=active,
        )

        self.assertIsNotNone(fact)
        self.assertEqual("cancelled", fact.status)
        self.assertIsNone(duplicate)
        self.assertIsNone(tracker.context)
        self.assertTrue(tracker.reserve(_grant("e" * 32)))

    def test_exact_live_stop_owns_one_terminal_for_original_cancellation(self):
        tracker, ownership, websocket, active = _bound_follow()
        stop_registry = StopControlTrackerRegistry()
        stop_tracker = _stop_tracker(websocket, active)
        self.assertTrue(stop_registry.register(stop_tracker))
        coordinator = CommandFeedbackResultCoordinator(
            tracker=tracker,
            evidence_evaluator=CommandTerminalEvidenceEvaluator(),
            stop_terminal_arbitrator=CommandStopTerminalArbitrator(
                stop_registry
            ),
        )
        result = CommandResultDTO(
            request_id="request-follow",
            ok=False,
            status=CommandResultStatus.CANCELLED,
            data={"result_reason": "user_stop_requested"},
        )

        fact = coordinator.accept_fact(
            websocket=websocket,
            envelope=_envelope(result),
            result=result,
            outcome=_accepted_outcome(active),
            expected_active=active,
        )
        published = []
        publisher = StopControlTerminalResponsePublisher(
            terminal_listener=SimpleNamespace(publish=published.append),
            renderer=StopControlResponseRenderer(),
            diagnostic_reporter=SimpleNamespace(warn=lambda _message: None),
        )
        decision = SimpleNamespace(
            status="completed",
            control_outcome="stopped",
            reason="stopped",
        )
        publisher.publish(tracker=stop_tracker, decision=decision)
        publisher.publish(tracker=stop_tracker, decision=decision)

        self.assertIsNone(fact)
        self.assertIsNone(tracker.context)
        self.assertEqual(1, len(published))
        self.assertEqual("멈췄어", published[0].text)
        self.assertEqual("stop_terminal", published[0].response_kind)

    def test_mismatched_or_quarantined_stop_cannot_suppress_original(self):
        tracker, ownership, websocket, active = _bound_follow()
        stop_registry = StopControlTrackerRegistry()
        other_active = SimpleNamespace(
            request_id="other-request",
            command_message_id="other-message",
            session_id=active.session_id,
            generation=active.generation,
        )
        stop_tracker = _stop_tracker(websocket, other_active)
        self.assertTrue(stop_registry.register(stop_tracker))
        arbitrator = CommandStopTerminalArbitrator(stop_registry)

        self.assertFalse(
            arbitrator.specialized_stop_owns_terminal(
                status=CommandResultStatus.CANCELLED,
                result_reason="user_stop_requested",
                expected_active=active,
            )
        )
        stop_tracker.enter_quarantine()
        self.assertFalse(
            arbitrator.specialized_stop_owns_terminal(
                status=CommandResultStatus.CANCELLED,
                result_reason="user_stop_requested",
                expected_active=other_active,
            )
        )


def _bound_follow():
    tracker = CraftingFeedbackTracker()
    ownership = FabricChatClefConnectionOwnership(
        crafting_feedback_tracker=tracker
    )
    websocket = object()
    if not ownership.try_activate(
        websocket=websocket,
        session_id="session-follow",
    ).accepted:
        raise AssertionError("fixture connection failed")
    grant = _grant("d" * 32)
    if not ownership.reserve_command_feedback(grant):
        raise AssertionError("fixture reservation failed")
    active = ownership.begin_command(
        request_id="request-follow",
        command_message_id="message-follow",
        command="@follow Steve",
        source="lavi_gui",
        metadata=_metadata("d" * 32),
    )
    if active is None:
        raise AssertionError("fixture command binding failed")
    permit = tracker.claim_start(grant, _accepted_submission_result())
    if permit is None:
        raise AssertionError("fixture start claim failed")
    tracker.acknowledge_publication(permit, True)
    return tracker, ownership, websocket, active


def _grant(event_id: str):
    factory = CommandFeedbackDescriptorFactory()
    descriptor = factory.decode_registered_command_name_only(
        "@follow Steve",
        command_source="lavi_gui",
        event_id=event_id,
        provider_id="minecraft_gui",
        event_kind="raw_command_submit",
    )
    if descriptor is None:
        raise AssertionError("fixture descriptor failed")
    return CommandFeedbackAdmissionCoordinator(
        live_proof_validator=lambda _proof, _event: False,
        descriptor_factory=factory,
    ).issue_descriptor(descriptor)


def _stop_tracker(websocket, active):
    return StopControlTracker(
        identity=StopControlIdentity(
            request_id="stop-request",
            message_id="stop-message",
            session_id=active.session_id,
            server_connection_generation=active.generation,
        ),
        barrier_token=object(),
        transport_websocket=websocket,
        request=SimpleNamespace(metadata={"target_scope": "tracked_command"}),
        event_id="9" * 32,
        source="lavi_chat_ui",
        target_scope="tracked_command",
        target=StopControlTargetSnapshot(
            request_id=active.request_id,
            command_message_id=active.command_message_id,
            session_id=active.session_id,
            server_connection_generation=active.generation,
            owner_token=active,
        ),
    )


def _metadata(event_id: str):
    return {
        "input_event": {
            "source": "lavi_gui",
            "provider_id": "minecraft_gui",
            "event_kind": "raw_command_submit",
            "final": True,
            "event_id": event_id,
        }
    }


def _accepted_submission_result():
    return {
        "ok": True,
        "status": {
            "request_id": "request-follow",
            "ok": True,
            "status": "accepted",
            "data": {
                "session_id": "session-follow",
                "connection_generation": 1,
                "command_message_id": "message-follow",
            },
        },
    }


def _envelope(result):
    return BridgeEnvelopeDTO(
        protocol_version=1,
        message_type=BridgeMessageType.COMMAND_RESULT,
        message_id="result-follow",
        correlation_id="message-follow",
        session_id="session-follow",
        timestamp_ms=1,
        payload=result.to_dict(),
    )


def _accepted_outcome(active):
    return ActiveCommandReconciliationOutcome(
        accepted=True,
        reason="accepted",
        before_snapshot={
            "active_session_id": active.session_id,
            "active_generation": active.generation,
            "active_request_id": active.request_id,
            "active_command_message_id": active.command_message_id,
            "active_command": active.command,
            "active_command_source": active.source,
            "active_started_at_ms": active.started_at_ms,
        },
        after_snapshot={},
    )


if __name__ == "__main__":
    unittest.main()
