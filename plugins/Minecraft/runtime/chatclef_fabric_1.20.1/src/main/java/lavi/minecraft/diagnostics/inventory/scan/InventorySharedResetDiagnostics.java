package lavi.minecraft.diagnostics.inventory.scan;

import lavi.minecraft.diagnostics.inventory.InventoryActiveScanOwners;
import lavi.minecraft.diagnostics.inventory.InventoryDiagnosticFields;
import lavi.minecraft.diagnostics.inventory.InventoryScanContext;

import java.util.List;

//20260805_kpopmodder: Observe InventorySubTracker shared map reset boundaries without changing reset behavior.
public final class InventorySharedResetDiagnostics {
    public void emit(InventoryScanContext context,
                     InventoryScanRegistry registry,
                     InventoryScanEventEmitter emitter,
                     String phase,
                     int uniquePlayerItems,
                     int uniqueContainerItems,
                     int playerItemCountEntries,
                     int containerItemCountEntries) {
        List<InventoryScanContext> activeOwners = registry.activeScansForTracker(context.trackerIdentity());
        boolean overlapping = activeOwners.size() > 1 || context.scanDepthOnCurrentThread() > 0;
        if (overlapping || !context.clientThread()) {
            context.arm();
        }
        if (!context.armed()) {
            return;
        }
        emitter.emitBeginIfNeeded(context, "inventory_subtracker_shared_reset_" + phase.toLowerCase());
        emitter.emit("INVENTORY_SUBTRACKER_SHARED_RESET_" + phase,
                "inventory_subtracker_shared_reset_" + phase.toLowerCase(),
                context,
                InventoryDiagnosticFields.sharedReset(
                        context,
                        phase,
                        context.lastSharedResetGeneration(),
                        uniquePlayerItems,
                        uniqueContainerItems,
                        playerItemCountEntries,
                        containerItemCountEntries,
                        activeOwners
                ),
                overlapping,
                "SHARED_RESET|"
                        + phase
                        + "|"
                        + context.clientThread()
                        + "|"
                        + InventoryActiveScanOwners.fingerprint(context, activeOwners)
                        + "|"
                        + context.beginSnapshot().stableKey());
    }
}
