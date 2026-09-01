#20260901_kpopmodder: Isolate reconciliation candidate sequence transitions.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)

from ..stable_evidence import StableLifecycleEvidence
from .candidate_update import CandidateUpdate
from .reconciliation_candidate import ReconciliationCandidate


class ReconciliationCandidateTracker:
    def update(
        self,
        *,
        current: ReconciliationCandidate | None,
        expected_active: FabricChatClefActiveCommand,
        evidence: StableLifecycleEvidence,
    ) -> CandidateUpdate:
        if evidence.sequence == 1:
            return CandidateUpdate(
                candidate=ReconciliationCandidate(
                    expected_active=expected_active,
                    first_sequence=evidence.sequence,
                    first_message_id=evidence.envelope_message_id,
                    fingerprint=evidence.fingerprint,
                ),
                would_reconcile=False,
                reason="sequence_one_recorded",
            )
        if evidence.sequence != 2:
            return CandidateUpdate(
                candidate=None,
                would_reconcile=False,
                reason="unexpected_evidence_sequence",
            )
        if current is None:
            return CandidateUpdate(
                candidate=None,
                would_reconcile=False,
                reason="sequence_two_without_sequence_one",
            )
        would_reconcile, reason = current.evaluate(
            expected_active=expected_active,
            evidence=evidence,
        )
        return CandidateUpdate(
            candidate=None,
            would_reconcile=would_reconcile,
            reason=reason,
        )
