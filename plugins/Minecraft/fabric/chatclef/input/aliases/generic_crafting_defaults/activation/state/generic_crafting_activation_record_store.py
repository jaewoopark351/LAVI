#20260905_kpopmodder: Own activation capacity, locking, and record CRUD only.
from __future__ import annotations

import threading


class GenericCraftingActivationRecordStore:
    def __init__(self, *, capacity: int):
        if type(capacity) is not int or capacity < 0:
            raise ValueError("capacity must be a non-negative exact int")
        self.capacity = capacity
        self.lock = threading.RLock()
        self.registry_token = object()
        self.records: dict[str, dict[str, object]] = {}

    def contains(self, event_id: str) -> bool:
        return event_id in self.records

    def is_at_capacity(self) -> bool:
        return len(self.records) >= self.capacity

    def get(self, event_id: str) -> dict[str, object] | None:
        return self.records.get(event_id)

    def put(self, event_id: str, record: dict[str, object]) -> None:
        self.records[event_id] = record

    def remove(self, event_id: str) -> None:
        self.records.pop(event_id, None)

    def event_ids_for_proof(self, eligibility_proof: object) -> tuple[str, ...]:
        return tuple(
            event_id
            for event_id, record in self.records.items()
            if record.get("proof") is eligibility_proof
        )

    @property
    def record_count(self) -> int:
        with self.lock:
            return len(self.records)


__all__ = ("GenericCraftingActivationRecordStore",)
