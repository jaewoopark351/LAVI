#20260901_kpopmodder: Seal one exact saved trusted-container inventory snapshot.
from __future__ import annotations

from dataclasses import dataclass, field


_CONTAINER_INVENTORY_SNAPSHOT_SEAL = object()


@dataclass(frozen=True, slots=True)
class TrustedContainerInventorySnapshot:
    block_entity_id: str
    position: tuple[int, int, int]
    capacity_slots: int
    occupied_slots: int
    empty_slots: int
    has_capacity: bool
    item_counts: tuple[tuple[str, int], ...]
    inventory_fingerprint: str
    capacity_scope: str
    _seal: object = field(default=None, repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _CONTAINER_INVENTORY_SNAPSHOT_SEAL:
            raise ValueError("container inventory snapshot must be collector-created")


def _create_container_inventory_snapshot(
    *,
    block_entity_id: str,
    position: tuple[int, int, int],
    capacity_slots: int,
    occupied_slots: int,
    empty_slots: int,
    has_capacity: bool,
    item_counts: tuple[tuple[str, int], ...],
    inventory_fingerprint: str,
) -> TrustedContainerInventorySnapshot:
    return TrustedContainerInventorySnapshot(
        block_entity_id=block_entity_id,
        position=position,
        capacity_slots=capacity_slots,
        occupied_slots=occupied_slots,
        empty_slots=empty_slots,
        has_capacity=has_capacity,
        item_counts=item_counts,
        inventory_fingerprint=inventory_fingerprint,
        capacity_scope="SINGLE_BLOCK_ENTITY_SAVED_SLOTS",
        _seal=_CONTAINER_INVENTORY_SNAPSHOT_SEAL,
    )
