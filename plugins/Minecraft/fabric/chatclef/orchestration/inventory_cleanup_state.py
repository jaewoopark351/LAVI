#20260819_kpopmodder: Model Python-only inventory cleanup evidence without touching ChatClef Java behavior.
from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum


class InventoryEvidenceState(str, Enum):
    AVAILABLE = "available"
    FULL = "full"
    UNKNOWN = "unknown"


@dataclass(frozen=True)
class InventoryStack:
    item: str
    count: int
    slot: str = ""
    protected: bool = False
    tags: frozenset[str] = field(default_factory=frozenset)

    def __post_init__(self) -> None:
        item = str(self.item or "").strip()
        slot = str(self.slot or "").strip()
        count = self.count
        if type(count) is not int:
            raise TypeError("stack count must be an exact int")
        object.__setattr__(self, "item", item)
        object.__setattr__(self, "slot", slot)
        object.__setattr__(self, "tags", frozenset(str(tag) for tag in self.tags))


@dataclass(frozen=True)
class InventorySnapshot:
    state: InventoryEvidenceState
    free_slots: int | None = None
    stacks: tuple[InventoryStack, ...] = ()
    snapshot_id: str = ""

    def __post_init__(self) -> None:
        state = InventoryEvidenceState(self.state)
        free_slots = self.free_slots
        if free_slots is not None and type(free_slots) is not int:
            raise TypeError("free_slots must be an exact int or None")
        object.__setattr__(self, "state", state)
        object.__setattr__(self, "stacks", tuple(self.stacks))
        object.__setattr__(self, "snapshot_id", str(self.snapshot_id or ""))

    @classmethod
    def available(
        cls,
        *,
        free_slots: int,
        stacks: tuple[InventoryStack, ...] = (),
        snapshot_id: str = "",
    ) -> "InventorySnapshot":
        return cls(
            state=InventoryEvidenceState.AVAILABLE,
            free_slots=free_slots,
            stacks=stacks,
            snapshot_id=snapshot_id,
        )

    @classmethod
    def full(
        cls,
        *,
        stacks: tuple[InventoryStack, ...],
        snapshot_id: str = "",
    ) -> "InventorySnapshot":
        return cls(
            state=InventoryEvidenceState.FULL,
            free_slots=0,
            stacks=stacks,
            snapshot_id=snapshot_id,
        )

    @classmethod
    def unknown(cls) -> "InventorySnapshot":
        return cls(state=InventoryEvidenceState.UNKNOWN)


@dataclass(frozen=True)
class InventoryCleanupPostcondition:
    cleanup_request_accepted: bool
    cleanup_terminal_completed: bool
    active_command_cleared: bool
    operation_id_matches: bool
    cleanup_attempt_count: int
    primary_was_accepted: bool
    post_cleanup_snapshot: InventorySnapshot
    required_free_slots: int
    protected_item_preservation_verified: bool | None
    request_context_matches: bool = True

    def __post_init__(self) -> None:
        attempt_count = self.cleanup_attempt_count
        required_free_slots = self.required_free_slots
        if type(attempt_count) is not int:
            raise TypeError("cleanup_attempt_count must be an exact int")
        if type(required_free_slots) is not int:
            raise TypeError("required_free_slots must be an exact int")
