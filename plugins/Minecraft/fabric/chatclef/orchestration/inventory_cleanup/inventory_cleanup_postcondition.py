#20260901_kpopmodder: Keep post-cleanup evidence requirements in one contract file.
from __future__ import annotations

from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_snapshot import (
    InventorySnapshot,
)


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
