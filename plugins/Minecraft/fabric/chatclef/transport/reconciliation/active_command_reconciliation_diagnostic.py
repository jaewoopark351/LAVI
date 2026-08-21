#20260820_kpopmodder: Build dry-run evidence for stale active-command reconciliation without releasing ownership.
from __future__ import annotations

from typing import Any, Mapping

from .active_command_identity_diagnostic import ActiveCommandIdentityDiagnosticBuilder
from .active_command_lifecycle_evidence import ActiveCommandLifecycleEvidenceBuilder
from .active_command_reconciliation_profile import (
    ActiveCommandReconciliationProfileResolver,
)
from .reconciliation_mapping_utils import mapping, short_text, text


class ActiveCommandReconciliationDiagnosticBuilder:
    POLICY_VERSION = "2026-08-20.diagnostics-only.v1"

    def __init__(
        self,
        profile_resolver: ActiveCommandReconciliationProfileResolver | None = None,
        identity_builder: ActiveCommandIdentityDiagnosticBuilder | None = None,
        lifecycle_builder: ActiveCommandLifecycleEvidenceBuilder | None = None,
    ):
        self._profile_resolver = (
            profile_resolver or ActiveCommandReconciliationProfileResolver()
        )
        self._identity_builder = (
            identity_builder or ActiveCommandIdentityDiagnosticBuilder()
        )
        self._lifecycle_builder = (
            lifecycle_builder
            or ActiveCommandLifecycleEvidenceBuilder(
                self._profile_resolver.NEUTRAL_ROOT_CLASSES
            )
        )

    def build(self, commands: Mapping[str, Any] | None) -> dict[str, Any]:
        command_status = mapping(commands)
        last_result = mapping(command_status.get("last_result"))
        last_data = mapping(last_result.get("data"))
        profile = self._profile_resolver.profile_for(
            command_status.get("active_command")
        )
        identity = self._identity_builder.build(
            command_status,
            last_result,
            last_data,
        )
        lifecycle = self._lifecycle_builder.build(
            command_status,
            last_result,
            last_data,
        )
        blocked_reasons = self._blocked_reasons(
            command_status=command_status,
            profile=profile,
            identity=identity,
            lifecycle=lifecycle,
        )
        would_reconcile = not blocked_reasons

        return {
            "diagnostic_event": "active_command_reconciliation_diagnostic",
            "policy_version": self.POLICY_VERSION,
            "mode": "diagnostics_only",
            "reconciliation_mode": "dry_run",
            "gameplay_effect": "UNVERIFIED",
            "terminal_status": "NONE",
            "active_release_performed": False,
            "would_reconcile_to_unknown": would_reconcile,
            "reconciled_action_if_enabled": (
                "RECONCILED_UNKNOWN" if would_reconcile else None
            ),
            "synthetic_result_status_if_enabled": (
                "unknown" if would_reconcile else None
            ),
            "terminal_result_status_if_enabled": (
                "RECONCILED_UNKNOWN" if would_reconcile else None
            ),
            "reconciliation_decision": (
                "ELIGIBLE_DRY_RUN" if would_reconcile else "BLOCKED"
            ),
            "blocked_reason": (
                "none" if would_reconcile else blocked_reasons[0]
            ),
            "blocked_reasons": blocked_reasons,
            "retry_count": 0,
            "replay_count": 0,
            "auto_stop_requested": False,
            "follow_up_command_submitted": False,
            "active_command_still_owned_by_python": (
                text(command_status.get("active_request_id")) is not None
            ),
            "active_request_id": text(command_status.get("active_request_id")),
            "active_session_id": text(command_status.get("active_session_id")),
            "active_generation": command_status.get("active_generation"),
            "active_command_message_id": text(
                command_status.get("active_command_message_id")
            ),
            "active_command": short_text(command_status.get("active_command")),
            "active_command_source": short_text(
                command_status.get("active_command_source")
            ),
            "active_age_ms": command_status.get("active_age_ms"),
            "active_started_at_ms": command_status.get("active_started_at_ms"),
            "command_profile": profile,
            "identity_quality": identity["quality"],
            "identity_checks": identity["checks"],
            "identity_missing_fields": identity["missing_fields"],
            "identity_mismatch_fields": identity["mismatch_fields"],
            "accepted_evidence_provenance": identity[
                "accepted_evidence_provenance"
            ],
            "lifecycle_evidence": lifecycle,
            "release_guard": "dry_run_only_no_cas_release",
        }

    def _blocked_reasons(
        self,
        *,
        command_status: Mapping[str, Any],
        profile: Mapping[str, Any],
        identity: Mapping[str, Any],
        lifecycle: Mapping[str, Any],
    ) -> list[str]:
        reasons: list[str] = []
        if text(command_status.get("active_request_id")) is None:
            reasons.append("no_active_command")
        if profile.get("auto_reconcile_to_unknown_allowed") is not True:
            reasons.append("reconciliation_profile_not_allowed")
        if identity.get("quality") != "EXACT":
            reasons.append("identity_not_exact")
        if lifecycle.get("last_result_matches_active") is not True:
            reasons.append("last_result_not_matching_active")
        if lifecycle.get("stronger_terminal_result_present") is True:
            reasons.append("stronger_terminal_result_present")
        if lifecycle.get("last_result_status") != "running":
            reasons.append("latest_accepted_status_not_running")
        if lifecycle.get("dispatch_returned") is not True:
            reasons.append("missing_dispatch_returned_evidence")
        if lifecycle.get("finish_callback_received") is not True:
            reasons.append("missing_finish_callback_evidence")
        if lifecycle.get("task_finished_event_received") is not False:
            reasons.append("missing_task_finished_event_absence_evidence")
        stable_quiescence = mapping(lifecycle.get("stable_request_quiescence"))
        if stable_quiescence.get("qualified") is not True:
            reasons.append("missing_stable_request_quiescence_evidence")
        if lifecycle.get("current_root_is_neutral") is not True:
            reasons.append("current_root_not_neutral_or_unknown")
        return reasons
