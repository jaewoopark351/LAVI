package lavi.minecraft.diagnostics.inventory.scan;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.inventory.InventoryActiveScanOwners;
import lavi.minecraft.diagnostics.inventory.InventoryCallerSnapshot;
import lavi.minecraft.diagnostics.inventory.InventoryDiagnosticFields;
import lavi.minecraft.diagnostics.inventory.InventoryOffThreadCallerCensus;
import lavi.minecraft.diagnostics.inventory.InventoryScanContext;
import lavi.minecraft.diagnostics.inventory.InventoryScanSnapshot;
import lavi.minecraft.diagnostics.inventory.InventoryScreenSnapshot;

import java.util.List;
import java.util.Map;

//20260805_kpopmodder: Own diagnostic scan begin/end flow apart from InventorySubTracker hook methods.
public final class InventoryScanLifecycleDiagnostics {
    private final Map<String, String> lastStableKeyByTracker = new java.util.concurrent.ConcurrentHashMap<>();
    private final InventoryOffThreadCallerCensus offThreadCallers = new InventoryOffThreadCallerCensus();

    public void beginScan(String trackerIdentity,
                          InventoryScanRegistry registry,
                          InventoryScanEventEmitter emitter) {
        int scanDepthOnCurrentThread = registry.scanDepthOnCurrentThread();
        List<InventoryScanContext> activeOwnersBefore = registry.activeScansForTracker(trackerIdentity);
        int activeScanCountBefore = activeOwnersBefore.size();
        boolean clientThread = isClientThread();
        boolean sameThreadReentrant = scanDepthOnCurrentThread > 0
                || sameThreadOwnerPresent(activeOwnersBefore, Thread.currentThread().getId());
        InventoryScanContext otherActive = InventoryActiveScanOwners.firstOther(null, activeOwnersBefore);
        InventoryScanSnapshot beginSnapshot = InventoryScanSnapshot.capture();
        String previousStableKey = lastStableKeyByTracker.put(trackerIdentity, beginSnapshot.stableKey());
        boolean firstScanForTracker = previousStableKey == null;
        boolean contextChanged = previousStableKey != null && !previousStableKey.equals(beginSnapshot.stableKey());
        boolean includeTopFrames = !clientThread || sameThreadReentrant || !activeOwnersBefore.isEmpty();
        InventoryScanContext context = new InventoryScanContext(
                ChatClefDiagnostics.nextOperationId(),
                trackerIdentity,
                ChatClefDiagnostics.currentClientTickId(),
                System.nanoTime(),
                Thread.currentThread().getName(),
                Thread.currentThread().getId(),
                clientThread,
                "Render thread",
                "unavailable",
                activeScanCountBefore,
                scanDepthOnCurrentThread,
                InventoryCallerSnapshot.capture(includeTopFrames),
                beginSnapshot,
                firstScanForTracker || contextChanged || !clientThread || sameThreadReentrant || !activeOwnersBefore.isEmpty()
        );

        registry.activate(context);
        InventoryOffThreadCallerCensus.InventoryOffThreadCallerSummary offThreadSummary =
                offThreadCallers.observe(
                        context,
                        activeScanCountBefore + 1,
                        ChatClefDiagnostics.currentClientTickId(),
                        System.nanoTime()
                );

        if (context.armed()) {
            emitter.emitBeginIfNeeded(context, firstScanForTracker ? "first_inventory_scan_observed" : "inventory_scan_context_changed");
        }
        if (offThreadSummary != null) {
            emitter.emit("INVENTORY_SUBTRACKER_OFF_THREAD_CALLER_SUMMARY",
                    "inventory_subtracker_off_thread_caller_summary",
                    context,
                    InventoryDiagnosticFields.offThreadCallerSummary(context, offThreadSummary),
                    false,
                    "OFF_THREAD_CALLER|"
                            + offThreadSummary.callerFingerprint()
                            + "|"
                            + offThreadSummary.observationCount());
        }
        if (sameThreadReentrant || !activeOwnersBefore.isEmpty()) {
            context.arm();
            emitter.emitBeginIfNeeded(context, "inventory_scan_overlap_context");
            String overlapType = InventoryActiveScanOwners.overlapType(context, activeOwnersBefore);
            emitter.emit("INVENTORY_SUBTRACKER_SCAN_OVERLAP_DETECTED",
                    sameThreadReentrant ? "same_thread_inventory_scan_reentry" : "inventory_scan_overlap",
                    context,
                    InventoryDiagnosticFields.overlap(
                            context,
                            otherActive,
                            activeOwnersBefore,
                            overlapType
                    ),
                    true,
                    overlapFingerprint(context, activeOwnersBefore, overlapType));
        }
    }

    public void endScan(String trackerIdentity,
                        InventoryScanRegistry registry,
                        InventoryScanEventEmitter emitter,
                        int uniquePlayerItems,
                        int uniqueContainerItems,
                        int playerItemCountEntries,
                        int containerItemCountEntries) {
        InventoryScanContext context = registry.currentContext(trackerIdentity);
        if (context == null) {
            return;
        }
        InventoryScreenSnapshot endScreen = InventoryScreenSnapshot.capture();
        registry.deactivate(context);
        if (context.beginEmitted() || context.armed()) {
            emitter.emit("INVENTORY_SUBTRACKER_SCAN_END",
                    "inventory_scan_end",
                    context,
                    InventoryDiagnosticFields.scanEnd(
                            context,
                            endScreen,
                            uniquePlayerItems,
                            uniqueContainerItems,
                            playerItemCountEntries,
                            containerItemCountEntries,
                            System.nanoTime() - context.startNanos()
                    ),
                    false,
                    "SCAN_END|" + context.beginSnapshot().stableKey() + "|" + endScreen.stableKey());
        }
    }

    private static boolean sameThreadOwnerPresent(List<InventoryScanContext> activeOwners,
                                                  long threadId) {
        for (InventoryScanContext context : activeOwners) {
            if (context.threadId() == threadId) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClientThread() {
        String threadName = Thread.currentThread().getName();
        return "Render thread".equals(threadName) || "Client thread".equals(threadName);
    }

    private static String overlapFingerprint(InventoryScanContext context,
                                             List<InventoryScanContext> activeOwners,
                                             String overlapType) {
        return "OVERLAP|"
                + overlapType
                + "|"
                + context.beginSnapshot().stableKey()
                + "|"
                + InventoryActiveScanOwners.fingerprint(context, activeOwners);
    }
}
