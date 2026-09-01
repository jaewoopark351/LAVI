#20260901_kpopmodder: Keep inventory snapshot normalization and factories in one contract file.
from __future__ import annotations

from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_evidence_state import (
    InventoryEvidenceState,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_stack import (
    InventoryStack,
)


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
