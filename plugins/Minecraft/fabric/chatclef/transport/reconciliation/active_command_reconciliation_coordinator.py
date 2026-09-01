#20260821_kpopmodder: Coordinate shadow and test-injected stale-active reconciliation without Java mutation.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)

from .active_command_admission_quarantine import ActiveCommandAdmissionQuarantine
from .active_command_reconciliation_outcome import (
    ActiveCommandReconciliationOutcome,
)
from .command_reconciliation_state import CommandReconciliationState
from .deposit_profile import DepositReconciliationProfileResolver
from .reconciled_active_command_tombstone import ReconciledActiveCommandTombstone
from .candidate_tracking import ReconciliationCandidateTracker
from .reconciliation_feature_gate import ReconciliationFeatureGate
from .reconciliation_identity import ReconciliationIdentity
from .stable_evidence import StableLifecycleEvidenceParser


class ActiveCommandReconciliationCoordinator:
    def __init__(
        self,
        *,
        feature_gate: ReconciliationFeatureGate | None = None,
        profile_resolver: DepositReconciliationProfileResolver | None = None,
        evidence_parser: StableLifecycleEvidenceParser | None = None,
        candidate_tracker: ReconciliationCandidateTracker | None = None,
    ):
        self._feature_gate = feature_gate or ReconciliationFeatureGate()
        self._profile_resolver = profile_resolver or DepositReconciliationProfileResolver()
        self._evidence_parser = evidence_parser or StableLifecycleEvidenceParser()
        self._candidate_tracker = candidate_tracker or ReconciliationCandidateTracker()

    @property
    def feature_gate(self) -> ReconciliationFeatureGate:
        return self._feature_gate

    def accept_and_apply(
        self,
        *,
        state: CommandReconciliationState,
        expected_active: FabricChatClefActiveCommand,
        envelope: BridgeEnvelopeDTO,
        result: CommandResultDTO,
        raw_payload: Mapping[str, Any],
        before_snapshot: dict[str, Any],
        monotonic_ms: int,
    ) -> tuple[CommandReconciliationState, ActiveCommandReconciliationOutcome]:
        if result.status in _TERMINAL_STATUSES:
            next_state = state.with_java_result(result, local_effective=True)
            next_state = next_state.without_active()
            return next_state, self._outcome(
                accepted=True,
                reason="accepted",
                before_snapshot=before_snapshot,
                after_snapshot={},
                audit={
                    "event": "command_result_accepted",
                    "path": "normal_terminal",
                    "status": result.status.value,
                    "active_release_performed": True,
                },
            )

        if result.status is not CommandResultStatus.RUNNING:
            next_state = state.with_java_result(
                result,
                local_effective=True,
                candidate=None,
            )
            return next_state, self._outcome(
                accepted=True,
                reason="accepted",
                before_snapshot=before_snapshot,
                after_snapshot={},
                audit={
                    "event": "command_result_accepted",
                    "path": "nonterminal_non_running",
                    "status": result.status.value,
                    "active_release_performed": False,
                },
            )

        return self._accept_running(
            state=state,
            expected_active=expected_active,
            envelope=envelope,
            result=result,
            raw_payload=raw_payload,
            before_snapshot=before_snapshot,
            monotonic_ms=monotonic_ms,
        )

    def audit_late_result(
        self,
        *,
        state: CommandReconciliationState,
        envelope: BridgeEnvelopeDTO,
        result: CommandResultDTO,
        before_snapshot: dict[str, Any],
        monotonic_ms: int,
    ) -> tuple[CommandReconciliationState, ActiveCommandReconciliationOutcome | None]:
        data = result.data if isinstance(result.data, Mapping) else {}
        event_kind = _late_event_kind(result)
        next_tombstones, audit = state.tombstones.audit_late_result(
            session_id=envelope.session_id,
            correlation_id=envelope.correlation_id,
            result=result,
            data=data,
            event_kind=event_kind,
            now_ms=monotonic_ms,
        )
        if audit is None:
            return state, None
        next_state = state.with_tombstones(next_tombstones)
        return next_state, self._outcome(
            accepted=False,
            reason=event_kind,
            before_snapshot=before_snapshot,
            after_snapshot={},
            audit=audit,
        )

    def _accept_running(
        self,
        *,
        state: CommandReconciliationState,
        expected_active: FabricChatClefActiveCommand,
        envelope: BridgeEnvelopeDTO,
        result: CommandResultDTO,
        raw_payload: Mapping[str, Any],
        before_snapshot: dict[str, Any],
        monotonic_ms: int,
    ) -> tuple[CommandReconciliationState, ActiveCommandReconciliationOutcome]:
        profile = self._profile_resolver.resolve(expected_active.command)
        parsed = self._evidence_parser.parse(
            envelope=envelope,
            raw_payload=raw_payload,
        )
        blocked_reasons: list[str] = []
        if not profile.allowed:
            blocked_reasons.append(profile.reason)
        if not parsed.accepted or parsed.evidence is None:
            blocked_reasons.append(parsed.reason)
        if blocked_reasons:
            next_state = state.with_java_result(
                result,
                local_effective=True,
                candidate=None,
            )
            return next_state, self._shadow_outcome(
                before_snapshot=before_snapshot,
                result=result,
                profile_name=profile.normalized_command or "unconfigured",
                would_reconcile=False,
                decision_reason=blocked_reasons[0],
                blocked_reasons=blocked_reasons,
                active_release_performed=False,
            )

        evidence = parsed.evidence
        candidate_update = self._candidate_tracker.update(
            current=state.candidate,
            expected_active=expected_active,
            evidence=evidence,
        )
        next_state = state.with_java_result(
            result,
            local_effective=True,
            candidate=candidate_update.candidate,
        )
        if not candidate_update.would_reconcile:
            return next_state, self._shadow_outcome(
                before_snapshot=before_snapshot,
                result=result,
                profile_name="deposit",
                would_reconcile=False,
                decision_reason=candidate_update.reason,
                blocked_reasons=[candidate_update.reason],
                active_release_performed=False,
                evidence=evidence.to_dict(),
            )

        gate = self._feature_gate
        if not gate.effective_enabled:
            return next_state, self._shadow_outcome(
                before_snapshot=before_snapshot,
                result=result,
                profile_name="deposit",
                would_reconcile=True,
                decision_reason=gate.blocked_reason,
                blocked_reasons=[gate.blocked_reason],
                active_release_performed=False,
                evidence=evidence.to_dict(),
            )

        identity = ReconciliationIdentity.from_active(
            expected_active,
            canonical_command=profile.normalized_command,
        )
        synthetic = self._synthetic_unknown(result.request_id)
        tombstone = ReconciledActiveCommandTombstone(
            identity=identity,
            reconciled_at_ms=monotonic_ms,
            expires_at_ms=monotonic_ms + next_state.tombstones.retention_ms,
            evidence_sequence=evidence.sequence,
            synthetic_unknown_reason="terminal_reconciliation_gap",
            profile="deposit",
        )
        tombstones = next_state.tombstones.record(tombstone, now_ms=monotonic_ms)
        quarantine = ActiveCommandAdmissionQuarantine.for_reconciled_unknown(
            identity=identity,
            activated_at_ms=monotonic_ms,
        )
        committed_state = next_state.with_reconciled_unknown(
            synthetic_result=synthetic,
            tombstones=tombstones,
            quarantine=quarantine,
        )
        return committed_state, self._outcome(
            accepted=True,
            reason="accepted",
            before_snapshot=before_snapshot,
            after_snapshot={},
            audit={
                "event": "stale_active_reconciliation",
                "command_profile": "deposit",
                "feature_gate": gate.to_dict(),
                "would_reconcile_to_unknown": True,
                "reconciliation_action": "RECONCILED_UNKNOWN",
                "active_release_performed": True,
                "tombstone_recorded": True,
                "admission_quarantine_active": True,
                "decision_reason": "reconciled_unknown_committed",
                "evidence": evidence.to_dict(),
                "identity": identity.to_dict(),
            },
        )

    def _synthetic_unknown(self, request_id: str) -> CommandResultDTO:
        return CommandResultDTO(
            request_id=request_id,
            ok=False,
            status=CommandResultStatus.UNKNOWN,
            error_code=None,
            message="Command terminal state could not be reconciled.",
            data={
                "result_reason": "terminal_reconciliation_gap",
                "detail_reason": (
                    "finish_callback_without_task_finished_event_after_stable_idle"
                ),
                "reconciliation_action": "RECONCILED_UNKNOWN",
                "result_origin": "python_stale_active_reconciliation",
                "source_status": "running",
                "java_terminal_observed": False,
                "gameplay_effect": "UNVERIFIED",
            },
        )

    def _shadow_outcome(
        self,
        *,
        before_snapshot: dict[str, Any],
        result: CommandResultDTO,
        profile_name: str,
        would_reconcile: bool,
        decision_reason: str,
        blocked_reasons: list[str],
        active_release_performed: bool,
        evidence: dict[str, object] | None = None,
    ) -> ActiveCommandReconciliationOutcome:
        return self._outcome(
            accepted=True,
            reason="accepted",
            before_snapshot=before_snapshot,
            after_snapshot={},
            audit={
                "event": "stale_active_reconciliation",
                "command_profile": profile_name,
                "feature_gate": self._feature_gate.to_dict(),
                "source_status": result.status.value,
                "would_reconcile_to_unknown": would_reconcile,
                "reconciliation_action": (
                    "RECONCILED_UNKNOWN" if would_reconcile else None
                ),
                "active_release_performed": active_release_performed,
                "tombstone_recorded": False,
                "admission_quarantine_active": False,
                "decision_reason": decision_reason,
                "blocked_reasons": blocked_reasons,
                "evidence": evidence or {},
            },
        )

    def _outcome(
        self,
        *,
        accepted: bool,
        reason: str,
        before_snapshot: dict[str, Any],
        after_snapshot: dict[str, Any],
        audit: dict[str, Any],
    ) -> ActiveCommandReconciliationOutcome:
        return ActiveCommandReconciliationOutcome(
            accepted=accepted,
            reason=reason,
            before_snapshot=before_snapshot,
            after_snapshot=after_snapshot,
            audit=audit,
        )


_TERMINAL_STATUSES = {
    CommandResultStatus.COMPLETED,
    CommandResultStatus.REJECTED,
    CommandResultStatus.FAILED,
    CommandResultStatus.CANCELLED,
    CommandResultStatus.DEADLINE_EXCEEDED,
    CommandResultStatus.UNKNOWN,
}


def _late_event_kind(result: CommandResultDTO) -> str:
    if result.status in _TERMINAL_STATUSES:
        return "late_terminal_after_reconciliation"
    return "late_nonterminal_after_reconciliation"
