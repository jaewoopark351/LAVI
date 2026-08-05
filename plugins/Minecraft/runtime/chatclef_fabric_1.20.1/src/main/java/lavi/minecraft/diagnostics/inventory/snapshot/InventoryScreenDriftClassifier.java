package lavi.minecraft.diagnostics.inventory.snapshot;

import lavi.minecraft.diagnostics.inventory.InventoryScreenSnapshot;

//20260805_kpopmodder: Classify screen/handler drift independently from snapshot capture.
public final class InventoryScreenDriftClassifier {
    private InventoryScreenDriftClassifier() {
    }

    public static String driftReason(InventoryScreenSnapshot begin, InventoryScreenSnapshot current) {
        if (current == null) {
            return "CURRENT_SNAPSHOT_UNAVAILABLE";
        }
        int changed = 0;
        String reason = "";
        if (!begin.screenClass().equals(current.screenClass())) {
            changed++;
            reason = "SCREEN_CHANGED";
        }
        if (!begin.playerHandlerIdentity().equals(current.playerHandlerIdentity())) {
            changed++;
            reason = "HANDLER_IDENTITY_CHANGED";
        }
        if (!begin.playerHandlerSyncId().equals(current.playerHandlerSyncId())) {
            changed++;
            reason = "SYNC_ID_CHANGED";
        }
        if (!begin.playerHandlerSlotCount().equals(current.playerHandlerSlotCount())) {
            changed++;
            reason = "SLOT_COUNT_CHANGED";
        }
        if (!begin.screenHandlerMatchesPlayerHandler().equals(current.screenHandlerMatchesPlayerHandler())) {
            changed++;
            reason = "SCREEN_HANDLER_MISMATCH";
        }
        return changed == 0 ? "none" : changed > 1 ? "MULTIPLE_CONTEXT_FIELDS_CHANGED" : reason;
    }
}
