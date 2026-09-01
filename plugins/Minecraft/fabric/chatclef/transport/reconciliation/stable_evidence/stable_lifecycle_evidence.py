#20260901_kpopmodder: Isolate the accepted stable lifecycle evidence contract.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class StableLifecycleEvidence:
    sequence: int
    envelope_message_id: str
    fingerprint: tuple[tuple[str, object], ...]
    version: str
    stage: str
    data: dict[str, Any]

    def to_dict(self) -> dict[str, object]:
        return {
            "evidence_sequence": self.sequence,
            "envelope_message_id": self.envelope_message_id,
            "lifecycle_evidence_version": self.version,
            "lifecycle_evidence_stage": self.stage,
            "fingerprint": dict(self.fingerprint),
        }
