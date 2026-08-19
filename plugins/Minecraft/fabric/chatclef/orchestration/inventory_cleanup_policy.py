#20260819_kpopmodder: Plan Python-owned targeted inventory cleanup without bare deposit fallback.
from __future__ import annotations

import re
from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup_state import (
    InventoryCleanupPostcondition,
    InventoryEvidenceState,
    InventorySnapshot,
    InventoryStack,
)


@dataclass(frozen=True)
class CleanupTargetPlan:
    command: str
    target: str
    count: int
    source_slot: str = ""


@dataclass(frozen=True)
class CleanupAdmissionDecision:
    cleanup_allowed: bool
    primary_allowed: bool
    reason: str
    cleanup_plan: CleanupTargetPlan | None = None


class ChatClefInventoryCleanupPolicy:
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
                return CleanupAdmissionDecision(True, False, "targeted_cleanup_required", plan)
        return CleanupAdmissionDecision(False, False, "no_safe_cleanup_target")

    def may_submit_primary_after_cleanup(
        self,
        postcondition: InventoryCleanupPostcondition,
    ) -> CleanupAdmissionDecision:
        if postcondition.primary_was_accepted:
            return CleanupAdmissionDecision(False, False, "primary_already_accepted")
        if not postcondition.cleanup_request_accepted:
            return CleanupAdmissionDecision(False, False, "cleanup_not_accepted")
        if not postcondition.cleanup_terminal_completed:
            return CleanupAdmissionDecision(False, False, "cleanup_terminal_not_completed")
        if not postcondition.active_command_cleared:
            return CleanupAdmissionDecision(False, False, "cleanup_active_not_cleared")
        if not postcondition.operation_id_matches or not postcondition.request_context_matches:
            return CleanupAdmissionDecision(False, False, "cleanup_context_mismatch")
        if postcondition.cleanup_attempt_count != 1:
            return CleanupAdmissionDecision(False, False, "cleanup_attempt_count_not_one")
        if postcondition.protected_item_preservation_verified is not True:
            return CleanupAdmissionDecision(False, False, "protected_preservation_unverified")
        snapshot = postcondition.post_cleanup_snapshot
        if snapshot.state is not InventoryEvidenceState.AVAILABLE:
            return CleanupAdmissionDecision(False, False, "post_cleanup_inventory_not_available")
        free_slots = snapshot.free_slots
        if type(free_slots) is not int or free_slots < postcondition.required_free_slots:
            return CleanupAdmissionDecision(False, False, "post_cleanup_free_slots_insufficient")
        return CleanupAdmissionDecision(False, True, "post_cleanup_verified")

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
