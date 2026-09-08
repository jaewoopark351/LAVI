#20260908_kpopmodder: Prove correlated STORE_HOME evidence reaches one projected Korean terminal response.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.terminal import (
    CraftingFeedbackTerminalPresenter,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackEffectVerifier,
    CraftingFeedbackResultCoordinator,
    CraftingFeedbackTracker,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.active_command_reconciliation_outcome import (
    ActiveCommandReconciliationOutcome,
)


class StoreHomeTerminalIntegrationTests(unittest.TestCase):
    def test_matching_terminal_projects_count_and_duplicate_cannot_replay(self):
        tracker, websocket, active = _bound_store_home()
        coordinator = _coordinator(tracker)
        result = _completed_store_home(909)
        values = _accept_values(websocket, active, result)

        response = coordinator.accept(**values)
        duplicate = coordinator.accept(**values)

        self.assertIsNotNone(response)
        self.assertEqual("아이템 909개를 집에 정리했어", response.text)
        self.assertEqual("store_home", response.command_name)
        self.assertIsNone(duplicate)

    def test_identity_mismatch_creates_no_fact_or_response(self):
        tracker, _websocket, active = _bound_store_home()
        presenter = _RecordingPresenter()
        coordinator = CraftingFeedbackResultCoordinator(
            tracker=tracker,
            effect_verifier=CraftingFeedbackEffectVerifier(),
            response_renderer=presenter,
        )
        result = _completed_store_home(909)

        response = coordinator.accept(
            **_accept_values(object(), active, result)
        )

        self.assertIsNone(response)
        self.assertEqual([], presenter.facts)


def _bound_store_home():
    tracker = CraftingFeedbackTracker()
    ownership = FabricChatClefConnectionOwnership(
        crafting_feedback_tracker=tracker
    )
    websocket = object()
    if not ownership.try_activate(
        websocket=websocket,
        session_id="session-store-home",
    ).accepted:
        raise AssertionError("fixture connection failed")
    event = SimpleNamespace(
        text="아이템 집에 정리해줘",
        source="lavi_chat_ui",
        provider_id="lavi_chat_ui",
        event_kind="chat_submit",
        final=True,
        event_id="a" * 32,
    )
    descriptor_factory = CommandFeedbackDescriptorFactory()
    descriptor = descriptor_factory.from_trusted_translation(
        event=event,
        translation={
            "status": "validated",
            "executable": True,
            "command": "store_home",
            "resolved_target": None,
            "intent": {
                "intent_type": "store_home",
                "language": "ko",
                "original_text": event.text,
            },
        },
    )
    grant = CommandFeedbackAdmissionCoordinator(
        live_proof_validator=lambda _proof, _event: False,
        descriptor_factory=descriptor_factory,
    ).issue_descriptor(descriptor)
    if grant is None or not ownership.reserve_command_feedback(grant):
        raise AssertionError("fixture reservation failed")
    active = ownership.begin_command(
        request_id="request-store-home",
        command_message_id="message-store-home",
        command="store_home",
        source="lavi_chat_ui",
        metadata={
            "input_event": {
                "source": event.source,
                "provider_id": event.provider_id,
                "event_kind": event.event_kind,
                "final": True,
                "event_id": event.event_id,
            }
        },
    )
    if active is None:
        raise AssertionError("fixture command binding failed")
    permit = tracker.claim_start(
        grant,
        {
            "ok": True,
            "status": {
                "request_id": "request-store-home",
                "ok": True,
                "status": "accepted",
                "data": {
                    "session_id": "session-store-home",
                    "connection_generation": 1,
                    "command_message_id": "message-store-home",
                },
            },
        },
    )
    if permit is None:
        raise AssertionError("fixture start claim failed")
    tracker.acknowledge_publication(permit, True)
    return tracker, websocket, active


def _coordinator(tracker):
    return CraftingFeedbackResultCoordinator(
        tracker=tracker,
        effect_verifier=CraftingFeedbackEffectVerifier(),
        response_renderer=CraftingFeedbackTerminalPresenter(
            response_renderer=CommandLifecycleResponseRenderer()
        ),
    )


def _completed_store_home(stored_items: int) -> CommandResultDTO:
    return CommandResultDTO(
        request_id="request-store-home",
        ok=True,
        status=CommandResultStatus.COMPLETED,
        data={
            "result_reason": "matching_task_finished",
            "result_fidelity": "callback_plus_matching_user_task_event",
            "operation": "store_home",
            "store_home_result": "COMPLETED",
            "stored_items": stored_items,
            "remaining_stacks": 0,
            "reason": "matching_task_finished",
            "goal_satisfied": True,
        },
    )


def _accept_values(websocket, active, result):
    return {
        "websocket": websocket,
        "envelope": BridgeEnvelopeDTO(
            protocol_version=1,
            message_type=BridgeMessageType.COMMAND_RESULT,
            message_id="result-store-home",
            correlation_id="message-store-home",
            session_id="session-store-home",
            timestamp_ms=1,
            payload=result.to_dict(),
        ),
        "result": result,
        "outcome": ActiveCommandReconciliationOutcome(
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
        ),
        "expected_active": active,
    }


class _RecordingPresenter:
    def __init__(self) -> None:
        self.facts = []

    def present(self, fact):
        self.facts.append(fact)
        return object()


if __name__ == "__main__":
    unittest.main()
