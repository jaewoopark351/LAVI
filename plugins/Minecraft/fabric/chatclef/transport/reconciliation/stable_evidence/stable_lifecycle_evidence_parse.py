#20260901_kpopmodder: Isolate the stable lifecycle evidence parse result contract.
from __future__ import annotations

from dataclasses import dataclass

from .stable_lifecycle_evidence import StableLifecycleEvidence


@dataclass(frozen=True)
class StableLifecycleEvidenceParse:
    accepted: bool
    reason: str
    evidence: StableLifecycleEvidence | None = None
