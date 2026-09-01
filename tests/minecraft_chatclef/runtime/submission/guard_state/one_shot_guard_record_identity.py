#20260901_kpopmodder: Carry only validated persisted guard record identity.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class OneShotGuardRecordIdentity:
    invocation_fingerprint: str
    command_fingerprint: str
    reconciliation_reason: str = ""
