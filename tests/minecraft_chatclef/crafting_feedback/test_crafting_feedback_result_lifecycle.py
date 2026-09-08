#20260907_kpopmodder: Verify terminal correlation, cleanup, and fault containment.
from __future__ import annotations

import threading
import unittest

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle import (
    CraftingLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackAdmissionGrant,
    CraftingFeedbackEffectVerifier,
    CraftingFeedbackResultCoordinator,
    CraftingFeedbackTerminalDelivery,
    CraftingFeedbackTerminalListener,
    CraftingFeedbackTracker,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_result_handler import (
    FabricChatClefCommandResultHandler,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.active_command_reconciliation_outcome import (
    ActiveCommandReconciliationOutcome,
)


class CraftingFeedbackResultLifecycleTests(unittest.TestCase):
    def test_failure_wording_depends_on_accepted_dispatch_started_evidence(self):
        for dispatch_started, expected in (
            (False, "다이아 곡괭이 만들지 못했어"),
            (True, "다이아 곡괭이 만들다가 실패했어"),
        ):
            with self.subTest(dispatch_started=dispatch_started):
                tracker, ownership, websocket, active = _bound_ownership()
                coordinator = _coordinator(tracker)
                if dispatch_started:
                    coordinator.accept(
                        websocket=websocket,
                        envelope=_envelope(_running()),
                        result=_running(),
                        outcome=_accepted_outcome(active),
                        expected_active=active,
                    )

                response = coordinator.accept(
                    websocket=websocket,
                    envelope=_envelope(_failed()),
                    result=_failed(),
                    outcome=_accepted_outcome(active),
                    expected_active=active,
                )

                self.assertIsNotNone(response)
                self.assertEqual(expected, response.text)
                self.assertIn(
                    "diamond_pickaxe",
                    response.presentation_detail_log,
                )
                self.assertNotIn("diamond_pickaxe", response.text)

    def test_accepted_terminal_correlation_contradiction_retires_old_context(self):
        tracker, _ownership, _websocket, active = _bound_ownership()

        response = _coordinator(tracker).accept(
            websocket=object(),
            envelope=_envelope(_completed()),
            result=_completed(),
            outcome=_accepted_outcome(active),
            expected_active=active,
        )

        self.assertIsNone(response)
        self.assertIsNone(tracker.context)
        self.assertTrue(tracker.reserve(_grant("e" * 32)))

    def test_throwing_terminal_listener_is_contained_and_never_replayed(self):
        tracker, ownership, websocket, _active = _bound_ownership()
        diagnostics = _Diagnostics()
        listener = CraftingFeedbackTerminalListener()
        callback_count = []

        def fail_delivery(_response):
            callback_count.append(1)
            raise RuntimeError("private callback failure")

        listener.set_callback(fail_delivery)
        handler = FabricChatClefCommandResultHandler(
            connection_ownership=ownership,
            command_lock=threading.RLock(),
            diagnostics=diagnostics,
            crafting_feedback_result_coordinator=_coordinator(tracker),
            crafting_feedback_terminal_delivery=CraftingFeedbackTerminalDelivery(
                terminal_listener=listener,
                diagnostics=diagnostics,
            ),
        )

        handler.handle(websocket, _envelope(_running()))
        handler.handle(websocket, _envelope(_completed()))
        handler.handle(websocket, _envelope(_completed()))

        self.assertEqual([1], callback_count)
        self.assertIsNone(tracker.context)
        self.assertEqual(
            1,
            sum(
                message.startswith("crafting terminal response delivery failed")
                for message in diagnostics.warnings
            ),
        )
        self.assertNotIn("private callback failure", " ".join(diagnostics.warnings))
        self.assertTrue(tracker.reserve(_grant("f" * 32)))


def _bound_ownership():
    tracker = CraftingFeedbackTracker()
    ownership = FabricChatClefConnectionOwnership(
        crafting_feedback_tracker=tracker
    )
    websocket = object()
    admission = ownership.try_activate(
        websocket=websocket,
        session_id="session-1",
    )
    if not admission.accepted:
        raise AssertionError("fixture connection failed")
    grant = _grant("d" * 32)
    if not ownership.reserve_crafting_feedback(grant):
        raise AssertionError("fixture reservation failed")
    active = ownership.begin_command(
        request_id="request-1",
        command_message_id="message-1",
        command="get diamond_pickaxe 1",
        source="lavi_chat_ui",
        metadata=_metadata("d" * 32),
    )
    if active is None or tracker.context is None:
        raise AssertionError("fixture binding failed")
    permit = tracker.claim_start(grant, _accepted_submission_result())
    if permit is None:
        raise AssertionError("fixture start claim failed")
    tracker.acknowledge_publication(permit, True)
    return tracker, ownership, websocket, active


def _coordinator(tracker):
    return CraftingFeedbackResultCoordinator(
        tracker=tracker,
        effect_verifier=CraftingFeedbackEffectVerifier(),
        response_renderer=CraftingLifecycleResponseRenderer(),
    )


def _grant(event_id: str):
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


def _metadata(event_id: str):
    return {
        "input_event": {
            "source": "lavi_chat_ui",
            "provider_id": "lavi_chat_ui",
            "event_kind": "chat_submit",
            "final": True,
            "event_id": event_id,
        }
    }


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


def _running():
    return CommandResultDTO(
        request_id="request-1",
        ok=True,
        status=CommandResultStatus.RUNNING,
        data={
            "result_reason": "dispatch_started",
            "evidence_sequence": 1,
        },
    )


def _failed():
    return CommandResultDTO(
        request_id="request-1",
        ok=False,
        status=CommandResultStatus.FAILED,
        data={"result_reason": "task_failed"},
    )


def _completed():
    return CommandResultDTO(
        request_id="request-1",
        ok=True,
        status=CommandResultStatus.COMPLETED,
        data={
            "result_reason": "matching_task_finished",
            "result_fidelity": "callback_plus_matching_user_task_event",
            "effect_kind": "get_acquisition_delta",
            "target_item": "diamond_pickaxe",
            "requested_count": 1,
            "before_target_count": 2,
            "after_target_count": 3,
            "target_count_delta": 1,
            "effect_observation_status": "authoritative",
        },
    )


def _envelope(result):
    return BridgeEnvelopeDTO(
        protocol_version=1,
        message_type=BridgeMessageType.COMMAND_RESULT,
        message_id="result-message-1",
        correlation_id="message-1",
        session_id="session-1",
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


class _Diagnostics:
    def __init__(self):
        self.infos = []
        self.warnings = []

    def info(self, message):
        self.infos.append(message)

    def warning(self, message):
        self.warnings.append(message)


if __name__ == "__main__":
    unittest.main()
