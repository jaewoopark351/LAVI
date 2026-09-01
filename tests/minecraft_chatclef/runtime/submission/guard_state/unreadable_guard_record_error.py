#20260901_kpopmodder: Represent one fail-closed persisted guard read failure.
from __future__ import annotations


class UnreadableGuardRecordError(Exception):
    def __init__(self, reason: str):
        super().__init__(reason)
        self.reason = reason
