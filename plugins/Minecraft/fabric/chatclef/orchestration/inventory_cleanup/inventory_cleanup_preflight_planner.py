#20260901_kpopmodder: Isolate safe targeted-cleanup planning from post-cleanup admission.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.cleanup_admission_decision import (
    CleanupAdmissionDecision,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.cleanup_target_plan import (
    CleanupTargetPlan,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_evidence_state import (
    InventoryEvidenceState,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_snapshot import (
    InventorySnapshot,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_stack import (
    InventoryStack,
)


class InventoryCleanupPreflightPlanner:
    _TARGET_RE = re.compile(r"^[a-z0-9_]+$")
    _PROTECTED_TAGS = frozenset(
        {
            "tool",
            "armor",
            "food",
            "torch",
            "fuel",
            "movement",
            "building_essential",
            "rare",
            "protected",
        }
    )

    def plan_before_primary(
        self,
        *,
        snapshot: InventorySnapshot,
        primary_target: str,
        required_free_slots: int = 1,
        protected_targets: set[str] | frozenset[str] | None = None,
        required_materials: set[str] | frozenset[str] | None = None,
    ) -> CleanupAdmissionDecision:
        if type(required_free_slots) is not int or required_free_slots < 1:
            return CleanupAdmissionDecision(False, False, "invalid_required_free_slots")
        if snapshot.state is InventoryEvidenceState.UNKNOWN:
            return CleanupAdmissionDecision(False, True, "inventory_unknown_no_cleanup")
        if snapshot.state is InventoryEvidenceState.AVAILABLE:
            free_slots = snapshot.free_slots
            if type(free_slots) is int and free_slots >= required_free_slots:
                return CleanupAdmissionDecision(False, True, "inventory_available")
            return CleanupAdmissionDecision(False, False, "available_snapshot_insufficient")

        protected = self._protected_targets(
            primary_target=primary_target,
            protected_targets=protected_targets,
            required_materials=required_materials,
        )
        for stack in snapshot.stacks:
            if self._is_safe_cleanup_stack(stack, protected):
                plan = CleanupTargetPlan(
                    command=f"deposit {stack.item} {stack.count}",
                    target=stack.item,
                    count=stack.count,
                    source_slot=stack.slot,
                )
                return CleanupAdmissionDecision(
                    True,
                    False,
                    "targeted_cleanup_required",
                    plan,
                )
        return CleanupAdmissionDecision(False, False, "no_safe_cleanup_target")

    def _protected_targets(
        self,
        *,
        primary_target: str,
        protected_targets: set[str] | frozenset[str] | None,
        required_materials: set[str] | frozenset[str] | None,
    ) -> frozenset[str]:
        values = {str(primary_target or "").strip()}
        values.update(str(target or "").strip() for target in protected_targets or set())
        values.update(str(target or "").strip() for target in required_materials or set())
        return frozenset(target for target in values if target)

    def _is_safe_cleanup_stack(
        self,
        stack: InventoryStack,
        protected_targets: frozenset[str],
    ) -> bool:
        if stack.protected:
            return False
        if stack.item in protected_targets:
            return False
        if stack.count < 1:
            return False
        if not self._TARGET_RE.fullmatch(stack.item):
            return False
        return self._PROTECTED_TAGS.isdisjoint(stack.tags)
