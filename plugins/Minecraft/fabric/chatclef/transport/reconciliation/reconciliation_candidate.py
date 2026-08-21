#20260821_kpopmodder: Track only the exact 1->2 stable-evidence progression for shadow reconciliation.
from __future__ import annotations

from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)

from .stable_lifecycle_evidence import StableLifecycleEvidence


@dataclass(frozen=True)
class ReconciliationCandidate:
    expected_active: FabricChatClefActiveCommand
    first_sequence: int
    first_message_id: str
    fingerprint: tuple[tuple[str, object], ...]

    def evaluate(
        self,
        *,
        expected_active: FabricChatClefActiveCommand,
        evidence: StableLifecycleEvidence,
    ) -> tuple[bool, str]:
        if expected_active is not self.expected_active:
            return False, "active_object_changed"
        if self.first_sequence != 1:
            return False, "first_sequence_not_one"
        if evidence.sequence != 2:
            return False, "second_sequence_not_two"
        if evidence.envelope_message_id == self.first_message_id:
            return False, "duplicate_envelope_message_id"
        if evidence.fingerprint != self.fingerprint:
            return False, "stable_fingerprint_changed"
        return True, "stable_sequence_1_to_2"


@dataclass(frozen=True)
class CandidateUpdate:
    candidate: ReconciliationCandidate | None
    would_reconcile: bool
    reason: str


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
