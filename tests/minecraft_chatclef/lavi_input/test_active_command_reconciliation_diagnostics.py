#20260820_kpopmodder: Verify stale active-command reconciliation diagnostics stay dry-run only.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input import MinecraftChatClefInputRouter
from plugins.Minecraft.fabric.chatclef.input.routing import (
    MinecraftChatClefSubmissionPrecheck,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation import (
    ActiveCommandReconciliationDiagnosticBuilder,
)


class ActiveCommandReconciliationDiagnosticTests(unittest.TestCase):
    def test_running_deposit_with_missing_finish_callback_stays_blocked(self):
        diagnostic = ActiveCommandReconciliationDiagnosticBuilder().build(
            _active_commands(finish_callback_received=False)
        )

        self.assertEqual("deposit", diagnostic["command_profile"]["command_name"])
        self.assertTrue(
            diagnostic["command_profile"]["auto_reconcile_to_unknown_allowed"]
        )
        self.assertEqual("EXACT", diagnostic["identity_quality"])
        self.assertEqual(
            "running",
            diagnostic["lifecycle_evidence"]["last_result_status"],
        )
        self.assertFalse(diagnostic["would_reconcile_to_unknown"])
        self.assertFalse(diagnostic["active_release_performed"])
        self.assertEqual("dry_run", diagnostic["reconciliation_mode"])
        self.assertEqual("UNVERIFIED", diagnostic["gameplay_effect"])
        self.assertEqual("NONE", diagnostic["terminal_status"])
        self.assertEqual(
            "missing_finish_callback_evidence",
            diagnostic["blocked_reason"],
        )
        self.assertIn(
            "missing_stable_request_quiescence_evidence",
            diagnostic["blocked_reasons"],
        )
        self.assertEqual(0, diagnostic["retry_count"])
        self.assertEqual(0, diagnostic["replay_count"])
        self.assertFalse(diagnostic["auto_stop_requested"])
        self.assertFalse(diagnostic["follow_up_command_submitted"])

    def test_exact_finish_callback_and_stable_idle_root_is_dry_run_eligible(self):
        diagnostic = ActiveCommandReconciliationDiagnosticBuilder().build(
            _active_commands(
                finish_callback_received=True,
                stable_quiescence=True,
            )
        )

        self.assertTrue(diagnostic["would_reconcile_to_unknown"])
        self.assertEqual("ELIGIBLE_DRY_RUN", diagnostic["reconciliation_decision"])
        self.assertEqual("unknown", diagnostic["synthetic_result_status_if_enabled"])
        self.assertEqual(
            "stable_request_quiescence_observed",
            diagnostic["lifecycle_evidence"]["lifecycle_evidence_stage"],
        )
        self.assertEqual(2, diagnostic["lifecycle_evidence"]["evidence_sequence"])
        self.assertEqual(
            "fabric-session-1",
            diagnostic["accepted_evidence_provenance"]["accepted_session_id"],
        )
        self.assertEqual(
            "RECONCILED_UNKNOWN",
            diagnostic["terminal_result_status_if_enabled"],
        )
        self.assertFalse(diagnostic["active_release_performed"])
        self.assertEqual("dry_run_only_no_cas_release", diagnostic["release_guard"])

    def test_busy_precheck_adds_reconciliation_diagnostic_details(self):
        precheck = MinecraftChatClefSubmissionPrecheck()

        readiness = precheck.inspect(
            _StatusExtension(_bridge_status(_active_commands()))
        )

        self.assertFalse(readiness.ready)
        self.assertEqual("minecraft_command_busy", readiness.reason)
        diagnostic = readiness.details["active_command_reconciliation"]
        self.assertEqual("active-command-request", diagnostic["active_request_id"])
        self.assertEqual("deposit diamond 2", diagnostic["active_command"])
        self.assertFalse(diagnostic["active_release_performed"])

    def test_router_logs_busy_reconciliation_diagnostic_without_submission(self):
        logs: list[str] = []
        extension = _StatusExtension(_bridge_status(_active_commands()))
        router = MinecraftChatClefInputRouter(
            extension=extension,
            intent_gate=_AlwaysIntentGate(),
            log_callback=logs.append,
        )

        decision = router.route("put diamond in chest")

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_busy", decision.reason)
        self.assertEqual(0, len(extension.submitted))
        diagnostic = decision.result["details"]["active_command_reconciliation"]
        self.assertEqual("BLOCKED", diagnostic["reconciliation_decision"])
        self.assertFalse(diagnostic["active_release_performed"])
        self.assertTrue(
            any(
                "active command reconciliation diagnostic" in message
                and '"active_release_performed":false' in message
                for message in logs
            )
        )


class _AlwaysIntentGate:
    def should_consider(self, _text: object) -> bool:
        return True


class _StatusExtension:
    def __init__(self, status: dict[str, object]):
        self.status = status
        self.submitted: list[tuple[dict[str, object], dict[str, object]]] = []

    def get_status(self) -> dict[str, object]:
        return self.status

    def translate_natural_language_command(self, _text: object) -> dict[str, object]:
        return {
            "status": "validated",
            "executable": True,
            "command": "deposit diamond 2",
            "intent": {
                "intent_type": "deposit_item",
                "quantity": 2,
                "item_phrase": "diamond",
                "original_text": "put diamond in chest",
            },
            "resolved_target": "diamond",
            "reason_code": "validated",
            "message": "translated",
            "data": {},
        }

    def submit_translated_command(
        self,
        request: dict[str, object],
        translation: dict[str, object],
    ) -> dict[str, object]:
        self.submitted.append((dict(request), dict(translation)))
        return {"ok": True, "status": {"status": "accepted"}}


def _bridge_status(commands: dict[str, object]) -> dict[str, object]:
    return {
        "backend_id": "fabric_chatclef",
        "enabled": True,
        "connected": True,
        "lifecycle_state": "connected",
        "details": {"commands": commands},
    }


def _active_commands(
    *,
    finish_callback_received: bool = False,
    stable_quiescence: bool = False,
) -> dict[str, object]:
    data: dict[str, object] = {
        "result_reason": "dispatch_started",
        "dispatch_returned": True,
            "ownership": {
                "request_id": "active-command-request",
                "session_id": "fabric-session-1",
                "connection_generation": 1,
                "correlation_id": "command-message-1",
        },
        "finish_callback_received": finish_callback_received,
        "task_finished_event_received": False,
        "waiting_reason": "waiting_for_task_finished_event",
        "bound_root_task": {
            "class_name": "adris.altoclef.tasks.container.StoreInAnyContainerTask",
            "identity": "store-root-1",
        },
    }
    if stable_quiescence:
        data["result_reason"] = "stable_request_quiescence_observed"
        data["classification"] = "nonterminal_diagnostic"
        data["terminal_status"] = "NONE"
        data["gameplay_effect"] = "UNVERIFIED"
        data["evidence_sequence"] = 2
        data["lifecycle_evidence"] = {
            "version": "2026-08-20.java-nonterminal-evidence.v1",
            "stage": "stable_request_quiescence_observed",
            "evidence_sequence": 2,
            "status": "running",
            "gameplay_effect": "UNVERIFIED",
        }
        data["current_root_task"] = {
            "class_name": "adris.altoclef.tasks.movement.IdleTask",
            "identity": "idle-root-1",
        }
        data["stable_request_quiescence"] = {
            "qualified": True,
            "satisfied": True,
            "elapsed_since_finish_callback_ms": 1250,
            "observation_count": 3,
            "consecutive_neutral_snapshots": 3,
            "stable_duration_ms": 1000,
            "neutral_duration_ms": 1000,
            "snapshot_age_ms": 10,
            "same_session_generation": True,
            "request_root_reappeared": False,
            "request_root_observation_state": "OBSERVED_AND_GONE",
        }
    return {
        "active_session_id": "fabric-session-1",
        "active_generation": 1,
        "active_request_id": "active-command-request",
        "active_command_message_id": "command-message-1",
        "active_command": "deposit diamond 2",
        "active_command_source": "lavi_chat_mic_router",
        "active_started_at_ms": 1000,
        "active_age_ms": 9000,
        "last_result": {
            "request_id": "active-command-request",
            "ok": True,
            "status": "running",
            "error_code": None,
            "message": "dispatch started",
            "data": data,
        },
    }


if __name__ == "__main__":
    unittest.main()
