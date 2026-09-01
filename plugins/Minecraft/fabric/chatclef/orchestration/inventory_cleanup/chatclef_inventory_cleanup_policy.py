#20260901_kpopmodder: Preserve the cleanup policy API as a thin composition boundary.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.cleanup_admission_decision import (
    CleanupAdmissionDecision,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_cleanup_postcondition import (
    InventoryCleanupPostcondition,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_cleanup_preflight_planner import (
    InventoryCleanupPreflightPlanner,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_snapshot import (
    InventorySnapshot,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.post_cleanup_primary_admission import (
    PostCleanupPrimaryAdmission,
)


class ChatClefInventoryCleanupPolicy:
    def plan_before_primary(
        self,
        *,
        snapshot: InventorySnapshot,
        primary_target: str,
        required_free_slots: int = 1,
        protected_targets: set[str] | frozenset[str] | None = None,
        required_materials: set[str] | frozenset[str] | None = None,
    ) -> CleanupAdmissionDecision:
        return InventoryCleanupPreflightPlanner().plan_before_primary(
            snapshot=snapshot,
            primary_target=primary_target,
            required_free_slots=required_free_slots,
            protected_targets=protected_targets,
            required_materials=required_materials,
        )

    def may_submit_primary_after_cleanup(
        self,
        postcondition: InventoryCleanupPostcondition,
    ) -> CleanupAdmissionDecision:
        return PostCleanupPrimaryAdmission().may_submit_primary_after_cleanup(
            postcondition
        )
