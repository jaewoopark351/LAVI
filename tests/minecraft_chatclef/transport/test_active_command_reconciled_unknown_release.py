#20260821_kpopmodder: Verify default-OFF shadow reconciliation and test-only guarded UNKNOWN release.
from __future__ import annotations

import unittest

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.input.routing import (
    MinecraftChatClefSubmissionPrecheck,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.deposit_reconciliation_profile import (
    DepositReconciliationProfileResolver,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.reconciliation_runtime_capability import (
    ReconciliationRuntimeCapability,
)


class ActiveCommandReconciledUnknownReleaseTests(unittest.TestCase):
    def test_default_off_shadow_reconciliation_does_not_release_active_or_leak_wire_snapshot(self):
        ownership, websocket = _active_ownership(
            reconcile_requested=True,
        )

        first = _accept_running_evidence(ownership, websocket, sequence=1)
        second = _accept_running_evidence(ownership, websocket, sequence=2)

        self.assertTrue(first.accepted)
        self.assertTrue(second.accepted)
        self.assertTrue(second.audit["would_reconcile_to_unknown"])
        self.assertFalse(second.audit["active_release_performed"])
        self.assertEqual(
            "runtime_readiness_unavailable",
            second.audit["feature_gate"]["blocked_reason"],
        )
        local = ownership.local_admission_snapshot()
        wire = ownership.wire_snapshot()
        self.assertEqual("request-1", local["active_request_id"])
        self.assertEqual("running", local["local_effective_result"]["status"])
        self.assertEqual("running", local["last_java_result"]["status"])
        self.assertEqual("running", wire["last_result"]["status"])
        self.assertNotIn("local_effective_result", wire)
        self.assertNotIn("admission_quarantine", wire)
        self.assertNotIn("tombstones", wire)

    def test_test_injected_runtime_ready_commits_reconciled_unknown_local_only_and_quarantines_admission(self):
        ownership, websocket = _active_ownership(
            reconcile_requested=True,
            runtime_capability=ReconciliationRuntimeCapability.for_test(),
        )

        _accept_running_evidence(ownership, websocket, sequence=1)
        outcome = _accept_running_evidence(ownership, websocket, sequence=2)

        self.assertTrue(outcome.accepted)
        self.assertTrue(outcome.audit["active_release_performed"])
        self.assertEqual("RECONCILED_UNKNOWN", outcome.audit["reconciliation_action"])
        local = ownership.local_admission_snapshot()
        wire = ownership.wire_snapshot()
        self.assertIsNone(local["active_request_id"])
        self.assertEqual("running", local["last_java_result"]["status"])
        self.assertEqual("unknown", local["local_effective_result"]["status"])
        self.assertEqual(
            "RECONCILED_UNKNOWN",
            local["local_effective_result"]["data"]["reconciliation_action"],
        )
        self.assertTrue(local["admission_quarantine"]["active"])
        self.assertEqual("running", wire["last_result"]["status"])
        self.assertNotIn("local_effective_result", wire)

        blocked = ownership.begin_command(
            request_id="request-2",
            command_message_id="message-2",
            command="deposit coal 1",
            source="unit-test",
        )

        self.assertIsNone(blocked)

    def test_quarantine_is_visible_to_router_precheck_after_guarded_release(self):
        ownership, websocket = _active_ownership(
            reconcile_requested=True,
            runtime_capability=ReconciliationRuntimeCapability.for_test(),
        )
        _accept_running_evidence(ownership, websocket, sequence=1)
        _accept_running_evidence(ownership, websocket, sequence=2)

        readiness = MinecraftChatClefSubmissionPrecheck().inspect(
            _StatusExtension(_bridge_status(ownership.local_admission_snapshot()))
        )

        self.assertFalse(readiness.ready)
        self.assertEqual("minecraft_command_quarantined", readiness.reason)
        self.assertEqual("java_retirement_unverified", readiness.error)

    def test_late_terminal_after_reconciled_unknown_is_audit_only(self):
        ownership, websocket = _active_ownership(
            reconcile_requested=True,
            runtime_capability=ReconciliationRuntimeCapability.for_test(),
        )
        _accept_running_evidence(ownership, websocket, sequence=1)
        _accept_running_evidence(ownership, websocket, sequence=2)

        late = _terminal_result()
        outcome = ownership.accept_result_and_reconcile(
            websocket=websocket,
            envelope=_envelope(
                message_id="terminal-late",
                payload=late.to_dict(),
            ),
            result=late,
            raw_payload=late.to_dict(),
        )

        local = ownership.local_admission_snapshot()
        audit = ownership.audit_snapshot()
        self.assertFalse(outcome.accepted)
        self.assertEqual("late_terminal_after_reconciliation", outcome.reason)
        self.assertIsNone(local["active_request_id"])
        self.assertEqual("running", local["last_java_result"]["status"])
        self.assertEqual("unknown", local["local_effective_result"]["status"])
        self.assertEqual(1, audit["tombstones"][0]["late_event_count"])

    def test_current_terminal_result_uses_normal_terminal_path_before_reconciliation(self):
        ownership, websocket = _active_ownership(
            reconcile_requested=True,
            runtime_capability=ReconciliationRuntimeCapability.for_test(),
        )
        terminal = _terminal_result(data=_stable_data(sequence=2))

        outcome = ownership.accept_result_and_reconcile(
            websocket=websocket,
            envelope=_envelope(message_id="terminal-1", payload=terminal.to_dict()),
            result=terminal,
            raw_payload=terminal.to_dict(),
        )

        local = ownership.local_admission_snapshot()
        self.assertTrue(outcome.accepted)
        self.assertEqual("normal_terminal", outcome.audit["path"])
        self.assertIsNone(local["active_request_id"])
        self.assertEqual("completed", local["last_java_result"]["status"])
        self.assertEqual("completed", local["local_effective_result"]["status"])
        self.assertFalse(local["admission_quarantine"]["active"])
        self.assertEqual([], ownership.audit_snapshot()["tombstones"])

    def test_strict_parser_rejects_string_true_evidence_for_shadow_release(self):
        ownership, websocket = _active_ownership(
            reconcile_requested=True,
            runtime_capability=ReconciliationRuntimeCapability.for_test(),
        )
        payload = _running_payload(sequence=1)
        payload["ok"] = "true"
        result = CommandResultDTO.from_mapping(payload)

        outcome = ownership.accept_result_and_reconcile(
            websocket=websocket,
            envelope=_envelope(message_id="result-1", payload=payload),
            result=result,
            raw_payload=payload,
        )

        self.assertTrue(outcome.accepted)
        self.assertEqual("raw_ok_not_true_bool", outcome.audit["decision_reason"])
        self.assertFalse(outcome.audit["would_reconcile_to_unknown"])
        self.assertEqual("request-1", ownership.snapshot()["active_request_id"])

    def test_contradictory_sequence_between_one_and_two_prevents_release(self):
        ownership, websocket = _active_ownership(
            reconcile_requested=True,
            runtime_capability=ReconciliationRuntimeCapability.for_test(),
        )

        _accept_running_evidence(ownership, websocket, sequence=1)
        middle = _accept_running_evidence(ownership, websocket, sequence=3)
        later = _accept_running_evidence(ownership, websocket, sequence=2)

        self.assertEqual("unexpected_evidence_sequence", middle.audit["decision_reason"])
        self.assertEqual(
            "sequence_two_without_sequence_one",
            later.audit["decision_reason"],
        )
        self.assertFalse(later.audit["active_release_performed"])
        self.assertEqual("request-1", ownership.snapshot()["active_request_id"])

    def test_canonical_deposit_profile_requires_exact_deposit_command(self):
        resolver = DepositReconciliationProfileResolver()

        accepted = resolver.resolve("/deposit diamond 2")
        rejected = resolver.resolve("deposit minecraft:diamond 2")

        self.assertTrue(accepted.allowed)
        self.assertEqual("deposit diamond 2", accepted.normalized_command)
        self.assertFalse(rejected.allowed)
        self.assertEqual("deposit_item_id_not_canonical", rejected.reason)


class _StatusExtension:
    def __init__(self, status: dict[str, object]):
        self._status = status

    def get_status(self) -> dict[str, object]:
        return self._status


def _active_ownership(
    *,
    reconcile_requested: bool = False,
    runtime_capability: ReconciliationRuntimeCapability | None = None,
):
    websocket = object()
    ownership = FabricChatClefConnectionOwnership(
        reconcile_stale_deposit_to_unknown_requested=reconcile_requested,
        reconciliation_runtime_capability=runtime_capability,
    )
    admission = ownership.try_activate(websocket=websocket, session_id="session-1")
    if not admission.accepted:
        raise AssertionError("fixture connection was not accepted")
    active = ownership.begin_command(
        request_id="request-1",
        command_message_id="message-1",
        command="deposit diamond 2",
        source="unit-test",
    )
    if active is None:
        raise AssertionError("fixture command was not accepted")
    return ownership, websocket


def _accept_running_evidence(
    ownership: FabricChatClefConnectionOwnership,
    websocket: object,
    *,
    sequence: int,
):
    payload = _running_payload(sequence=sequence)
    result = CommandResultDTO.from_mapping(payload)
    return ownership.accept_result_and_reconcile(
        websocket=websocket,
        envelope=_envelope(message_id=f"result-{sequence}", payload=payload),
        result=result,
        raw_payload=payload,
    )


def _running_payload(*, sequence: int) -> dict[str, object]:
    return {
        "request_id": "request-1",
        "ok": True,
        "status": "running",
        "error_code": None,
        "message": "stable running evidence",
        "data": _stable_data(sequence=sequence),
    }


def _terminal_result(data: dict[str, object] | None = None) -> CommandResultDTO:
    return CommandResultDTO(
        request_id="request-1",
        ok=True,
        status=CommandResultStatus.COMPLETED,
        message="completed",
        data=data
        or {
            "connection_generation": 1,
            "terminal_status": "COMPLETED",
        },
    )


def _stable_data(*, sequence: int) -> dict[str, object]:
    return {
        "connection_generation": 1,
        "dispatch_returned": True,
        "finish_callback_received": True,
        "task_finished_event_received": False,
        "waiting_reason": "waiting_for_task_finished_event",
        "classification": "nonterminal_diagnostic",
        "terminal_status": "NONE",
        "gameplay_effect": "UNVERIFIED",
        "lifecycle_evidence": {
            "version": "2026-08-20.java-nonterminal-evidence.v1",
            "stage": "stable_request_quiescence_observed",
            "evidence_sequence": sequence,
            "gameplay_effect": "UNVERIFIED",
        },
        "stable_request_quiescence": {
            "qualified": True,
            "same_session_generation": True,
            "request_root_reappeared": False,
        },
        "bound_root_task": {
            "class_name": "adris.altoclef.tasks.container.StoreInAnyContainerTask",
            "identity": "deposit-root-1",
            "relationship": "bound_request_root",
        },
        "current_root_task": {
            "class_name": "adris.altoclef.tasks.movement.IdleTask",
            "identity": "idle-root-1",
            "relationship": "current_root",
        },
    }


def _envelope(
    *,
    message_id: str,
    payload: dict[str, object],
) -> BridgeEnvelopeDTO:
    return BridgeEnvelopeDTO(
        protocol_version=1,
        message_type=BridgeMessageType.COMMAND_RESULT,
        message_id=message_id,
        correlation_id="message-1",
        session_id="session-1",
        timestamp_ms=1,
        payload=payload,
    )


def _bridge_status(commands: dict[str, object]) -> dict[str, object]:
    return {
        "backend_id": "fabric_chatclef",
        "enabled": True,
        "connected": True,
        "lifecycle_state": "connected",
        "details": {"commands": commands},
    }


if __name__ == "__main__":
    unittest.main()
