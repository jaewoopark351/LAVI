#20260901_kpopmodder: Isolate fail-closed post-cleanup primary admission checks.
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.cleanup_admission_decision import (
    CleanupAdmissionDecision,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_cleanup_postcondition import (
    InventoryCleanupPostcondition,
)
from plugins.Minecraft.fabric.chatclef.orchestration.inventory_cleanup.inventory_evidence_state import (
    InventoryEvidenceState,
)


class PostCleanupPrimaryAdmission:
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
            return CleanupAdmissionDecision(
                False,
                False,
                "cleanup_attempt_count_not_one",
            )
        if postcondition.protected_item_preservation_verified is not True:
            return CleanupAdmissionDecision(
                False,
                False,
                "protected_preservation_unverified",
            )
        snapshot = postcondition.post_cleanup_snapshot
        if snapshot.state is not InventoryEvidenceState.AVAILABLE:
            return CleanupAdmissionDecision(
                False,
                False,
                "post_cleanup_inventory_not_available",
            )
        free_slots = snapshot.free_slots
        if type(free_slots) is not int or free_slots < postcondition.required_free_slots:
            return CleanupAdmissionDecision(
                False,
                False,
                "post_cleanup_free_slots_insufficient",
            )
        return CleanupAdmissionDecision(False, True, "post_cleanup_verified")
