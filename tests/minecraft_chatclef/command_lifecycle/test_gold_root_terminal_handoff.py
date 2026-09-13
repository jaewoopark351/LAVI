#20260913_kpopmodder: Verify the Java root-replacement failure contract through the real Python lifecycle.
from __future__ import annotations

from types import SimpleNamespace

import pytest

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import CommandLifecycleResponseRenderer
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackAdmissionCoordinator,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleFacade,
    CommandFeedbackResultCoordinator,
    CommandTerminalEvidenceEvaluator,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)


@pytest.mark.parametrize("reason", ["command_root_replaced", "command_callback_ownership_replaced"])
def test_replacement_failure_is_correlated_and_claimed_once(reason):
    tracker, coordinator, websocket, active, outcome, ownership = _bound_gold()
    result, envelope = _failure(reason)
    arguments = dict(websocket=websocket, envelope=envelope, result=result,
                     outcome=outcome, expected_active=active)

    fact = coordinator.accept_fact(**arguments)
    duplicate = coordinator.accept_fact(**arguments)

    assert fact is not None
    assert fact.status == "failed"
    assert fact.result_reason == reason
    assert not fact.verified
    assert fact.dispatch_started
    assert duplicate is None
    text = CommandLifecycleResponseRenderer().render_terminal(fact)
    assert "실패" in text
    assert "다 구했어" not in text
    assert tracker.context is None


@pytest.mark.parametrize("mismatch", ["request", "session", "message", "websocket"])
def test_late_foreign_failure_cannot_claim_current_gold_execution(mismatch):
    tracker, coordinator, websocket, active, outcome, ownership = _bound_gold()
    result, envelope = _failure("command_root_replaced", mismatch)
    received_socket = object() if mismatch == "websocket" else websocket
    rejected = ownership.accept_result_and_reconcile(
        websocket=received_socket, envelope=envelope, result=result,
        raw_payload=result.to_dict(),
    )
    assert not rejected.accepted
    assert ownership.active_command_owner is active
    fact = coordinator.accept_fact(
        websocket=received_socket,
        envelope=envelope, result=result,
        outcome=rejected,
        expected_active=active,
    )
    assert fact is None
    assert tracker.context is not None
    valid_result, valid_envelope = _failure("command_root_replaced")
    assert coordinator.accept_fact(websocket=websocket, envelope=valid_envelope,
                                   result=valid_result, outcome=outcome,
                                   expected_active=active) is not None


def _bound_gold():
    factory = CommandFeedbackDescriptorFactory()
    descriptor = factory.decode_registered_command_name_only(
        "@get gold_ingot 10", command_source="lavi_gui", event_id="b" * 32,
        provider_id="minecraft_gui", event_kind="raw_command_submit",
    )
    assert descriptor is not None
    grant = CommandFeedbackAdmissionCoordinator(
        live_proof_validator=lambda _proof, _event: False,
        descriptor_factory=factory,
    ).issue_descriptor(descriptor)
    tracker = CommandFeedbackLifecycleFacade()
    ownership = FabricChatClefConnectionOwnership(crafting_feedback_tracker=tracker)
    websocket = object()
    assert ownership.try_activate(websocket=websocket, session_id="gold-session").accepted
    assert ownership.reserve_command_feedback(grant)
    active = ownership.begin_command(
        request_id="gold-request", command_message_id="gold-message",
        command=descriptor.command, source=descriptor.command_source,
        metadata={"input_event": {"source": descriptor.input_source,
                  "provider_id": descriptor.provider_id, "event_kind": descriptor.event_kind,
                  "final": True, "event_id": descriptor.event_id}},
    )
    assert active is not None
    permit = tracker.claim_start(grant, {
        "ok": True, "status": {"request_id": active.request_id, "ok": True,
        "status": "accepted", "data": {"session_id": active.session_id,
        "connection_generation": active.generation, "command_message_id": active.command_message_id}},
    })
    assert permit is not None
    tracker.acknowledge_publication(permit, True)
    tracker.record_nonterminal(status="running", result_reason="dispatch_started", evidence_sequence=1)
    outcome = SimpleNamespace(accepted=True, reason="accepted", before_snapshot={
        "active_session_id": active.session_id, "active_generation": active.generation,
        "active_request_id": active.request_id, "active_command_message_id": active.command_message_id,
        "active_command": active.command, "active_command_source": active.source,
        "active_started_at_ms": active.started_at_ms,
    })
    return tracker, CommandFeedbackResultCoordinator(
        tracker=tracker, evidence_evaluator=CommandTerminalEvidenceEvaluator(),
    ), websocket, active, outcome, ownership


def _failure(reason, mismatch=""):
    result = CommandResultDTO(
        request_id="old-request" if mismatch == "request" else "gold-request",
        ok=False, status=CommandResultStatus.FAILED, data={"result_reason": reason},
    )
    return result, BridgeEnvelopeDTO(
        protocol_version=1, message_type=BridgeMessageType.COMMAND_RESULT,
        message_id="gold-terminal", timestamp_ms=1, payload=result.to_dict(),
        correlation_id="old-message" if mismatch == "message" else "gold-message",
        session_id="old-session" if mismatch == "session" else "gold-session",
    )
