#20260905_kpopmodder: Isolate synchronized STOP claim record persistence.
from __future__ import annotations

from collections.abc import Callable

from plugins.Minecraft.fabric.chatclef.input.stop.stop_control_claim_record import (
    StopControlClaimRecord,
)

class StopControlClaimRecordStore:
    def __init__(self, *, capacity: int, lock: object):
        self._capacity = capacity
        self._lock = lock
        self._records: dict[str, StopControlClaimRecord] = {}

    def transact(
        self,
        callback: Callable[[dict[str, StopControlClaimRecord], int], object],
    ) -> object:
        with self._lock:
            return callback(self._records, self._capacity)

    def clear(self) -> None:
        with self._lock:
            self._records.clear()

    @property
    def count(self) -> int:
        with self._lock:
            return len(self._records)


__all__ = ("StopControlClaimRecordStore",)
