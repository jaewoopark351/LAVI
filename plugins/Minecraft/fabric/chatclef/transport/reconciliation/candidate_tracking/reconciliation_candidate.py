#20260901_kpopmodder: Isolate the stable-evidence reconciliation candidate contract.
from __future__ import annotations

from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)

from ..stable_evidence import StableLifecycleEvidence


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
